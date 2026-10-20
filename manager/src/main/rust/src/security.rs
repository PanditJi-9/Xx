/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
//! رصد بيئة التشغيل **من داخل كود مُصرَّف** — ومنطق خالص يُقاس على المضيف.
//!
//! # لماذا هذه الوحدة موجودة
//!
//! كل ما في `nd.max.core.security` من Kotlin يمكن تعطيله من Java: `Debug.isDebuggerConnected`
//! تُستبدل، و`File("/proc/self/maps")` تُحوَّل إلى ملفّ نظيف. أمّا هنا فالرصد يقع داخل
//! المكتبة الأصلية ويُعاد **بتٌّ واحد** عبر JNI — فتعطيله يحتاج تعديل ذاكرة عملية مُصرَّفة
//! لا سطرًا في DEX. وهذا هو **الوحيد** الذي يبرّر نقلًا إلى native: أن يصير للحكم مساران
//! مستقلّان يجب إبطالهما **معًا**. ونقلُ منطق إلى C++ لأجل الحجب وحده لا يحمي شيئًا.
//!
//! # وحدوده المعلنة — بلا تهويل
//!
//! * `TracerPid` يُقرأ من `/proc/self/status`، وهو **قابل للتزييف** في نواة معدَّلة أو عبر
//!   hook على `open`/`read` داخل العملية. فلا يُبنى عليه حكم قاطع، بل إشارة تُصدَّق بغيرها.
//! * وقائمة أسماء الهوك (`HOOK_MARKERS`) **قائمة سوداء**، والقوائم السوداء تتقادم: سطح هوك
//!   جديد باسم جديد لا يُرصد. ولذلك يُرصد **وجوده** ولا يُدَّعى انعدامه.
//! * ومنع الرصد لا يُدَّعى أبدًا: كل ما هنا يرفع الكلفة، ولا يعد بشيء آخر.

use sha2::{Digest, Sha256};
use std::fs::File;
use std::io::Read;
use std::path::Path;

/// بتّة: متتبّع مرتبط بالعملية (`TracerPid != 0`). والقيم مطابقة حرفيًّا لـ`SecurityBridge.kt`.
pub const BIT_TRACER: i32 = 1;
/// بتّة: مكتبة هوك/ريث ظاهرة في خريطة الذاكرة.
pub const BIT_HOOK_LIBRARY: i32 = 2;
/// بتّة: منفذ تنقيح/ريث معلن في جدول مقابس النواة.
pub const BIT_DEBUG_PORT: i32 = 4;

/// أسماء مكتبات الهوك المعروفة. تُطابَق **باسمها** لا بنمط عشوائي، لأن البحث عن كلمة
/// «frida» في أي سلسلة يعطي بلاغات كاذبة (اسم حزمة، مسار مشروع، نصّ) — والمسبار الذي
/// يُبلّغ كذبًا أسوأ من غياب المسبار.
pub const HOOK_MARKERS: [&str; 11] = [
    "frida-agent",
    "frida-gadget",
    "libsubstrate",
    "libxposed",
    "liblspd",
    "libepic",
    "libsandhook",
    "libdobby",
    "libwhale",
    "libriru",
    "libpine",
];

/// منافذ الريث/التنقيح الشائعة: 27042 (frida) و23946 (IDA/gdbserver).
const DEBUG_PORTS: [&str; 2] = ["69A2", "5D8A"];

/// حجم القطعة عند حساب بصمة ملفّ — المكتبة الأصلية بضع مئات الكيلوبايت، فلا يُحمَّل ملفّ
/// كامل في الذاكرة بلا داعٍ.
const CHUNK: usize = 64 * 1024;

/// `TracerPid` من محتوى `/proc/self/status`.
///
/// و`None` تعني «السطر غائب» (نواة لا تُعلنه، أو ملفّ غير مقروء) — **ولا تعني صفرًا**.
/// والفرق جوهريّ: صفرٌ مخترع يجعل «لم أقِس» يُقرأ «سليم»، وهو صنف العطب الذي أُعلن في
/// هذا المستودع مرارًا في مسارات التشخيص.
pub fn parse_tracer_pid(status: &str) -> Option<i32> {
    status
        .lines()
        .find_map(|line| line.strip_prefix("TracerPid:"))
        .and_then(|value| value.trim().parse::<i32>().ok())
}

/// هل العملية تحت متتبّع؟ `false` تعني «أُعلن صفرًا»، وغياب السطر يعني كذلك — وهذا مقصود
/// هنا: **إشارةٌ** لا تُرفض بها نسخة، فلا حاجة إلى تمييز «لم أقرأ» عن «لا متتبّع».
pub fn tracer_attached(status: &str) -> bool {
    parse_tracer_pid(status).is_some_and(|pid| pid > 0)
}

/// أوّل اسم مكتبة هوك موجود في خريطة الذاكرة، أو `None`.
pub fn find_hook_marker(maps: &str) -> Option<&'static str> {
    HOOK_MARKERS
        .iter()
        .find(|marker| maps.contains(**marker))
        .copied()
}

/// هل يظهر منفذ تنقيح/ريث في **حالة استماع**؟ يُقرأ من `/proc/net/tcp` و`tcp6`، ورمز
/// الحالة `0A` هو `TCP_LISTEN` في النواة.
pub fn debug_port_listening(table: &str) -> bool {
    table.lines().any(|line| {
        // ترتيب الأعمدة في `/proc/net/tcp`: `sl` ثم المحلّي ثم البعيد ثم الحالة —
        // فتُتخطّى الأولى والثانية والثالثة بالترتيب، ولا يُقرأ عمود الحالة بالخطأ
        // عنوانًا بعيدًا (وهو عطب بدأ فعلًا في أول نسخة من هذه الدالّة، والتقطه الاختبار).
        let mut fields = line.split_whitespace().skip(1);
        let local = match fields.next() {
            Some(value) => value,
            None => return false,
        };
        if fields.next().is_none() {
            return false;
        }
        let state = match fields.next() {
            Some(value) => value,
            None => return false,
        };
        if state != "0A" {
            return false;
        }
        let port = local.rsplit(':').next().unwrap_or("");
        DEBUG_PORTS.iter().any(|known| known.eq_ignore_ascii_case(port))
    })
}

/// قناع البتّات من المحتوى المقروء — دالّة واحدة يجمعها النداء الأصلي، وهي **نقيّة**
/// فتقع تحت الاختبار على المضيف بلا جهاز.
pub fn probe_mask(status: &str, maps: &str, tcp: &str) -> i32 {
    let mut mask = 0;
    if tracer_attached(status) {
        mask |= BIT_TRACER;
    }
    if find_hook_marker(maps).is_some() {
        mask |= BIT_HOOK_LIBRARY;
    }
    if debug_port_listening(tcp) {
        mask |= BIT_DEBUG_PORT;
    }
    mask
}

/// بصمة SHA-256 سداسية لملفّ. و`None` عند أي فشل قراءة — ولا يُخترع بديل.
pub fn file_sha256_hex(path: &str) -> Option<String> {
    let file = File::open(Path::new(path)).ok()?;
    let mut reader = std::io::BufReader::new(file);
    let mut hasher = Sha256::new();
    let mut buffer = vec![0u8; CHUNK];
    loop {
        let read = reader.read(&mut buffer).ok()?;
        if read == 0 {
            break;
        }
        hasher.update(&buffer[..read]);
    }
    Some(hasher.finalize().iter().map(|byte| format!("{:02x}", byte)).collect())
}

/// قراءة ملفّ نصّي (يُتجاهل حجمه: ملفّات `/proc` هنا سطور فقط). و`None` إن تعذّرت.
pub fn read_text(path: &str) -> Option<String> {
    std::fs::read_to_string(path).ok()
}

/// قناع البتّات من ملفّات النظام الحقيقيّة.
pub fn runtime_probe() -> i32 {
    // وكلّ ملفّ يُقرأ مرّة واحدة: `/proc` يُنتج المحتوى عند القراءة لا عند الفتح، فالثلاثة
    // تُقرأ هنا صريحًا لتقع تحت الاختبار على المضيف من نفس الدالّة.
    let status = read_text("/proc/self/status").unwrap_or_default();
    let maps = read_text("/proc/self/maps").unwrap_or_default();
    let tcp = format!(
        "{}{}",
        read_text("/proc/net/tcp").unwrap_or_default(),
        read_text("/proc/net/tcp6").unwrap_or_default()
    );
    probe_mask(&status, &maps, &tcp)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn tracer_pid_is_read_not_invented() {
        assert_eq!(parse_tracer_pid("Name:\tmax\nTracerPid:\t0\nUid:\t10123\n"), Some(0));
        assert_eq!(parse_tracer_pid("Name:\tmax\nTracerPid:\t4242\n"), Some(4242));
        // غياب السطر = «لم أقِس» لا صفرًا.
        assert_eq!(parse_tracer_pid("Name:\tmax\nUid:\t1\n"), None);
        assert!(!tracer_attached("TracerPid:\t0\n"));
        assert!(tracer_attached("TracerPid:\t7\n"));
    }

    #[test]
    fn hook_marker_is_matched_by_name_only() {
        let maps = "/system/lib64/libc.so\n/data/local/tmp/frida-agent-64.so\n";
        assert_eq!(find_hook_marker(maps), Some("frida-agent"));
        // كلمة موجودة في مسار مشروع ليست هوكًا: الاسم لا يُطابق.
        assert_eq!(find_hook_marker("/data/app/nd.max/files/notes.txt\n"), None);
        assert_eq!(find_hook_marker(""), None);
    }

    #[test]
    fn only_listening_debug_ports_count() {
        // 0A = LISTEN على 27042 (frida).
        assert!(debug_port_listening(
            "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when\n   0: 0100007F:69A2 00000000:0000 0A 00000000:00000000\n"
        ));
        // نفس المنفذ في حالة اتصال (01) لا يُحتسب.
        assert!(!debug_port_listening(
            "   1: 0100007F:69A2 0100007F:1F90 01 00000000:00000000\n"
        ));
        // منفذ آخر في استماع لا يُحتسب.
        assert!(!debug_port_listening(
            "   2: 00000000:1F90 00000000:0000 0A 00000000:00000000\n"
        ));
    }

    #[test]
    fn probe_mask_combines_independent_signals() {
        assert_eq!(probe_mask("TracerPid:\t0\n", "libc.so\n", ""), 0);
        assert_eq!(probe_mask("TracerPid:\t9\n", "libc.so\n", ""), BIT_TRACER);
        assert_eq!(probe_mask("TracerPid:\t0\n", "libxposed.so\n", ""), BIT_HOOK_LIBRARY);
        let both = probe_mask("TracerPid:\t9\n", "libpine.so\n", "");
        assert_eq!(both, BIT_TRACER | BIT_HOOK_LIBRARY);
    }

    #[test]
    fn file_digest_matches_known_sha256() {
        // نصّ معروف بصمته: "abc" ⇒ ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad
        let dir = std::env::temp_dir().join("maxmanager-security-test");
        std::fs::create_dir_all(&dir).expect("temp dir");
        let path = dir.join("abc.bin");
        std::fs::write(&path, b"abc").expect("write");
        let hex = file_sha256_hex(path.to_str().expect("path")).expect("digest");
        assert_eq!(hex, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        // وغياب الملفّ ليس بصمةً مخترعة.
        assert!(file_sha256_hex("/definitely/missing/file.bin").is_none());
    }
}

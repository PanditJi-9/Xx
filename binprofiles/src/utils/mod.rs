/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
use crate::props::*;
use std::os::unix::fs::PermissionsExt;
use std::fs; 
use std::path::Path; 
use std::process::Command;
use std::sync::{Mutex, OnceLock};
use std::time::{SystemTime, UNIX_EPOCH};
use glob::glob;
use std::collections::{HashMap, HashSet};

pub const CONFIG_PATH: &str = "/data/adb/.config/MaxManager";
pub const MY_PATH: &str = "/system/bin:/system/xbin:/data/adb/ap/bin:/data/adb/ksu/bin:/data/adb/magisk:/debug_ramdisk:/sbin:/sbin/su:/su/bin:/su/xbin:/data/data/com.termux/files/usr/bin";

/// قراءة خصيصة — **داخل العملية أولًا**، والصدفة احتياطٌ مُعلن لا أصل.
///
/// **السبب المقيس:** كل `getprop` كان ولادة عملية كاملة (`fork`+`exec`)، وهذه الدالة
/// تُنادى في مسار ساخن (كل كتابة تنادي `log_verbose` ← `get_debugmode`، وكل نداء تركيبة
/// ينادي `get_limiter`/`per_app_governor_isolation_active`/`manual_freq_session_active`).
/// والبديل داخلي: `prop_read::read_property` على bionic — الكلفة المقيسة في نظيره
/// بالتطبيق **×١٨٧ أسرع** (٢٣٦٦ ميكرو ⇒ ١٢٫٦).
///
/// **والدلالة محفوظة حرفيًّا:** على مضيف بلا bionic يعود `None` فيُسأل `getprop` كما كان،
/// وعلى الجهاز تُقرأ الخصيصة نفسها من المصدر نفسه — فرق كلفة لا فرق معنى.
pub fn getprop(key: &str) -> String {
    if let Some(value) = crate::prop_read::read_property(key) {
        return value.trim().to_string();
    }
    if let Ok(output) = Command::new("getprop").arg(key).output() {
        String::from_utf8_lossy(&output.stdout).trim().to_string()
    } else {
        String::new()
    }
}

pub fn resetprop(key: &str, val: &str) {
    let status = if val.is_empty() {
        Command::new("resetprop").arg("--delete").arg(key).status()
    } else {
        Command::new("resetprop").arg(key).arg(val).status()
    };

    if let Err(e) = status {
        log_info(&format!("Failed to resetprop '{}': {}", key, e));
    }
}

pub fn setprop(key: &str, val: &str) {
    if let Err(e) = Command::new("setprop").arg(key).arg(val).status() {
        log_info(&format!("Failed to setprop '{}' to '{}': {}", key, val, e));
    }
}

pub fn log_verbose(message: &str) {
    if get_debugmode() {
        let _ = Command::new("sys.maxmanager-service")
            .args(["--verboselog", "AZLog", "0", message])
            .status();
    }
}

pub fn log_info(message: &str) {
    let _ = Command::new("sys.maxmanager-service")
        .args(["--log", "MaxManager_Profiler", "1", message])
        .status();
}

pub fn chmod(path: &str, mode: u32) {
    if let Ok(metadata) = fs::metadata(path) {
        let mut perms = metadata.permissions();
        perms.set_mode(mode);
        let _ = fs::set_permissions(path, perms);
    }
}

/// هل القيمة المطلوبة **هي القيمة القائمة**؟ — قرار نقيّ يُقاس وحده (`cargo test`).
///
/// المقارنة على **المضمون** لا على البايتات: عقد `sysfs` تعيد `1804800\n`، وعقد `/proc/ppm`
/// تعيد `"0 1804800"` بفراغات متفاوتة — والاختلاف في المحيط ليس اختلافًا في القيمة. ولو
/// قارنّا حرفيًّا لكان الحرس ساقطًا في كل عقدة تُزيّن مخرَجها، فتُهدر الكتابة التي وُجد لمنعها.
///
/// وغياب الملف (`None`) **ليس** تساويًا: العقدة التي لم تُقرأ تُكتب كما كانت تُكتب دائمًا —
/// لا يُستنتج «مكتوبة» من عقدة محجوبة أو غائبة.
pub fn value_already_applied(current: Option<&str>, desired: &str) -> bool {
    let Some(raw) = current else {
        return false;
    };
    let normalize = |text: &str| {
        text.split_whitespace().collect::<Vec<_>>().join(" ")
    };
    normalize(raw) == normalize(desired)
}

/// كتابة عقدة واحدة — **وبلا كتابة إن كانت القيمة هي القيمة**.
///
/// **السبب الجذري المقيس (جولة الحرارة واللاغ):** كانت هذه الدالة تُزيّن العقدة
/// (`chmod 644`) ثم تكتب ثم تُقفلها (`chmod 444`) **في كل نداء**، ولو كانت القيمة نفسها.
/// والخادم ينادي `applyfreqbalance` كل دورة (‏`System.c`)، و`dsetfreq` يكتب لكل نواة
/// `scaling_max_freq` و`scaling_min_freq` — أي **١٦ كتابة + ٤٨ نداء `chmod` + ولادة عملية**
/// في كل دورة، لنواة لا يتغيّر فيها شيء. وهذا بعينه ما يُقاس على الجهاز حرارةً ولاغًا:
/// كل كتابة على `scaling_max_freq` تُشغّل سياسة التردد في النواة من جديد.
/// فالآن: تُقرأ القيمة أولًا، وإن كانت هي المطلوبة **لا كتابة ولا `chmod`** — والقيمة
/// المخالفة (أعادها البائع أو تغيّر الطلب) تُكتب كما كانت تُكتب دائمًا.
///
/// وطبقة ثانية فوقها: عقدة رُفضت كتابتها تُتخطّى من الذاكرة ([remember_denied]) لا
/// بمعاودة المحاولة الفاشلة نفسها في كل تطبيق ملفّ — والدليل يبقى: سطر الرفض الأوّل
/// وسطر تخطٍّ واحد لكل مسار في كل عملية، لا سطر لكل نداء.
///
/// والعائد: هل وقعت كتابة فعلًا — يقرؤه المُنادي ليعرف أتثبيت العقد (`chmod 444`) مطلوب.
pub fn write_unlock_core(value: &str, path_str: &str, lock: bool) -> bool {
    let path = Path::new(path_str);

    // عُرفت غير قابلة للكتابة وما زال تسجيلها ساريًا: لا `stat` ولا قراءة ولا `chmod`
    // ولا محاولة فاشلة — والدليل محفوظ (سطر الرفض الأوّل وسطر التخطّي الواحد لكل عملية).
    if denied_skip_applies(path_str) {
        log_skip_once(path_str);
        return false;
    }
    let parent_name = path.parent().and_then(|p| p.file_name()).unwrap_or_default().to_string_lossy();
    let file_name = path.file_name().unwrap_or_default().to_string_lossy();
    let pathname = if parent_name.is_empty() { file_name.into_owned() } else { format!("{}/{}", parent_name, file_name) };

    if !path.exists() { return false; }

    // القراءة قبل الكتابة: القرار هنا لا في النواة.
    let current = fs::read_to_string(path).ok();
    if value_already_applied(current.as_deref(), value) {
        return false;
    }

    chmod(path_str, 0o644);

    let val_with_newline = format!("{}\n", value);
    
    if fs::write(path, val_with_newline).is_err() {
        log_verbose(&format!("Cannot write to /{} (permission denied)", pathname));
        remember_denied(path_str);
        if lock { chmod(path_str, 0o444); }
        return false;
    }

    forget_denied_if_known(path_str);
    log_verbose(&format!("Set /{} to {}", pathname, value));
    if lock { chmod(path_str, 0o444); }
    true
}

/// ذاكرة العقد غير القابلة للكتابة — «اختُبرت القدرة مرة ثم تُذكَّرت».
///
/// والدافع مقيس في حزمة ٢٠٢٦-١٠-١٥: عقد (`power_efficient` · `nr_requests` ·
/// `dvfsrc_force_vcore_dvfs_opp`) تُرفض كتابتها **في كل تطبيق ملفّ** (٦٩ · ٤٦ · ٢٣ سطر
/// رفض)، وكل رفض `stat` وقراءتان و`chmod` وكتابة فاشلة وسطر سجلّ — بلا أي احتمال نجاح.
/// فالمسار المرفوض يُسجَّل ويُتخطّى: داخل العملية من الذاكرة، وعبر العمليات من ملفّ،
/// وتُعاد تجربته بعد يوم كامل (فالعقدة قد تُكتب بعد تحديث نواة — والتخطّي الأبدي
/// يُبقي تحسينًا حبيسًا).
///
/// وصيغة الملفّ سطر لكل مسار: `epoch path` — epoch ثواني Unix وقت تسجيل الرفض.
/// والملفّ أفضل جهد: غيابه أو تعذّر كتابته لا يُسقط كتابة (تبقى ذاكرة العملية).
fn denied_cache_path() -> String {
    format!("{CONFIG_PATH}/unwritable_nodes.cache")
}

/// مُهلَة إعادة اختبار عقدة مسجّلة: يوم كامل. والاتجاه الآمن عند الشك (ساعة رجعت
/// إلى الوراء) هو إعادة الاختبار لا التخطّي — فاختبار زائد كتابة مرفوضة واحدة،
/// وتخطٍّ خاطئ تحسين ضائع.
const DENIED_REPROBE_SECS: u64 = 86_400;

/// صيغة سطر الذاكرة — خالصة تُقاس وحدها (`cargo test`).
fn format_denied_entry(path: &str, epoch_secs: u64) -> String {
    format!("{} {}", epoch_secs, path)
}

/// قراءة الذاكرة — الأسطر المعطوبة تُسقَط بصمت (ذاكرة لا عقدًا دستورًا)، والأحدث
/// يغلب الأقدم عند التكرار.
fn parse_denied_cache(text: &str) -> Vec<(String, u64)> {
    let mut out = Vec::new();
    for line in text.lines() {
        let line = line.trim();
        if line.is_empty() || line.starts_with('#') {
            continue;
        }
        let mut parts = line.splitn(2, ' ');
        let (Some(epoch_raw), Some(path_raw)) = (parts.next(), parts.next()) else {
            continue;
        };
        let (Ok(epoch), path) = (epoch_raw.parse::<u64>(), path_raw.trim()) else {
            continue;
        };
        if path.is_empty() {
            continue;
        }
        if let Some(slot) = out.iter_mut().find(|(p, _)| p == path) {
            slot.1 = epoch;
        } else {
            out.push((path.to_string(), epoch));
        }
    }
    out
}

/// هل تسجيل الرفض ما زال ساريًا؟ — خالصة تُقاس وحدها. والقديم (أو ساعة راجعة)
/// يُعاد اختباره لا يُتخطّى.
fn denied_entry_is_fresh(saved_epoch: u64, now_epoch: u64) -> bool {
    now_epoch >= saved_epoch && now_epoch - saved_epoch < DENIED_REPROBE_SECS
}

fn now_epoch_secs() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0)
}

/// ما سُجّل غير قابل للكتابة: المسار ⇒ epoch التسجيل. يُحمَّل من الملفّ مرّة واحدة.
fn denied_memory() -> &'static Mutex<HashMap<String, u64>> {
    static MEM: OnceLock<Mutex<HashMap<String, u64>>> = OnceLock::new();
    MEM.get_or_init(|| {
        let cached = fs::read_to_string(denied_cache_path())
            .map(|text| parse_denied_cache(&text))
            .unwrap_or_default();
        Mutex::new(cached.into_iter().collect())
    })
}

/// ما قيل عنه سطر تخطٍّ في هذه العملية — سطر واحد لكل مسار لا سطر لكل نداء.
fn skip_logged_paths() -> &'static Mutex<HashSet<String>> {
    static LOGGED: OnceLock<Mutex<HashSet<String>>> = OnceLock::new();
    LOGGED.get_or_init(|| Mutex::new(HashSet::new()))
}

/// هل يُتخطّى هذا المسار؟ سارٍ ⇒ نعم. منتهي الصلاحية ⇒ يُحذف من الذاكرة لتُعاد
/// تجربته الآن (والملفّ يُحدَّث عند النتيجة).
fn denied_skip_applies(path_str: &str) -> bool {
    let mut mem = denied_memory().lock().unwrap_or_else(|e| e.into_inner());
    match mem.get(path_str) {
        Some(&saved) if denied_entry_is_fresh(saved, now_epoch_secs()) => true,
        Some(_) => {
            mem.remove(path_str);
            false
        }
        None => false,
    }
}

/// تسجيل رفض: الذاكرة فورًا، والملفّ أفضل جهد (يُستبدَل سطر المسار لا يُكرَّر).
fn remember_denied(path_str: &str) {
    let epoch = now_epoch_secs();
    denied_memory()
        .lock()
        .unwrap_or_else(|e| e.into_inner())
        .insert(path_str.to_string(), epoch);
    let mut entries: Vec<(String, u64)> = fs::read_to_string(denied_cache_path())
        .map(|text| parse_denied_cache(&text))
        .unwrap_or_default()
        .into_iter()
        .filter(|(p, _)| p != path_str)
        .collect();
    entries.push((path_str.to_string(), epoch));
    let text = entries
        .iter()
        .map(|(p, e)| format_denied_entry(p, *e))
        .collect::<Vec<_>>()
        .join("\n");
    let _ = fs::write(denied_cache_path(), format!("{text}\n"));
}

/// نجاح بعد تسجيل: العقدة صارت تُكتب — يُنسى التسجيل من الذاكرة والملفّ معًا.
fn forget_denied_if_known(path_str: &str) {
    let was_known = denied_memory()
        .lock()
        .unwrap_or_else(|e| e.into_inner())
        .remove(path_str)
        .is_some();
    if !was_known {
        return;
    }
    let entries: Vec<(String, u64)> = fs::read_to_string(denied_cache_path())
        .map(|text| parse_denied_cache(&text))
        .unwrap_or_default()
        .into_iter()
        .filter(|(p, _)| p != path_str)
        .collect();
    let text = entries
        .iter()
        .map(|(p, e)| format_denied_entry(p, *e))
        .collect::<Vec<_>>()
        .join("\n");
    let _ = fs::write(denied_cache_path(), if text.is_empty() { String::new() } else { format!("{text}\n") });
}

/// سطر تخطٍّ واحد لكل مسار في العملية — الدليل يبقى (الرفض الأوّل + هذا السطر)
/// بلا سطر لكل نداء مكرّر.
fn log_skip_once(path_str: &str) {
    let fresh = skip_logged_paths()
        .lock()
        .unwrap_or_else(|e| e.into_inner())
        .insert(path_str.to_string());
    if fresh {
        log_verbose(&format!(
            "Skipping known-unwritable {} (remembered; reprobe after 24h)",
            path_str
        ));
    }
}

/// كتابة بلا تثبيت. العائد: هل وقعت كتابة فعلًا.
pub fn write_unlock(value: &str, path_str: &str) -> bool {
    write_unlock_core(value, path_str, false)
}

/// كتابة مع تثبيت العقد (`chmod 444`). العائد: هل وقعت كتابة فعلًا.
pub fn write_lock(value: &str, path_str: &str) -> bool {
    write_unlock_core(value, path_str, true)
}

pub fn systemv(command: &str) -> i32 {
    match Command::new("/system/bin/sh")
        .arg("-c")
        .arg(command)
        .env("PATH", MY_PATH)
        .status()
    {
        Ok(status) => status.code().unwrap_or(-1),
        Err(e) => {
            log_info(&format!("systemv failed for '{}': {}", command, e));
            -1
        }
    }
}

pub fn get_limiter() -> u64 {
    let val = getprop(CONF_FREQOFFSET);
    if val == "Disabled" || val.is_empty() {
        100
    } else {
        val.replace('%', "").parse().unwrap_or(100)
    }
}

/// True while the manager app's Core Grid screen holds a manual CPU frequency
/// session. Every CPU-frequency-writing function below must call this first:
/// profile re-application runs on every Max AI decision and app switch, and
/// without the guard it wipes hand-applied limits within a minute.
pub fn manual_freq_session_active() -> bool {
    getprop(MANUAL_FREQ_SESSION) == "1"
}

/// Logs and reports whether a manual frequency session blocked this call.
/// Returns true when the caller must skip its frequency writes entirely.
pub(crate) fn blocked_by_manual_session(caller: &str) -> bool {
    if manual_freq_session_active() {
        log_info(&format!(
            "Manual frequency session active: {} preserves user CPU limits",
            caller
        ));
        true
    } else {
        false
    }
}

pub fn get_debugmode() -> bool {
    getprop(DEBUG_MODE) == "true"
}

pub fn get_clearapps() -> bool {
    getprop(CONF_CLEARBG) == "1"
}

pub fn get_litemode() -> bool {
    getprop(CONF_LITEMODE) == "1"
}

pub fn get_curprofile() -> String {
    fs::read_to_string(format!("{}/API/current_profile", CONFIG_PATH))
        .unwrap_or_default()
        .trim()
        .to_string()
}

pub fn setprop_cmd(key: &str, value: &str) {
    let _ = Command::new("setprop").arg(key).arg(value).status();
}

pub fn per_app_governor_isolation_active() -> bool {
    getprop("sys.maxmanager.perapp.governor_isolation") == "1"
}

pub fn applyfreqbalance() {
    // The daemon invokes this command periodically while Balanced/Eco is
    // active. It is a separate entry point from the profile functions, so the
    // profile-level guard cannot protect it. Without this guard it reclaimed
    // per-app CPU limits immediately after the UI/arbiter verified them.
    if per_app_governor_isolation_active() {
        log_info("Per-App governor isolation active: applyfreqbalance skipped");
        return;
    }
    if Path::new("/proc/ppm").exists() {
        dsetfreqppm();
    } else {
        dsetfreq();
    }
}

pub fn applyfreqgame() {
    if per_app_governor_isolation_active() {
        log_info("Per-App governor isolation active: applyfreqgame skipped");
        return;
    }
    if Path::new("/proc/ppm").exists() {
        dsetgamefreqppm();
    } else {
        dsetgamefreq();
    }
}

/// كتابة قيم سقوف الترددات (‏`scaling_max_freq` · `scaling_min_freq` · `/proc/ppm/policy/*`)
/// مع تثبيت العقدة.
///
/// وكانت نسخةً ثانية من منطق الكتابة (تكرار `chmod` ثم `write` ثم `chmod`)، فصارت نداءً على
/// [write_unlock_core] — **نسخة واحدة للقرار واحدة للسلوك**: كل ما يُقال عن «لا كتابة بلا تغيّر»
/// في مكان واحد يسري على مساري التردّد كلّيهما. والعائد: هل وقعت كتابة فعلًا.
pub fn applyppmnfreqsets(value: &str, path: &str) -> bool {
    write_unlock_core(value, path, true)
}

pub fn which_maxfreq(path: &str) -> Option<u64> {
    read_freqs(path).into_iter().max()
}

pub fn which_minfreq(path: &str) -> Option<u64> {
    read_freqs(path).into_iter().min()
}

pub fn which_midfreq(path: &str) -> Option<u64> {
    let freqs = read_freqs(path);
    if freqs.is_empty() {
        return None;
    }
    // Ekuivalen dengan logika awk mengambil nilai tengah
    Some(freqs[freqs.len() / 2])
}

pub fn setfreqs(file: &str, target: u64) -> u64 {
    let freqs = read_freqs(file);
    if freqs.is_empty() {
        return target;
    }
    // Mencari frekuensi yang selisihnya paling sedikit dengan target (closest match)
    *freqs
        .iter()
        .min_by_key(|&&f| (f as i64 - target as i64).abs())
        .unwrap_or(&target)
}

pub fn setgov(gov: &str) {
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpu*/cpufreq/scaling_governor") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            chmod(p_str, 0o644);
            let _ = fs::write(p_str, gov);
            chmod(p_str, 0o444);
        }
    }

    // Lock additional policy paths
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_governor") {
        for path in paths.flatten() {
            chmod(path.to_str().unwrap(), 0o444);
        }
    }
}

pub fn sets_io(scheduler: &str) {
    for block in &["sda", "sdb", "sdc", "mmcblk0", "mmcblk1"] {
        let path = format!("/sys/block/{}/queue/scheduler", block);
        if Path::new(&path).exists() {
            chmod(&path, 0o644);
            let _ = fs::write(&path, scheduler);
            chmod(&path, 0o444);
        }
    }
}

pub fn setfreqppm() {
    if blocked_by_manual_session("setfreqppm") {
        return;
    }
    if !Path::new("/proc/ppm").exists() {
        return;
    }

    let limiter = get_limiter();
    let curprofile = get_curprofile();
    let mut cluster = 0;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.file_name().unwrap_or_default().to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);
            let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_max_target = cpu_maxfreq * limiter / 100;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_maxfreq = setfreqs(&avail_file, new_max_target);

            if curprofile == "3" {
                let target_min_target = cpu_maxfreq * 40 / 100;
                let new_minfreq = setfreqs(&avail_file, target_min_target);

                write_lock(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                write_lock(&format!("{} {}", cluster, new_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_maxfreq, new_minfreq));
            } else {

                write_unlock(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                write_unlock(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_maxfreq, cpu_minfreq));
            }
            cluster += 1;
        }
    }
}

pub fn setfreq() {
    if blocked_by_manual_session("setfreq") {
        return;
    }
    let limiter = get_limiter();
    let curprofile = get_curprofile();

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/*/cpufreq") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();

            let policy_name = path.parent()
                .and_then(|p: &std::path::Path| p.file_name())
                .unwrap_or_default()
                .to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);
            let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_max_target = cpu_maxfreq * limiter / 100;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_maxfreq = setfreqs(&avail_file, new_max_target);

            if curprofile == "3" {
                let target_min_target = cpu_maxfreq * 40 / 100;
                let new_minfreq = setfreqs(&avail_file, target_min_target);

                write_lock(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_lock(&new_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_maxfreq, new_minfreq));
            } else {

                write_unlock(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_unlock(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_maxfreq, cpu_minfreq));

                if let Ok(sc_paths) = glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
                    for sp in sc_paths.flatten() {
                        chmod(sp.to_str().unwrap(), 0o444);
                    }
                }
            }
        }
    }
}

pub fn setgamefreqppm() {
    if blocked_by_manual_session("setgamefreqppm") {
        return;
    }
    if !Path::new("/proc/ppm").exists() {
        return;
    }

    let litemode = get_litemode();
    let mut cluster = 0;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.file_name().unwrap_or_default().to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_midtarget = cpu_maxfreq;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_midfreq = setfreqs(&avail_file, new_midtarget);

            if litemode {
                let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                    .unwrap_or_default().trim().parse().unwrap_or(0);

                write_lock(&format!("{} {}", cluster, new_midfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                write_lock(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                write_lock(&format!("{} {}", cluster, cpu_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");             
                write_lock(&format!("{} {}", cluster, cpu_maxfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, cpu_maxfreq, new_midfreq));
            }
            cluster += 1;
        }
    }
}


pub fn setgamefreq() {
    if blocked_by_manual_session("setgamefreq") {
        return;
    }
    let litemode = get_litemode();

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/*/cpufreq") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.parent()
                .and_then(|p: &std::path::Path| p.file_name())
                .unwrap_or_default()
                .to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_midtarget = cpu_maxfreq;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_midfreq = setfreqs(&avail_file, new_midtarget);

            if litemode {
                let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                    .unwrap_or_default().trim().parse().unwrap_or(0);

                // Bugfix: Menulis ke jalur sysfs standar, bukan ke /proc/ppm
                write_unlock(&new_midfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_unlock(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                write_unlock(&cpu_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_unlock(&cpu_maxfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, cpu_maxfreq, new_midfreq));

            }
        }
    }
}

pub fn dsetfreqppm() {
    if per_app_governor_isolation_active() {
        log_info("Per-App governor isolation active: dsetfreqppm skipped");
        return;
    }
    if blocked_by_manual_session("dsetfreqppm") {
        return;
    }
    if !Path::new("/proc/ppm").exists() {
        return;
    }

    let limiter = get_limiter();
    let curprofile = get_curprofile();
    let mut cluster = 0;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);
            let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_max_target = cpu_maxfreq * limiter / 100;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_maxfreq = setfreqs(&avail_file, new_max_target);

            if curprofile == "3" {
                let target_min_target = cpu_maxfreq * 40 / 100;
                let new_minfreq = setfreqs(&avail_file, target_min_target);

                applyppmnfreqsets(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                applyppmnfreqsets(&format!("{} {}", cluster, new_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");
            } else {
                applyppmnfreqsets(&format!("{} {}", cluster, new_maxfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                applyppmnfreqsets(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");
            }
            cluster += 1;
        }
    }
}

pub fn dsetfreq() {
    if per_app_governor_isolation_active() {
        log_info("Per-App governor isolation active: dsetfreq skipped");
        return;
    }
    if blocked_by_manual_session("dsetfreq") {
        return;
    }
    let limiter = get_limiter();
    let curprofile = get_curprofile();
    let mut wrote_any = false;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/*/cpufreq") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);
            let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_max_target = cpu_maxfreq * limiter / 100;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_maxfreq = setfreqs(&avail_file, new_max_target);

            if curprofile == "3" {
                let target_min_target = cpu_maxfreq * 40 / 100;
                let new_minfreq = setfreqs(&avail_file, target_min_target);

                wrote_any |= applyppmnfreqsets(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                wrote_any |= applyppmnfreqsets(&new_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));
            } else {
                wrote_any |= applyppmnfreqsets(&new_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                wrote_any |= applyppmnfreqsets(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));
            }
        }
    }

    // تثبيت العقد (`chmod 444`) مرّة واحدة بعد الدورة، و**فقط إن وقعت كتابة**: كان يُنفّذ
    // داخل حلقة الأنوية (‏`N` مرّة على القائمة كلها)، وكان يُنفّذ حتى حين لا تُكتب قيمة
    // واحدة — أي مئات نداءات `chmod` في الدورة بلا سبب. والتثبيت نفسه لم يُحذف: هو ما
    // يمنع البائع من إعادة كتابة السقف، فيبقى حيث يجب.
    if wrote_any {
        if let Ok(sc_paths) = glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
            for sp in sc_paths.flatten() {
                chmod(sp.to_str().unwrap(), 0o444);
            }
        }
    }
}

pub fn dsetgamefreqppm() {
    if per_app_governor_isolation_active() {
        log_info("Per-App governor isolation active: dsetgamefreqppm skipped");
        return;
    }
    if blocked_by_manual_session("dsetgamefreqppm") {
        return;
    }
    if !Path::new("/proc/ppm").exists() {
        return;
    }

    let litemode = get_litemode();
    let mut cluster = 0;

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.file_name().unwrap_or_default().to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_midtarget = cpu_maxfreq;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_midfreq = setfreqs(&avail_file, new_midtarget);

            if litemode {
                let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                    .unwrap_or_default().trim().parse().unwrap_or(0);

                write_unlock(&format!("{} {}", cluster, new_midfreq), "/proc/ppm/policy/hard_userlimit_max_cpu_freq");
                write_unlock(&format!("{} {}", cluster, cpu_minfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                applyppmnfreqsets(&format!("{} {}", cluster, new_midfreq), "/proc/ppm/policy/hard_userlimit_min_cpu_freq");
            }
            cluster += 1;
        }
    }
}

pub fn dsetgamefreq() {
    if per_app_governor_isolation_active() {
        log_info("Per-App governor isolation active: dsetgamefreq skipped");
        return;
    }
    if blocked_by_manual_session("dsetgamefreq") {
        return;
    }
    let litemode = get_litemode();

    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/*/cpufreq") {
        for path in paths.flatten() {
            let p_str = path.to_str().unwrap();
            let policy_name = path.parent()
                .and_then(|p: &std::path::Path| p.file_name())
                .unwrap_or_default()
                .to_string_lossy();

            let cpu_maxfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_max_freq", p_str))
                .unwrap_or_default().trim().parse().unwrap_or(0);

            let new_midtarget = cpu_maxfreq;
            let avail_file = format!("{}/scaling_available_frequencies", p_str);
            let new_midfreq = setfreqs(&avail_file, new_midtarget);

            if litemode {
                let cpu_minfreq: u64 = fs::read_to_string(format!("{}/cpuinfo_min_freq", p_str))
                    .unwrap_or_default().trim().parse().unwrap_or(0);

                write_unlock(&new_midfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                write_unlock(&cpu_minfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                log_info(&format!("Set {} maxfreq={} minfreq={}", policy_name, new_midfreq, cpu_minfreq));
            } else {
                let wrote_max = applyppmnfreqsets(&cpu_maxfreq.to_string(), &format!("{}/scaling_max_freq", p_str));
                let wrote_min = applyppmnfreqsets(&new_midfreq.to_string(), &format!("{}/scaling_min_freq", p_str));

                // تثبيت العقد بعد كتابة فعلية لا بعد كل نداء (نفس قرار `dsetfreq`).
                if wrote_max || wrote_min {
                    if let Ok(sc_paths) = glob("/sys/devices/system/cpu/cpufreq/policy*/scaling_*_freq") {
                        for sp in sc_paths.flatten() {
                            chmod(sp.to_str().unwrap(), 0o444);
                        }
                    }
                }
            }
        }
    }
}

pub fn devfreq_max_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let Some(max_freq) = which_maxfreq(&avail) {
        write_unlock(&max_freq.to_string(), &format!("{}/max_freq", path));
        write_unlock(&max_freq.to_string(), &format!("{}/min_freq", path));
    }
}

pub fn devfreq_mid_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let (Some(max_freq), Some(mid_freq)) = (which_maxfreq(&avail), which_midfreq(&avail)) {
        write_lock(&max_freq.to_string(), &format!("{}/max_freq", path));
        write_unlock(&mid_freq.to_string(), &format!("{}/min_freq", path));
    }
}

pub fn devfreq_unlock(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let (Some(max_freq), Some(min_freq)) = (which_maxfreq(&avail), which_minfreq(&avail)) {
        write_lock(&max_freq.to_string(), &format!("{}/max_freq", path));
        write_lock(&min_freq.to_string(), &format!("{}/min_freq", path));
    }
}

pub fn devfreq_min_perf(path: &str) {
    let avail = format!("{}/available_frequencies", path);
    if !Path::new(&avail).exists() { return; }

    if let Some(freq) = which_minfreq(&avail) {
        write_lock(&freq.to_string(), &format!("{}/min_freq", path));
        write_lock(&freq.to_string(), &format!("{}/max_freq", path));
    }
}


pub fn clear_background_apps() {
    // Menjalankan dumpsys window displays
    let output = Command::new("dumpsys").args(["window", "displays"]).output();
    let stdout = match output {
        Ok(o) => String::from_utf8_lossy(&o.stdout).into_owned(),
        Err(_) => return,
    };

    let mut visible_pkgs = HashSet::new();
    let mut invisible_pkgs = HashSet::new();

    // Membaca output per baris (setara grep "Task{")
    for line in stdout.lines() {
        if line.contains("Task{") {
            // Replikasi logika sed: 's/.*A=[0-9]*:\([^ ]*\).*/\1/p'
            if let Some(a_idx) = line.find("A=") {
                let part = &line[a_idx..];
                if let Some(colon_idx) = part.find(':') {
                    if let Some(space_idx) = part.find(' ') {
                        if colon_idx < space_idx {
                            let pkg = &part[colon_idx + 1..space_idx];

                            if line.contains("visible=true") {
                                visible_pkgs.insert(pkg.to_string());
                            } else if line.contains("visible=false") {
                                invisible_pkgs.insert(pkg.to_string());
                            }
                        }
                    }
                }
            }
        }
    }

    let exclude = ["com.android.systemui", "com.android.settings", "android", "system"];

    for pkg in invisible_pkgs {
        if visible_pkgs.contains(&pkg) {
            continue;
        }

        let is_excluded = exclude.iter().any(|&ex| pkg.contains(ex));

        if !is_excluded {
            let _ = Command::new("am")
                .args(["force-stop", &pkg])
                .status();

            log_verbose(&format!("Stopped app: {}", pkg));
        }
    }

    log_info("Cleared background apps");
}

pub fn get_mtk_gpu_max_freq() -> Option<u64> {
    let content = fs::read_to_string("/proc/gpufreq/gpufreq_opp_dump").unwrap_or_default();
    content.lines()
        .filter(|line: &&str| line.contains("freq = "))
        .filter_map(|line: &str| line.split("freq = ").nth(1)?.split_whitespace().next()?.parse::<u64>().ok())
        .max()
}

pub fn read_freqs(path: &str) -> Vec<u64> {
    let mut freqs: Vec<u64> = fs::read_to_string(path)
        .unwrap_or_default()
        .split_whitespace()
        .filter_map(|s: &str| s.parse().ok())
        .collect();
    freqs.sort_unstable();
    freqs
}

pub fn ppm_fix_freq(target_index: &str) {
    // A manual Core Grid session owns CPU limits: refuse both pins and
    // releases here. The "-1" calls from mediatek_balance/powersave are part
    // of profile re-application too, and refusing the whole function keeps
    // the user's state authoritative on every path.
    if blocked_by_manual_session("ppm_fix_freq") {
        return;
    }

    let ppm_path = "/proc/ppm/policy/ut_fix_freq_idx";

    if !Path::new(ppm_path).exists() {
        return;
    }

    let mut cluster_count = 0;
    if let Ok(paths) = glob::glob("/sys/devices/system/cpu/cpufreq/policy*") {
        cluster_count = paths.filter_map(Result::ok).count();
    }

    if cluster_count > 0 {
        let payload = vec![target_index; cluster_count].join(" ");

        write_lock(&payload, ppm_path);
        
    }
}

pub fn init_cpu_governor() {
    let cpu_path = "/sys/devices/system/cpu/cpu0/cpufreq";
    let gov_file = format!("{}/scaling_governor", cpu_path);
    chmod(&gov_file, 0o644);

    let mut default_gov = fs::read_to_string(&gov_file)
        .unwrap_or_default()
        .trim()
        .to_string();
        
    let performance_gov = "performance";
    let powersave_gov = "powersave";

    setprop_cmd(CPU_GOV_DEFAULT, &default_gov);
    log_info(&format!("Default CPU governor detected: {}", default_gov));

    // Handle fallback if default is 'performance'
    if default_gov == "performance" && getprop(CPU_GOV_CUSTOM_DEFAULT).is_empty() {
        log_info("Default governor is 'performance'");
        let avail_govs = fs::read_to_string(format!("{}/scaling_available_governors", cpu_path)).unwrap_or_default();
        let fallbacks = [
            "scx", "schedhorizon", "walt", "sched_pixel", "sugov_ext", "uag",
            "schedplus", "energy_step", "ondemand", "schedutil", "interactive",
            "conservative", "powersave"
        ];

        for gov in &fallbacks {
            if avail_govs.contains(gov) {
                setprop_cmd(CPU_GOV_DEFAULT, gov);
                default_gov = gov.to_string();
                log_info(&format!("Fallback governor to: {}", gov));
                break;
            }
        }
    }

    // Apply custom governor if set
    let custom_gov = getprop(CPU_GOV_CUSTOM_DEFAULT);
    if !custom_gov.is_empty() {
        default_gov = custom_gov;
    }
    
    log_info(&format!("Using CPU governor: {}", default_gov));
    setgov(&default_gov);

    // Set fallback props
    if getprop(CPU_GOV_CUSTOM_POWERSAVE).is_empty() {
        setprop_cmd(CPU_GOV_CUSTOM_POWERSAVE, &powersave_gov);
    }
    if getprop(CPU_GOV_CUSTOM_PERFORMANCE).is_empty() {
        setprop_cmd(CPU_GOV_CUSTOM_PERFORMANCE, &performance_gov);
    }
    
    log_info("Parsing CPU Governor complete");
}

pub fn init_io_scheduler() {
    let mut io_path = String::new();
    for dev in &["mmcblk0", "mmcblk1", "sda", "sdb", "sdc"] {
        let p = format!("/sys/block/{}/queue", dev);
        if Path::new(&format!("{}/scheduler", p)).exists() {
            io_path = p;
            log_info(&format!("Detected valid block device: {}", dev));
            break;
        }
    }

    if io_path.is_empty() {
        log_info("No valid block device with scheduler found");
        std::process::exit(1);
    }

    let sched_file = format!("{}/scheduler", io_path);
    chmod(&sched_file, 0o644);

    // Parse active IO scheduler (the one inside brackets [])
    let mut default_io = String::new();
    let sched_content = fs::read_to_string(&sched_file).unwrap_or_default();
    if let Some(start) = sched_content.find('[') {
        if let Some(end) = sched_content[start..].find(']') {
            default_io = sched_content[start + 1..start + end].to_string();
        }
    }

    setprop_cmd(IO_SCHED_DEFAULT, &default_io);
    log_info(&format!("Default IO Scheduler detected: {}", default_io));

    // Apply custom IO if set
    let custom_io = getprop(IO_SCHED_CUSTOM_DEFAULT);
    if !custom_io.is_empty() {
        default_io = custom_io;
    }
    
    log_info(&format!("Using IO Scheduler: {}", default_io));
    sets_io(&default_io);

    // Set fallback props
    if getprop(IO_SCHED_CUSTOM_POWERSAVE).is_empty() {
        setprop_cmd(IO_SCHED_CUSTOM_POWERSAVE, &default_io);
    }
    if getprop(IO_SCHED_CUSTOM_PERFORMANCE).is_empty() {
        setprop_cmd(IO_SCHED_CUSTOM_PERFORMANCE, &default_io);
    }
    
    log_info("Parsing IO Scheduler complete");
}

pub fn init_maligpu_governor() {
    let mut gpu_path = String::new();
    
    if let Ok(paths) = glob::glob("/sys/class/devfreq/*.mali") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                gpu_path = p_str.to_string();
                log_info(&format!("Detected Mali GPU path: {}", gpu_path));
                break;
            }
        }
    }

    if gpu_path.is_empty() {
        return;
    }

    let gov_file = format!("{}/governor", gpu_path);
    chmod(&gov_file, 0o644);

    let mut default_maligpu_gov = fs::read_to_string(&gov_file)
        .unwrap_or_default()
        .trim()
        .to_string();

    setprop_cmd(MALIGPU_GOV_DEFAULT, &default_maligpu_gov);
    log_info(&format!("Default Mali GPU governor detected: {}", default_maligpu_gov));

    let custom_maligpu_gov = getprop(MALIGPU_GOV_CUSTOM_DEFAULT);
    if !custom_maligpu_gov.is_empty() {
        default_maligpu_gov = custom_maligpu_gov;
    }

    log_info(&format!("Using Mali GPU governor: {}", default_maligpu_gov));
    write_lock(&default_maligpu_gov, &gov_file);

    if getprop(MALIGPU_GOV_CUSTOM_POWERSAVE).is_empty() {
        setprop_cmd(MALIGPU_GOV_CUSTOM_POWERSAVE, &default_maligpu_gov);
    }
    if getprop(MALIGPU_GOV_CUSTOM_PERFORMANCE).is_empty() {
        setprop_cmd(MALIGPU_GOV_CUSTOM_PERFORMANCE, &default_maligpu_gov);
    }

    log_info("Parsing Mali GPU Governor complete");
}

pub fn sets_mali_gov(gov: &str) {
    if let Ok(paths) = glob("/sys/class/devfreq/*.mali/governor") {
        for path in paths.flatten() {
            if let Some(p_str) = path.to_str() {
                chmod(p_str, 0o644);
                let _ = fs::write(p_str, gov);
                chmod(p_str, 0o444); 
            }
        }
    }
}

pub fn is_tweak_disabled() -> bool {
    let disable_tweak = getprop(DISABLE_TWEAK);
    disable_tweak == "1"
}

pub fn apply_custom_governor_io(perf_gov: &str, perf_io: &str, mali_gov: &str) {
    setgov(perf_gov);
    log_info(&format!("Applying governor to : {}", perf_gov));
    
    if !perf_io.is_empty() {
        sets_io(perf_io);
        log_info(&format!("Applying I/O scheduler to : {}", perf_io));
    }
    
    if !mali_gov.is_empty() {
        sets_mali_gov(mali_gov);
    }
}

pub fn setrender(renderer: &str) {
    if renderer == "default" || renderer.is_empty() {
        setprop("debug.hwui.renderer", "");
        setprop("debug.renderengine.backend", "");
        setprop("debug.hwui.render_thread", "");
        setprop("debug.skia.threaded_mode", "");
        resetprop("ro.hwui.use_vulkan", ""); 
        
        log_info("Resetting all renderers to system default");
        return;
    }

    setprop("debug.hwui.renderer", renderer);

    if renderer.contains("threaded") {
        setprop("debug.hwui.render_thread", "true");
        if renderer.contains("skia") {
            setprop("debug.skia.threaded_mode", "true");
        } else {
            setprop("debug.skia.threaded_mode", "false");
        }
    } else {
        setprop("debug.hwui.render_thread", "false");
        setprop("debug.skia.threaded_mode", "false");
    }

    match renderer {
        "skiavk" | "skiavkthreaded" | "vulkan" => {
            setprop("debug.renderengine.backend", "vulkan");
            resetprop("ro.hwui.use_vulkan", "true"); 
        }
        "skiagl" | "skiaglthreaded" | "gles" | "opengl" | "openglthreaded" => {
            setprop("debug.renderengine.backend", "gles");
            resetprop("ro.hwui.use_vulkan", "false");
        }
        "software" => {
            setprop("debug.renderengine.backend", "");
            resetprop("ro.hwui.use_vulkan", "false");
        }
        _ => {
            if renderer.contains("vk") || renderer.contains("vulkan") {
                setprop("debug.renderengine.backend", "vulkan");
                resetprop("ro.hwui.use_vulkan", "true");
            } else if renderer.contains("gl") || renderer.contains("gles") {
                setprop("debug.renderengine.backend", "gles");
                resetprop("ro.hwui.use_vulkan", "false");
            } else {
                setprop("debug.renderengine.backend", "");
            }
        }
    }
    log_info(&format!("Successfully applied renderer: {}", renderer));
}

pub fn init_renderer() {
    let renderer = getprop(CONF_RENDERER);
    
    if renderer.is_empty() || renderer.eq_ignore_ascii_case("default") {
        log_info("Renderer setting is default, skipping renderer setup");
        return;
    }
    
    log_info(&format!("Applying renderer: {}", renderer));
    setrender(&renderer);
}

#[cfg(test)]
mod tests {
    use super::*;

    /// **الجوهر المقيس:** القيمة نفسها ⇒ لا كتابة. وهذا هو الفرق بين دورة تُشغّل سياسة التردد
    /// في النواة من جديد، ودورة لا تعمل شيئًا.
    #[test]
    fn an_unchanged_value_is_not_written() {
        assert!(value_already_applied(Some("1804800\n"), "1804800"));
        assert!(value_already_applied(Some("  1804800  "), "1804800"));
    }

    /// والقيمة المخالفة تُكتب — الحرس لا يمنع الإصلاح ولا إعادة التثبيت بعد أن يعبث بها البائع.
    #[test]
    fn a_different_value_is_written() {
        assert!(!value_already_applied(Some("1996800\n"), "1804800"));
        assert!(!value_already_applied(Some(""), "1804800"));
    }

    /// العقدة التي لم تُقرأ (`None`) لا تُحسب «قيمة مطابقة»: تُكتب كما كانت تُكتب دائمًا،
    /// فلا يُخترع تطابق من عقدة محجوبة أو غائبة.
    #[test]
    fn an_unreadable_node_is_never_assumed_equal() {
        assert!(!value_already_applied(None, "1804800"));
    }

    /// صيغة `/proc/ppm` متعدّدة القيم: الاختلاف في الفراغات وحدها ليس اختلافًا في القيمة،
    /// والاختلاف في رقمٍ واحد اختلافٌ حقيقي.
    #[test]
    fn ppm_multi_value_form_is_compared_by_content() {
        assert!(value_already_applied(Some("0  1804800\n"), "0 1804800"));
        assert!(!value_already_applied(Some("0 1996800\n"), "0 1804800"));
        assert!(!value_already_applied(Some("0\n"), "0 1804800"));
    }

    /// صيغة الذاكرة `epoch path` ذهابًا وإيابًا — والمعطوب يُسقَط لا يُرمى به.
    #[test]
    fn denied_cache_roundtrip_keeps_only_well_formed_lines() {
        let text = "# comment\n\n1700000000 /sys/a\nnotanepoch /sys/b\n1700000001\n1700000002 /sys/a\n";
        let parsed = parse_denied_cache(text);
        assert_eq!(parsed, vec![("/sys/a".to_string(), 1700000002)]);
        let line = format_denied_entry("/sys/a", 1700000002);
        assert_eq!(line, "1700000002 /sys/a");
        assert_eq!(parse_denied_cache(&format!("{line}\n")), vec![("/sys/a".to_string(), 1700000002)]);
    }

    /// الصلاحية يوم كامل — والقديم أو ساعة راجعة يُعاد اختباره (الاتجاه الآمن).
    #[test]
    fn denied_freshness_allows_reprobe_after_a_day() {
        assert!(denied_entry_is_fresh(1_000_000, 1_000_000 + 86_399));
        assert!(!denied_entry_is_fresh(1_000_000, 1_000_000 + 86_400));
        assert!(!denied_entry_is_fresh(1_000_000, 1_000_000 + 200_000));
        assert!(!denied_entry_is_fresh(1_000_000, 999_999));
    }
}

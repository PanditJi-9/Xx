// Copyright (C) 2026-2027 Zexshia
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//! قراءة الخصائص **داخل العملية** بدل ولادة `getprop` لكل سؤال.
//!
//! **العطب المقيس الذي وُلد منه هذا الملف (جولة الحرارة واللاغ):** كان كل `getprop` في هذه
//! الثنائية ولادة عملية كاملة (`fork`+`exec`+تحميل `/system/bin/getprop`) — وقيست في جولة
//! قارئ Rust في التطبيق بـ**٢٣٦٦ ميكرو** مقابل **١٢٫٦ ميكرو** للقراءة داخل العملية (**×١٨٧**).
//! وهذه الثنائية تعمل في **مسار ساخن**: الخادم يناديها (`applyfreqbalance` ·
//! `applyfreqgame` · `%d` لكل تبديل)، وكل نداء منها كان يقرأ خصائصه بولادات خارجية
//! (`get_limiter` · `per_app_governor_isolation_active` · `manual_freq_session_active` ·
//! `get_debugmode` في كل كتابة عبر `log_verbose`). فصار كل نداء خادم = عشرات العمليات
//! قصيرة العمر على أنوية المعالج — وهذا بعينه ما يُقاس حرارةً واستهلاكًا على الجهاز.
//!
//! **والدلالة لا تتغيّر:** القراءة من bionic هي **نفس** ما يقرؤه `getprop` (كلاهما على
//! `__system_property_get`)، فالفرق كلفة لا معنى. و`None` تعني «لم أستطع القراءة هنا»
//! (مضيف بلا bionic) — فيعود المتصل إلى `getprop` كما كان، ولا تُخترع قيمة باسم خصيصة.

use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_uchar};

#[cfg(target_os = "android")]
#[link(name = "c")]
unsafe extern "C" {
    fn __system_property_get(name: *const c_uchar, value: *mut c_uchar) -> c_int;
}

/// بديل المضيف: نفس التوقيع بلا قراءة — والدعاوى لا تعتمد على قيم خصائص حقيقية.
#[cfg(not(target_os = "android"))]
unsafe extern "C" fn __system_property_get(_name: *const c_uchar, value: *mut c_uchar) -> c_int {
    if !value.is_null() {
        unsafe { *value = 0 };
    }
    0
}

/// أقصى طول لقيمة خصيصة في أندرويد (`PROP_VALUE_MAX`) — نفس المخزن في `getprop`.
const PROP_VALUE_MAX: usize = 92;

/// قراءة خصيصة واحدة.
///
/// `Some("")` = «الخصيصة غير مضبوطة» (حكم)، و`None` = «لا أستطيع القراءة هنا» — ولا
/// تُخلط الحالتان: من يقرأ `Some("")` يعرف أنها مضبوطة على الفراغ، ومن يقرأ `None` يعود
/// إلى الطريق الاحتياطي بدل أن يقرأ فراغًا كأنه قيمة.
pub fn read_property(name: &str) -> Option<String> {
    let key = CString::new(name).ok()?;
    let mut value = [0u8; PROP_VALUE_MAX];

    let len = unsafe {
        __system_property_get(
            key.as_ptr() as *const c_uchar,
            value.as_mut_ptr() as *mut c_uchar,
        )
    };

    // على المضيف يعود صفرًا دائمًا ⇒ `None` ⇒ الطريق الاحتياطي (`getprop`) — سلوك اليوم حرفيًّا.
    if len <= 0 {
        return None;
    }

    let text = unsafe { CStr::from_ptr(value.as_ptr() as *const c_char) }
        .to_string_lossy()
        .into_owned();
    Some(text)
}

#[cfg(test)]
mod tests {
    use super::*;

    /// على المضيف: الغياب يُعلن (`None`) ولا يُقرأ فراغًا — وهذا هو الفرق الذي يمنع
    /// «قرأت فراغًا فكأنّ شيئًا لم يُضبط» في بيئة لا bionic فيها.
    #[test]
    fn host_reports_unavailable_instead_of_an_empty_value() {
        assert_eq!(read_property("persist.sys.maxmanager.freqoffset"), None);
    }

    /// اسم فيه محرف صفري لا يُمرَّر إلى C (والـ`CString` يرفضه) — والقرار `None` لا انهيار.
    #[test]
    fn nul_in_name_is_refused_not_panicked() {
        assert_eq!(read_property("bad\0key"), None);
    }
}

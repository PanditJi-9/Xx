# MaxManager — دليل التفليش والدمج في الرومات
# MaxManager — Flashing & Custom-ROM Guide

> **النطاق / Scope:** هذا الدليل للمستخدم الذي يفلش الوحدة، ولمعدّل الروم الذي ينقل ملفّات إلى صور
> `product` و`system`. **ليس** دليل بناء AOSP من المصادر.
>
> This guide is for users flashing the module and for custom-ROM image modders copying files into
> `product` and `system` images. It is **not** an AOSP source-build guide.
>
> This file sits at the **root of the module zip** (`README.md`) and is installed to
> `/data/adb/modules/MaxManager/README.md`.
>
> هذا الملفّ في **جذر حزمة الوحدة** (`README.md`)، ويُثبَّت في `/data/adb/modules/MaxManager/README.md`.

---

## بالعربية

### ١) المستخدم العادي — تفليش الوحدة

**المتطلبات:** أندرويد 10 أو أحدث (API 29) · معمارية `arm64-v8a` أو `armeabi-v7a` · مدير جذر
(Magisk · KernelSU · KernelSU Next · APatch).

نزّل **`MaxManager-<version>.zip`** من صفحة الإصدارات (مثال: `MaxManager-V4.zip`). لا تستعمل
`MaxManager-developer-bundle.zip` للتفليش؛ تلك حزمة معدّلي الرومات.

1. افتح مدير الجذر ← **Modules**.
2. **Install from storage** ← اختر ملفّ الوحدة.
3. انتظر انتهاء التثبيت، ثم **أعد تشغيل الجهاز**.
4. افتح **MaxManager** وامنحه الجذر عند الطلب. إن لم تظهر الأيقونة، افتح بطاقة الوحدة واضغط زر
   **Action**.
5. **لا تنقل التطبيق إلى `/system` يدويًّا.** الوحدة تثبّته كتطبيق مستخدم (`/data/app`) وتمنح
   أذوناته التشغيلية عند الإقلاع من `service.sh`.

**التحديث:** فلّش النسخة الأحدث فوق القديمة، ثم أعد التشغيل. تبقى البيانات والإعدادات.

**الإزالة:** Modules ← MaxManager ← **Remove** ← أعد التشغيل. تُزال الوحدة والتطبيق وبياناته
(`/data/data/nd.max`) وإعداداته (`/data/adb/.config/MaxManager`).

**الأذونات التي تمنحها الوحدة تلقائيًّا عند الإقلاع:** الظهور فوق التطبيقات الأخرى · تعديل إعدادات
النظام · الوصول إلى بيانات الاستخدام · الوصول إلى كل الملفّات · تثبيت تطبيقات مجهولة المصدر.
إذا كان رومك يتجاهل `appops`، امنحها يدويًّا من: **الإعدادات ← التطبيقات ← MaxManager ← صلاحيات
خاصة**. ومساحة الألعاب (Game Space) تعرض إشعار «منح الصلاحية» إذا كانت صلاحية التراكب ناقصة.

**التحقّق السريع:**

```sh
su -c 'pidof sys.maxmanager-service'
su -c 'ls -l /data/adb/modules/MaxManager/system/bin/sys.maxmanager-service'
```

**إن لم يبدأ الخادم:** راجع `/data/adb/.config/MaxManager/package-recovery.log`، ثم أعد التشغيل.
وإن تكرّر توقّف الإقلاع، أنشئ الملفّ `/data/adb/modules/MaxManager/disable` من مدير الجذر أو من
`adb` وأعد التشغيل؛ ستُعطَّل الوحدة وحدها.

---

### ٢) معدّل الروم — نسخ الملفّات إلى الصورة

استعمل **`MaxManager-developer-bundle.zip`** (لا وحدة Magisk). داخله مجلّد واحد اسمه
`maxmanager/` يحاكي مسارات الوجهة الحقيقية:

| من الحزمة | إلى الصورة / الجهاز | الوضع |
| --- | --- | --- |
| `maxmanager/system/bin/sys.maxmanager-service` | `/system/bin/sys.maxmanager-service` | `0755` · `root:root` أو `root:shell` |
| `maxmanager/product/priv-app/MaxManager/MaxManager.apk` | `/product/priv-app/MaxManager/MaxManager.apk` | `0644` |
| `maxmanager/product/etc/permissions/privapp-permissions-nd.max.xml` | `/product/etc/permissions/privapp-permissions-nd.max.xml` | `0644` |

**لماذا هذه المسارات بالذات؟**

- التطبيق في `product/priv-app` تطبيقٌ مُمتَزَج، وملفّ الصلاحيات يجب أن يكون على **القسم نفسه**:
  `product/etc/permissions`. لا تضعه في `system/etc/permissions` مع تطبيق في `product`.
- اسم الملفّ يبقى `privapp-permissions-nd.max.xml`، واسم الحزمة داخله `nd.max` هو مفتاح المطابقة.
- الخادم في `/system/bin` **وحده**؛ ملفّ `maxmanager.rc` و`file_contexts` يشيران إلى هذا المسار
  حرفيًّا. لا تضعه في `/vendor/bin` ولا `/product/bin`.

**لتشغيل الخادم فعلًا على روم enforcing**، انسخ معه من الحزمة أيضًا:

- `maxmanager/system/etc/init/maxmanager.rc` ← `/system/etc/init/maxmanager.rc`
- `maxmanager/sepolicy/maxmanager.te` و`maxmanager/sepolicy/file_contexts` ← مصادر `sepolicy`
  في رومك (أو ادمجهما بالطريقة المعتادة في شجرة رومك).

هذان الملفّان ليسا زخرفة: بلا خدمة `init` لن يبدأ الخادم، وبلا وسم `maxmanager_exec` سيمنعه
SELinux. أمّا `BoardConfig.mk` و`product-inclusion.mk` في الحزمة فهما لمن يبني من المصادر؛
هذا الدليل لا يشرح بناء AOSP.

وإن كنت تعيد حزم صورة جاهزة فقط **ولا تستطيع إعادة بناء sepolicy**، فالأفضل أن يستعمل المستخدم
وحدة Magisk بدل دمج التطبيق في الصورة؛ نسخ الملفّات وحدها لا يكفي إذا كان الروم enforcing.

وديمون الحزمة **64-بت (arm64)**. أما روم 32-بت فيأخذ `sys.maxmanager-service` من عمود
`armeabi-v7a` في حزمة الوحدة/المصادر ويضعه في المسار نفسه `/system/bin/`.

**مهم — الصلاحيات:**

ملفّ XML يمنح **فقط** الصلاحيتين المُمتَزَجتين:

- `android.permission.WRITE_SECURE_SETTINGS`
- `android.permission.PACKAGE_USAGE_STATS`

أمّا الظهور فوق التطبيقات الأخرى، وتعديل إعدادات النظام، والوصول إلى كل الملفّات، وتثبيت تطبيقات
مجهولة المصدر، والوصول إلى بيانات الاستخدام فهي **appops** لا تمنحها قائمة `privapp`. امنحها
للمستخدم عند أول تشغيل، أو أضف منحًا مسبقًا في رومك إن كان يدعم ذلك. ومساحة الألعاب تعرض إشعار
الصلاحية وتفتح الإعداد المناسب إذا كانت صلاحية التراكب ناقصة.

**لا تخلط المسارين:** لا تفلّش وحدة Magisk على روم يحمل هذه الملفّات أصلًا، ولا تنسخ تطبيق
`app/MaxManager.apk` من حزمة الوحدة إلى `product`؛ استعمل نسخة حزمة المطوّرين. النسختان من البناء
نفسه، لكن مسار التركيب وملكية الصلاحيات مختلفان.

---

### ٣) أيّ ملفّ أختار؟

| حالتك | الملفّ |
| --- | --- |
| مستخدم عادي · جهاز بجذر | `MaxManager-<version>.zip` (وحدة Magisk/KernelSU/APatch) |
| معدّل روم · تعديل صور `product`/`system` | `MaxManager-developer-bundle.zip` |
| بانٍ AOSP من المصادر | راجع `README.txt` داخل حزمة المطوّرين؛ هذا الدليل ليس دليل بناء |

---

### ٤) التحقّق بعد الإقلاع

```sh
adb shell ls -l /system/bin/sys.maxmanager-service
adb shell ls -l /product/priv-app/MaxManager/MaxManager.apk
adb shell ls -l /product/etc/permissions/privapp-permissions-nd.max.xml
adb shell pidof sys.maxmanager-service
adb shell getprop | grep maxmanager
adb shell dumpsys package nd.max | grep -E 'versionName|granted=true'
adb shell appops get nd.max SYSTEM_ALERT_WINDOW
```

الخادم يبدأ عند `sys.boot_completed=1`، فقد يظهر `pidof` فارغًا في أول ثوانٍ بعد الإقلاع. وإن
ظهر `avc: denied` في `logcat` باسم `maxmanager`، فسياسة SELinux لم تُدمج.

---

### ٥) حلّ المشكلات

| العَرَض | العلاج |
| --- | --- |
| الوحدة مثبّتة ولا تطبيق | افتح **Action** في بطاقة الوحدة، أو راجع `package-recovery.log`، ثم أعد التشغيل |
| اللوحة الجانبية لا تظهر | امنح **الظهور فوق التطبيقات الأخرى** من الصلاحيات الخاصة؛ Game Space يعرض الإشعار |
| الخادم لا يعمل بعد نسخ ملفّات الروم | تأكّد من `maxmanager.rc` و`sepolicy`، ثم `logcat \| grep avc` |
| الوحدة تسبّب توقّف إقلاع | أنشئ `/data/adb/modules/MaxManager/disable` وأعد التشغيل |
| تطبيق يعمل بلا تحكّم كامل | امنح الجذر، وتأكّد أن الخادم يعمل: `pidof sys.maxmanager-service` |

---

## English

### 1) Regular users — flash the module

**Requirements:** Android 10+ (API 29) · `arm64-v8a` or `armeabi-v7a` · a root manager
(Magisk · KernelSU · KernelSU Next · APatch).

Download **`MaxManager-<version>.zip`** from the Releases page (for example `MaxManager-V4.zip`).
Do **not** flash `MaxManager-developer-bundle.zip`; that one is for ROM modders.

1. Open your root manager → **Modules**.
2. **Install from storage** → select the module zip.
3. Wait for the installer to finish, then **reboot**.
4. Open **MaxManager** and grant root when asked. If the icon is missing, open the module card and
   press **Action**.
5. **Do not move the app to `/system` manually.** The module installs it as a normal user app
   (`/data/app`) and applies its runtime appops at boot from `service.sh`.

**Update:** flash the newer zip over the old one, then reboot. Data and settings are kept.

**Uninstall:** Modules → MaxManager → **Remove** → reboot. The module removes the app, its data
(`/data/data/nd.max`) and its settings (`/data/adb/.config/MaxManager`).

**Permissions the module grants at boot:** display over other apps · modify system settings ·
usage access · all-files access · install unknown apps. If your ROM ignores `appops`, grant them
manually under **Settings → Apps → MaxManager → Special access**. Game Space shows a
**Grant permission** notice when overlay access is missing.

**Quick check:**

```sh
su -c 'pidof sys.maxmanager-service'
su -c 'ls -l /data/adb/modules/MaxManager/system/bin/sys.maxmanager-service'
```

**If the daemon does not start:** check `/data/adb/.config/MaxManager/package-recovery.log`, then
reboot. If boot keeps failing, create `/data/adb/modules/MaxManager/disable` from your root manager
or `adb` and reboot; only the module is disabled.

---

### 2) Custom-ROM image modders — copy the three artifacts

Use **`MaxManager-developer-bundle.zip`** (not the Magisk module). It contains one `maxmanager/`
folder laid out with the real destination paths:

| From the bundle | To the image / device | Mode |
| --- | --- | --- |
| `maxmanager/system/bin/sys.maxmanager-service` | `/system/bin/sys.maxmanager-service` | `0755` · `root:root` or `root:shell` |
| `maxmanager/product/priv-app/MaxManager/MaxManager.apk` | `/product/priv-app/MaxManager/MaxManager.apk` | `0644` |
| `maxmanager/product/etc/permissions/privapp-permissions-nd.max.xml` | `/product/etc/permissions/privapp-permissions-nd.max.xml` | `0644` |

**Why exactly these paths?**

- The app in `product/priv-app` is privileged, so its allowlist must live on the **same partition**:
  `product/etc/permissions`. Do not put it under `system/etc/permissions` for a product app.
- Keep the file name `privapp-permissions-nd.max.xml`; the package name inside it (`nd.max`) is the
  match key.
- The daemon belongs in `/system/bin` **only**. Both `maxmanager.rc` and `file_contexts` point at that
  exact path. Do not place it in `/vendor/bin` or `/product/bin`.

**For the daemon to actually start on an enforcing ROM**, also copy:

- `maxmanager/system/etc/init/maxmanager.rc` → `/system/etc/init/maxmanager.rc`
- `maxmanager/sepolicy/maxmanager.te` and `maxmanager/sepolicy/file_contexts` → your ROM's sepolicy
  sources (or merge them the usual way for your tree).

These are not optional decoration: without the init service the daemon never starts, and without the
`maxmanager_exec` label SELinux blocks it. `BoardConfig.mk` and `product-inclusion.mk` in the bundle
are for source builds; this guide does not cover AOSP source builds.

If you only unpack/repack a finished image and **cannot rebuild sepolicy**, use the Magisk module
path for the user instead of integrating the app; copying the files alone will not start the daemon
on an enforcing ROM.

The bundle's `system/bin` daemon is **arm64**. For a 32-bit ROM, take `sys.maxmanager-service` from
the `armeabi-v7a` column in the module/source tree and place it at the same `/system/bin/` path.

**Important — permissions:**

The XML grants **only** the two privileged permissions:

- `android.permission.WRITE_SECURE_SETTINGS`
- `android.permission.PACKAGE_USAGE_STATS`

Overlay (`SYSTEM_ALERT_WINDOW`), modify-settings, all-files, install-unknown-apps and usage access
are **appops**, not `privapp` permissions. Grant them on first launch, or pre-grant them in your ROM
if it supports that. Game Space shows the permission notice and opens the right system screen when
overlay access is missing.

**Do not mix the two paths:** do not flash the Magisk module on a ROM that already carries these
files, and do not copy the module's `app/MaxManager.apk` into `product`; use the developer-bundle
APK. Both come from the same build, but the install path and privilege model are different.

---

### 3) Which package do I need?

| Your case | File |
| --- | --- |
| Regular rooted user | `MaxManager-<version>.zip` (Magisk/KernelSU/APatch module) |
| ROM modder editing `product`/`system` images | `MaxManager-developer-bundle.zip` |
| AOSP source builder | See `README.txt` inside the developer bundle; this is not a build guide |

---

### 4) Verify after boot

```sh
adb shell ls -l /system/bin/sys.maxmanager-service
adb shell ls -l /product/priv-app/MaxManager/MaxManager.apk
adb shell ls -l /product/etc/permissions/privapp-permissions-nd.max.xml
adb shell pidof sys.maxmanager-service
adb shell getprop | grep maxmanager
adb shell dumpsys package nd.max | grep -E 'versionName|granted=true'
adb shell appops get nd.max SYSTEM_ALERT_WINDOW
```

The daemon starts at `sys.boot_completed=1`, so `pidof` may be empty for the first few seconds. If
`logcat` shows `avc: denied` for `maxmanager`, the SELinux policy was not merged.

---

### 5) Troubleshooting

| Symptom | Fix |
| --- | --- |
| Module installed but no app | Press **Action** on the module card, check `package-recovery.log`, reboot |
| Side panel never appears | Grant **Display over other apps** in Special access; Game Space shows the notice |
| Daemon absent after ROM integration | Verify `maxmanager.rc` and sepolicy, then `logcat \| grep avc` |
| Module prevents boot | Create `/data/adb/modules/MaxManager/disable` and reboot |
| App opens but controls do nothing | Grant root and confirm `pidof sys.maxmanager-service` |

---

**License / الرخصة:** MaxManager is proprietary. Copying these files into a ROM is permitted only
with the copyright holder's written permission; see [`LICENSE`](../LICENSE).

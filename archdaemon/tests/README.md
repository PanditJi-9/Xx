# archdaemon/tests — تكافؤ منطق الخادم على المضيف

**الغاية:** إثبات أن منطق قرار الخادم (`sys.maxmanager-service`) لم ينحرف، **بلا جهاز وبلا NDK**.
فالوحدة تُبنى بـ`clang` **للمضيف**، مع شيم صغير لـ`<sys/system_properties.h>` (ترويسة bionic)،
وتُشغَّل دعاوى قابلة للتكذيب على **ملفات الخادم الحقيقية نفسها** — لا نسخة منها.

```sh
make -C archdaemon/tests run        # يبني إن لزم، ثم يشغّل (exit != 0 عند أي فشل)
make -C archdaemon/tests self-check # طفرات يجب أن تُسقط الدعوى (وعلى مضيف يُعلن `policyN` تُعلَن طفرتا الحاكم غير مُتحقَّقتين)
make -C archdaemon/tests clean

# وعلى نظام ملفّات **بلا تنفيذ** (noexec) — كـ`/mnt/sdcard` في أجهزة Android، ولا يقبل رابطًا
# رمزيًّا كذلك — يُبنى ويُشغَّل خارج الشجرة، ومع مترجم المضيف لا مترجم NDK:
make -C archdaemon/tests CC=gcc BUILD_DIR=/tmp/mm-build run self-check
```

**والعداد مشترك واحد**: كان `g_checks_run`/`g_checks_failed` في `host.h` بـ`static`، فيحمل كل ملف
عدّاده الخاص — فأعلن `main()` «نجاحًا تامًّا» بينما مجموعة أحدث قد فشلت بالفعل. صار العدّ `extern`
ومعرَّفًا مرّة واحدة، فلا يمكن أن يُعلن النجاح فوق فشل.

وتُشغَّل هذه في CI داخل **«Contract gates»** (خطوة `Native parity harness`) قبل أي بناء ثقيل، فتسقط
انحدارات المنطق في ثوانٍ لا بعد دقائق.

## ما تقيسه الطبقة (وكيف)

| الوحدة | ما يُقاس | الدعوى الفاصلة |
| --- | --- | --- |
| `StartupInit/PropValidator.c` | قائمة الخصائص المسموحة وحذف المتقادم | مفتاح داخل نطاق التطبيق (`maxmanagerconf.*`) **لا يُحذف**؛ مفتاح مجهول تحت نطاقنا **يُحذف بالاسم**؛ ونطاق غريب لا يُلمس؛ وحدّ `MAX_PENDING_DELETE` يُعلن `STALE_PROP_SKIPPED`؛ **وسطح مُعلَن من ٩ مفاتيح (واحد عن كل صنف غطاء: بادئة جديدة، وبادئة قديمة، والقائمة الدقيقة، ونطاق التصحيح) لا يُحذف أيٌّ منها** — وهي الدعوى التي كشفت عطب `gpu_studio` وتُسقط عودته |
| `MaxManagerUtility/DaemonUtility.c` | `trim_newline` · `escape_shell_string` · `extract_string_value` | هرس `'` إلى `'\''` (يصدّ حقن الأوامر)؛ وقيمة بلا اقتباس ⇒ `default` لا قيمة مقتطعة |
| `SystemProfile/ProfileUtility.c` | بوّابتا الشاشة/الطاقة وحسم الخيارات لكل تطبيق | غياب الكاش ⇒ يقظ/بلا توفير؛ تطابق الحزمة ينسخ الخيارات؛ عدم التطابق ⇒ `NULL` لا نسخ جزئي |
| `BypassCharge/ChargingNodes.c` | ثوابت جدول عُقد التجاوز | العدد **مُثبَّت (٧٧)** · أسماء وفواصل فريدة · مسارات مطلقة · `on != off` لكل عقدة |
| `ConfigHandler/ResolutionChanger.c` | تفريع SDK وبناء الأمر | `default` ⇒ لا أمر؛ `sdk>=13` ⇒ `cmd game set --mode 2 --downscale …`؛ وإلا `device_config` + `cmd game mode 2`؛ والاسترجاع يتفرّع كذلك |
| `ConfigHandler/RenderingHandler.c` | قرار تبديل المرسّم | الحالي==الهدف ⇒ لا أمر؛ اختلاف ⇒ يطلب من الثنائية `setrender` ويحفظ السابق؛ والخادم **لا** يكتب `debug.hwui.renderer` بنفسه (تقسيم العمل مُقاس) |
| `AppLoader/StatusMonitor.c` | صيغة ملف `app_status` المقروء من الخادم | الملف الكامل ⇒ كل حقل بقيمته المُعلَنة؛ **وسطر `v 1` ومفتاح مجهول وسطر تالف لا تُفسد الحقول**؛ والغياب ⇒ الافتراضي المُعلن (`Unknown` · `-1`)؛ وغياب الملف ⇒ **لا تحديث** لا تصفير |
| `AppLoader/AppLoader.c` | صيغة `gamelist/maxmanagerApplist.json` — **مدخل كل ميزة لكل تطبيق** | الشكل المضغوط (`":{"`) الذي يكتبه التطبيق ⇒ كل حقل بقيمته و`get_gamestart` **تجد التطبيق**؛ و`" : {`، و`resolution_target` القديم يُقرأ، والبحث **لا يتجاوز كتلة التطبيق**؛ والصفر على ملفّ فيه مفاتيح يُعلَن `GAMELIST_LOAD_EMPTY` |
| `BinaryCLI/CLIUtility.c` | جدول قرار `--profile` / `--from-ai` | ١٣ حالة من `fixtures/contracts/cli_profile.tsv`: ن=٠ تُقبل بخروج ٠، والوضع اليدوي **يُرفض أثناء MAX AI** بخروج ١، و`--from-ai` يعبرها، والقيمة خارج ٠..٣ تُرفض |

**و`AppLoader.c` عطبُه مقيس لا موصوف:** قارئ الملفّ كان يطالب بـ`": {"` (بفراغ) والتطبيق يكتب
`":{"`، فسجلّ جهاز المستخدم يقول `EVENT=GAMELIST_LOADED count=0` وله ١٢ تطبيقًا — فلا يُطبَّق
بروفايل ولا DND ولا تردد. وطفرتان تُسقطان الدعوى إن عاد: مطابقة الفراغ، وإسقاط اسم المفتاح القديم.

### الطبقتان الجديدتان تقرآن **عُقَدًا مشتركة** لا نسخًا منها

`fixtures/contracts/` يُقاس من الطرفين: Kotlin يُنتج `app_status.valid.txt` بحرفيّته (اختبار
`nd.max.contract.AppStatusProtocolContractTest`)، وهذا الـharness يُمرّر الملف **نفسه** إلى
`read_app_status` الحقيقي. والربط لا يمسّ المصدر المُصنَّع حرفًا واحدًا، بطريقتين:

- **`-Wl,--wrap=fopen`** (`host/fopen_router.c`): مسار واحد يُمكِن توجيهه إلى fixture. وبلا تسجيل مسار
  يكون الالتفاف مرورًا عاديًّا، فلا يغيّر سلوك أي دعوى أخرى.
- **`-Wl,--wrap=run_profiler --wrap=notify`** (`host/stubs.c`): الدالتان معرّفتان أصلًا في وحدات
  مضمّنة في البناء، فالالتفاف يلتقط **النداءات** ويسجّلها دون أن يُعدّل التعريفات.

## حدّها — يُعلن ولا يُطوى

هذه الطبقة **لا تُثبت سلوك الجهاز**: لا تفتح عقدة sysfs، ولا تُشغّل root، ولا تُنتج ثنائية ARM.
وما يلي يبقى **يحتاج جهازًا** ولا تدّعيه هذه الطبقة:

- هل تُطبَّق فعليًا كتابة العُقد وتنجح على عتاد بعينه (`echo_to_file` · `read_current_ma`).
- سلوك `inotify` و`poll` وخيط قفل الـJava.
- تشغيل `cargo`/Rust و`ndk-build` وتوقيع الإصدار (`KS_PWD`).
- أي مسار يقرأ/يكتب مسارات `/data/adb/...` الحقيقية (`StateHandler` مثلًا) — فهو خارج نطاق
  المضيف عمدًا لأن الشجرة ليست جذرًا للكتابة.

وبناءً عليه: نجاح هذه الطبقة يعني **«المنطق سليم»**، ولا يعني **«يعمل على هاتفك»**.

## تشغيلها على مضيف **أندرويد** (Termux على الجهاز): `TZ=UTC` ليس تجميلًا

`make -C archdaemon/tests run` يمرّ كاملًا على مضيف Linux عادي — و**ينهار** على مضيف libcه bionic
بعد مجموعة `cli_profile` **بلا سطر FAIL واحد**. وهذا قِيس هنا لا ظُنّ:

```
$ ./build/parity_test            # بلا TZ، على أندرويد
  PASS run_profiler re-checks module identity, and the shipped module.prop passes it
  PASS the reference table opens (fixtures/contracts/cli_profile.tsv)
make: *** [Makefile:112: run] Segmentation fault
```

وبتتبّع `AddressSanitizer` (بناء `CC='clang -fsanitize=address -g'`، والقراءة بـ`ASAN_OPTIONS=log_path=…`
لأنّ `suite_cli_profile` تحوّل `stderr` إلى `/dev/null` أثناء نداء `handle_profile`):

```
#7 run_profiler  …/SystemProfile/ProfileUtility.c:88:21      (localtime)
#5 tzset_unlocked / #3 __bionic_get_system_tz  bionic.cpp
#2 __system_property_read_callback  …/tests/host/props.c:81:5
#1 strlcpy → #0 __sanitizer::internal_strnlen  (SEGV, READ at 0x726961432f6163)
```

**السبب:** `run_profiler` تُنادي `localtime`، و`localtime` في **bionic** يقرأ خاصية `persist.sys.timezone`
عبر `__system_property_read_callback` بتوقيع bionic الداخلي (`prop_info*` + callback + `void*`)،
وشيم المضيف يخدم التوقيع المبسّط الذي تستعمله دعاوى الخادم — فيُسلَّم إلى `strlcpy` مؤشّر ليس سلسلة.
وعلى مضيف **glibc** لا يُنادى الشيم أصلًا من `tzset`، فالمجموعة تمرّ هناك (وهو حال CI).

**والحلّ سطر واحد ولا يعدّل كودًا:** `TZ` من البيئة يجعل bionic يستعملها بدل الخاصية:

```sh
TZ=UTC make -C archdaemon/tests run self-check     # 240/240 والطفرات كلها
```

**وحدّه:** هذا تجاوز بيئي لا إصلاح للشيم؛ ومن أراد `make run` **عاريًا** على أندرويد فالإصلاح في
`host/props.c` أن يخدم توقيع bionic (`prop_info*`)، وهو خارج هذه الطبقة.

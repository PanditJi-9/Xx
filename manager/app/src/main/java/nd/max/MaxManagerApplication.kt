/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max

import nd.max.core.security.SecurityGate
import nd.max.core.access.RemoteAccess
import nd.max.AppMonitorLogger
import android.app.Application
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import nd.max.core.diagnostics.DiagnosticCenter
import nd.max.core.hardware.HardwareOverlayStore
import nd.max.core.hardware.HardwareRepairExecutor
import nd.max.core.hardware.ManualControlLocks
import nd.max.core.hardware.MemoryUfsHardwareBackend
import nd.max.core.daemon.ModuleWatchman
import nd.max.core.hardware.SharedControlStorage
import nd.max.core.hardware.SharedHardwareOwnershipStore
import nd.max.core.hardware.TouchControlBackend
import nd.max.core.maxai.MaxAiEngine
import nd.max.core.privilege.ShizukuGateway
import nd.max.core.gamespace.SidePanelArmedStore
import nd.max.service.GamePanelService
import nd.max.ui.util.GamePanelPrefs
import nd.max.ui.settings.AppLanguage

/**
 * نقطة إقلاع التطبيق: تُنشئ مكون Hilt وتُقلع بمحرك MAX AI.
 *
 * المحرك يبدأ مع العملية (مرة واحدة) حتى وهو مطفأ — فمحرك الأمان
 * يعمل دائمًا بصرف النظر عن حالة AI، ودورة الإيقاف لا تكتب شيئًا على
 * العتاد. تفعيل Max AI نفسه يظل قرار المستخدم وحده (افتراضيًا مطفأ).
 */
@HiltAndroidApp
class MaxManagerApplication : Application() {

    @Inject lateinit var maxAiEngine: MaxAiEngine
    @Inject lateinit var atlasRepairExecutor: HardwareRepairExecutor

    override fun onCreate() {
        super.onCreate()
        // اللمس يمرّ من منفّذ أطلس الوحيد (مُحكِّم Hilt) لا من إنشاء ثانٍ في `TouchControlBackend`.
        TouchControlBackend.installAtlasRepair(atlasRepairExecutor)
        // **بوّابة الإقلاع — أوّل سطر بعد `super` وقبل أي عمل.**
        //
        // ولماذا هنا تحديدًا لا في خيط خلفيّ: الحكم الذي يُبنى عليه الرفض يجب أن يكون
        // **جاهزًا قبل أن يبدأ أي شيء** — لوحة الألعاب، الرفيق الجذريّ، محرّك MAX AI.
        // ولو أُجّل إلى خيط خلفيّ لصار في نافذة زمنيّة تبدأ فيها خدمات وتُكتب عقد قبل أن
        // يُعرف أنّ النسخة معدَّلة — أي أنّ التأجيل يفتح الثغرة التي وُجد الحارس لمنعها.
        // والكلفة مقيسة النوع: بصمة موقّع من `PackageManager` + قراءة كتلة توقيع الحزمة
        // + بصمات الأصول، مرّة واحدة في عمر العملية (والباقي قياسات حيّة رخيصة).
        // وحدّه المعلن: هذا الزمن **لم يُقَس على جهاز** في هذه البيئة ولا يُدّعى رقمه.
        //
        // **تحديث (شاشة سوداء عند الإقلاع):** الفحص الثابت يقرأ الحزمة كلها ويحسب بصمات مدخلاتها،
        // وكان يجري هنا على الخيط الرئيسيّ فتبقى النافذة سوداء حتى ينتهي. والحكم **لا يمنع شيئًا**
        // (`SecurityGate.shouldRefuse` تُعيد false دائمًا؛ هو سجلّ تشخيص وحده) فلا فائدة من انتظار
        // الواجهة له. صار `warmUp` **يُجدول** الفحص على خيط مستقلّ منخفض الأولوية ويعود فورًا،
        // و`evaluate` تفعل الشيء نفسه؛ فلا ينتظر النشاط ولا الخدمة الفحصَ الثقيل.
        SecurityGate.warmUp(this)
        // لغة التطبيق أولًا: AppCompat يطبّق اللغة ويجدّد النشاط عند الحاجة، وهذا الوقت هو الوحيد
        // الخالي من أي نشاط. بدون هذا السطر كان اختيار المستخدم يُنسى بعد كل قتل للعملية (settings_prefs).
        AppLanguage.applySaved(this)
        // **واستوديو الجزيرة (نقل Islandify — MIT) يُهيّأ هنا — لا في شاشة.**
        //
        // `init` قراءة ملفّ تفضيلات صغير **مرّة واحدة في عمر العملية** وتهيئة تدفّقات الحالة:
        // بعدها كلُّ سطح — الشاشة والمستمع والخدمتان ونافذة التراكب — يقرأ الحكم نفسه ولا يبنيه
        // من جديد. ولو أُجّل إلى فتح الشاشة لرأى مستمعٌ بدأ قبلها إعداداتٍ لم تُقرأ بعد.
        nd.max.island.core.IslandSettings.init(this)
        // والمستودع المشترك في **التخزين المحميّ بالجهاز** — النوع نفسه الذي يختاره الرفيق الجذريّ،
        // فلو اختلفا لصار **مستودعان لا يرى أحدهما الآخر**. وهذا هو عطب «تعمل مرّة ولا تعمل أخرى»
        // بعينه: كان الرفيق يبني مستودعه في تخزينٍ لا يُفتح قبل قفل الشاشة فيموت عند الإقلاع.
        // التفصيل والسجلّ المقيس في [`SharedControlStorage`].
        val controlFilesDir = SharedControlStorage.filesDir(this)
        // وفشل التهيئة **لا يُسقط التطبيق عند الإقلاع**: يُعلن باسمه في تشخيص التطبيق (فيراه
        // المستخدم في شاشة التشخيص) ويمضي محدودًا — والسقوط كان يعطي «التطبيق يتوقّف» بدل سبب.
        runCatching {
            SharedHardwareOwnershipStore.configure(controlFilesDir, applicationInfo.uid, android.os.Process.myPid())
            // Same directory as the journal: the UI writes locks here, the companion
            // process reads them, and both must agree on one durable lock set.
            ManualControlLocks.configure(controlFilesDir)
        }.onFailure {
            DiagnosticCenter.record(
                component = "control-plane",
                message = "shared control directory unavailable: ${it.message}",
                throwable = it,
            )
        }
        // **ومجلّد `API` لرفيق الجذر يُنشأ من هنا — لا من الرفيق.**
        //
        // والسبب مقيس (حزمة سنابدراجون ٢٠٢٦-١٠-١٤، أول إقلاع بعد التثبيت): الرفيق يبدأ قبل
        // أن ينشئ مدير الحزم `/data/data/nd.max` أصلًا، وإنشاء `mkdirs()` منه مرفوض
        // SELinux (نطاق الجذر لا ينشئ `app_data_file`)، فيرمي `writeAppGameInfo` بـ`ENOENT`
        // (`GAME_INFO_WRITE_FAILED`) ويفشل `touch` في سكربت الخادم بالسبب نفسه. وعملية
        // التطبيق تملك حق الإنشاء في مجلّدها — فتُنشئه هنا مرة واحدة، ويبقى نداء الرفيق
        // أفضل جهد كما كان (يُعاد في كل تبديل فيُشفى تلقائيًا بعد أول فتح للتطبيق).
        // والمسار من `applicationInfo` لا حرفيٌّ: لا اسم حزمة مخمَّن.
        runCatching {
            java.io.File(applicationInfo.dataDir, "API").mkdirs()
        }.onFailure {
            DiagnosticCenter.record(
                component = "control-plane",
                message = "app API directory unavailable: ${it.message}",
                throwable = it,
            )
        }
        // **وصدفة الجذر تُسخَّن هنا لا في فتح شاشة.**
        //
        // libsu تُنشئ صدفتها **عند أوّل أمر** لا عند الإقلاع: فأوّل `Shell.cmd(...).exec()`
        // يدفع ثمن ولادة `su` وانتظار جاهزيتها (إلى مهلة المكتبة) — وإن وقع ذلك في مسار فتح
        // شاشة، **انتظر المستخدم شاشةً لا تُرسم**. والتهيئة المسبقة هنا **غير حاجبة**: الدالّة
        // بالاستدعاء الراجع تبني الصدفة على خيط داخليّ للمكتبة، والإقلاع لا ينتظرها — فتصادف
        // أوّلُ شاشةٍ تُفتح صدفةً جاهزةً بدل أن تدفع ثمنها.
        //
        // ولا يُغيَّر سلوك فاشل: إن رُفض الجذر أو غاب، يسقط الاستدعاء الراجع صامتًا ويبقى كل
        // مسار كما هو (كل نداء جذر بعدها يسأل الصدفة نفسها ويُعلن فشله في موضعه).
        Shell.getShell { }

        // **وسياق جسر Shizuku يُثبَّت هنا — لأن ربط خدمة المستخدم يحتاج `ComponentName` من اسم الحزمة.**
        // ويُثبَّت في الإقلاع لا عند أول نداء: أوّل تنفيذ قد يقع من لوحة الألعاب، وهي خدمة مستقلّة
        // عن أي نشاط يملك سياقًا. والتثبيت نفسه **لا يربط ولا يطلب إذنًا** — حفظ سياق التطبيق فقط
        // (والربط عند أول أمر فعليّ، بفعل المستخدم وحده).
        ShizukuGateway.install(this)

        // **وحارس الوحدة يُشغَّل هنا أيضًا — لا في مستقبِل البثّ وحده.**
        //
        // والمستقبِل يُغطّي ما يملكه النظام (قيام، فتح، تحديث). وهذا يُغطّي ما يملكه الإنسان:
        // «فتحُ التطبيق». وهو مهمّ لأنّ المستخدم غالبًا **يفتح التطبيق** حين يلاحظ أن الوحدة لا
        // تعمل — ولو لم يكن هنا بابٌ للشفاء، لما وجد شيئًا يُصلح ما رآه.
        //
        // والثمن معلن ومقبول: قياسٌ واحد بصدفة **بلا جذر** (`pidof`) في كل قيام عملية، ولا نداء
        // جذر إلا إذا نفى القياس حياة أحدهما حقًّا ([`ModuleWatchman`] يشرح الطريقين). والخيط
        // منفصل ومنتظر لا يشارك الإقلاع، فلا يُبطئ أوّل شاشة.
        if (isUserUnlocked()) {
            Thread { runCatching { ModuleWatchman().watch() } }
                .apply { isDaemon = true }
                .start()
        }
        // **ولوحة مساحة الألعاب تُشغَّل هنا إن كانت هناك لعبة مُفعَّلة واحدة على الأقل.** وهذه النقطة تُبلغ
        // في كل مرة تبدأ فيها عملية التطبيق: من واجهته، أو بعد إعادة تشغيل الجهاز أو تحديثه (`ModuleWatchReceiver`).
        // والخدمة لا تُوقف نفسها ما بقيت لعبة مُفعَّلة (`GamePanelService.ensureRunning` تشرح الشرط).
        if (isUserUnlocked()) {
            // ملف اللوحة الذي يقرؤه الرفيق الجذري نسخة من التفضيلات: تُزامَن هنا، في خيط مستقل.
            // **وبنتيجتها**، لا برميها: الفشل هنا يعني لوحةً لا تُشغَّل عند فتح اللعبة من مشغّل خارجي
            // والتطبيق مقتول — وكان الفشل يمرّ صامتًا بلا سطر (سابقة حزمة المستخدم ٢٠٢٦-١٠-١٥).
            Thread {
                runCatching {
                    val sync = SidePanelArmedStore.sync(GamePanelPrefs.load(this).enabledPackages)
                    if (!sync.ok) {
                        AppMonitorLogger.w("EVENT=SIDE_PANEL_ARM_SYNC_FAILED stage=boot ${sync.logDetail()}")
                    } else if (sync.wrote) {
                        // والكتابة وحدها تُسجَّل: «مُتزامن أصلًا» هو الحال المعتاد ولا يُنتج ضجيجًا.
                        AppMonitorLogger.i("EVENT=SIDE_PANEL_ARM_SYNCED stage=boot ${sync.logDetail()}")
                    }
                }.onFailure { AppMonitorLogger.w("EVENT=SIDE_PANEL_ARM_SYNC_FAILED stage=boot reason=arm-sync-threw", it) }
            }.apply { isDaemon = true }.start()
            runCatching { GamePanelService.ensureRunning(this) }
        }
        // **وتصنيف المستخدم (تعليم الأجهزة) يُشغَّل هنا — قبل أوّل قراءة قدرة.**
        //
        // ولماذا في الإقلاع لا في فتح شاشة الذاكرة وUFS وحدها: التصنيف يُقرأ في مواضع كثيرة
        // (خريطة قدرة أطلس، ومحرّك MAX AI، والتشخيص، والباقي)، ولو انتظر فتحَ شاشةٍ لرأى المستخدم
        // جهازًا مُعلَّمًا في شاشة وغير مُعلَّم في خريطة القدرة — وهو التفريق الذي يمنعه المستودع
        // في تعريف المقبض الواحد. والكلفة محدودة ومعلنة: قراءة ملفّ ≤ ٣٢ ك.ب مرّة واحدة في عمر
        // العملية، وفشلها لا يُسقط الإقلاع (يعود التصنيف في القراءة التي تلي فتح الشاشة).
        runCatching {
            MemoryUfsHardwareBackend.installOverlay(
                HardwareOverlayStore.inDirectory(filesDir).load().entries,
            )
        }
        // **وجسر اللمس يُركَّب هنا — لا في شاشة.**
        //
        // ولماذا في الإقلاع لا عند فتح شاشة اللمس: قارئ الجسر ثلاثة (شاشة اللمس، ولوحة الألعاب،
        // ودورة المراقبة في `AppMonitor`)، ومركَّبٌ في موضع واحد يضمن أن الثلاثة يرون الحكم نفسه —
        // ومنادٍ يُنسى فيرى «لا نقل» بينما الروم يعلن خدمته هو عطب يظهر للمستخدم لا للاختبار.
        // والجسر نفسه **لا يكتب شيئًا**: يعلن أن الخدمة معلنة، ويعلن أن طريق الكتابة غير مثبَت،
        // ويُعيد `null` (رفض بالعقد) إن طُلب منه تطبيق. فتركيبه لا يفتح مقبضًا، وإنما يمنع نصًّا
        // يقول «يُستعمل محوّل الروم» وهو لا يُستعمل.
        // والافتراضي في `TouchControlBackend` بلا جسر: ما لم يُركَّب لا يُدَّعى.
        runCatching { TouchControlBackend.vendorBridge = XiaomiVendorFeatures.TouchBridge }
            .onFailure {
                DiagnosticCenter.record(
                    component = "control-plane",
                    message = "touch vendor bridge not installed: ${it.message}",
                    throwable = it,
                )
            }
        maxAiEngine.start()
        // **وذاكرة الوصول (الإعداد الموقّع + الترخيص) تُقرأ هنا — بلا شبكة في المسار.**
        //
        // ولماذا في الإقلاع لا في فتح شاشة: كل الأسطح تقرأ حكمها من `RemoteAccess.state`، ولو
        // انتظر أوّلُ فتحٍ لشاشةٍ قراءةَ الذاكرة لرأى المستخدم لحظةً «مفتوحة» ثم تُقفل — وهي
        // أسوأ من قفلٍ صريح. والقراءة هنا **ذاكرة فقط** (ملفّان صغيران وحكمٌ نقيّ)، والتحديث
        // الوحيد من الشبكة يجري **خلفيًّا** بعدها فلا ينتظره الإقلاع ولا أوّل شاشة (§٩ من مواصفة
        // التحكّم). وفشلها لا يُسقط التطبيق: يبقى الحكم على الافتراضي المحلي كما في الحزمة.
        runCatching { RemoteAccess.boot(this) }.onFailure {
            DiagnosticCenter.record(
                component = "license",
                message = "access boot unavailable: ${it.message}",
                throwable = it,
            )
        }
    }

    /**
     * هل تخزين المستخدم مفتوح؟ — وهذا شرط عمل الحارس لا شرط عمله وحده: كل مساراته تقرأ إعدادات
     * التطبيق، فقبل الفتح لا يُبنى قرار على قراءة ناقصة. وتخزين المستودع نفسه صار محميًّا بالجهاز
     * ([`SharedControlStorage`]) فلا يعتمد على هذا الشرط — وهذا هو الفرق بين إصلاح الجذر وحيطة المسار.
     */
    private fun isUserUnlocked(): Boolean = runCatching {
        getSystemService(android.os.UserManager::class.java)?.isUserUnlocked != false
    }.getOrDefault(false)
}

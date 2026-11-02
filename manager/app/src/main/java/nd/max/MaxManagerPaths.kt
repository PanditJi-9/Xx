/*
 * Copyright (C) 2026 Zexshia
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

import java.io.File

/**
 * Shared on-device path constants.
 *
 * Before this file existed, "/data/adb/.config/MaxManager/gamelist/maxmanagerApplist.json"
 * was hardcoded independently in TweakViewmodel.kt, ApplistViewmodel.kt,
 * AppSettingsViewmodel.kt, and (as of the Xiaomi-extras change) AppMonitor.kt.
 * Centralising it here means a future path change only needs one edit
 * instead of four, and new code has one obvious place to reuse instead of
 * copy-pasting the string again — see PROJECT_NOTES.md rule #3
 * ("متكررش منطق موجود").
 *
 * SERVICE_BIN was likewise hardcoded on its own in AppMonitor.kt's
 * restoreGlobalMaxManagerProfile() ("/data/adb/modules/MaxManager/system/bin/
 * sys.maxmanager-service"). Centralised here for the same reason, and now
 * also reused by AppMonitorLogger to forward log lines into the daemon's
 * shared log via its `--log` CLI hook. Module id confirmed from
 * mainfiles/module.prop (id=MaxManager).
 */
object MaxManagerPaths {
    const val MODULE_CONFIG = "/data/adb/.config/MaxManager"

    /**
     * نيّة المستخدم الواحدة: 1 أداء · 2 متوازن · 3 بطارية.
     * يقرؤها الخادم عند الإقلاع وعند تغييرها (`System.c` / `InotifyWatcher.c`)،
     * ويكتبها التطبيق من مبدّل Max AI. و`API/current_profile` يبقى الوضع الفاعل
     * المؤقّت الذي يكتبه الخادم (لعب/بطارية منخفضة) ويعود منه إلى هذا الملفّ.
     */
    const val MAX_AI_MODE = "$MODULE_CONFIG/API/max_ai_mode"
    const val MODULE_DIR = "/data/adb/modules/MaxManager"
    const val APPLIST_JSON = "$MODULE_CONFIG/gamelist/maxmanagerApplist.json"
    /** قائمة اللوحة الجانبية المُفعَّلة، نسخة يقرؤها الرفيق الجذري (انظر `SidePanelArmed`). */
    const val SIDE_PANEL_ARMED = "$MODULE_CONFIG/gamelist/side_panel_armed.txt"
    const val SERVICE_BIN = "$MODULE_DIR/system/bin/sys.maxmanager-service"

    /**
     * **وضع الروم (ROM install) — عطب مقيس من جهاز حقيقي، ٢٠٢٦-١١-٠١.**
     *
     * حين يُدمج MaxManager في صورة الروم (حزمة المطوّرين) يقع الديمون في `/system/bin` والـAPK في
     * `/product/priv-app/MaxManager` — **ولا يوجد** `/data/adb/modules/MaxManager` أصلًا. وكان كل
     * مستهلك في التطبيق يقرأ [SERVICE_BIN] (مسار الموديول) فيجد «service binary missing» ويبقى
     * الرئيسي على `IDLE` مهما مُنح الجذر. فالمسار صار **يُحلّ** ([serviceBin]) لا يُفترض.
     *
     * والقائمة مرتّبة بنفس أولويّة الأقسام التي تنشر إليها حزمة المطوّرين (`system` أولًا).
     */
    private const val DAEMON_NAME = "sys.maxmanager-service"
    val ROM_SERVICE_BINS: List<String> = listOf(
        "/system/bin/$DAEMON_NAME",
        "/system_ext/bin/$DAEMON_NAME",
        "/product/bin/$DAEMON_NAME",
        "/vendor/bin/$DAEMON_NAME",
    )

    /** مواضع الـAPK المميَّز في الروم — بنفس مسار حزمة المطوّرين `product/priv-app/MaxManager`. */
    val ROM_APK_PATHS: List<String> = listOf(
        "/product/priv-app/MaxManager/MaxManager.apk",
        "/system/priv-app/MaxManager/MaxManager.apk",
        "/system_ext/priv-app/MaxManager/MaxManager.apk",
        "/product/app/MaxManager/MaxManager.apk",
        "/system/app/MaxManager/MaxManager.apk",
    )

    /**
     * ثنائيّ الديمون المدموج في الروم، أو `null`. ومواضع الأقسام مقروءة لعملية التطبيق (بخلاف
     * `/data/adb` المحجوب عنها)، فالقياس صالح بلا جذر. والـ`exists` مُمرَّر ليُقاس على المضيف.
     */
    fun romServiceBin(exists: (String) -> Boolean = { File(it).exists() }): String? =
        ROM_SERVICE_BINS.firstOrNull(exists)

    /** هل التثبيت مدمج في الروم؟ (ديمون في قسم نظام لا في الموديول). */
    fun isRomInstall(exists: (String) -> Boolean = { File(it).exists() }): Boolean =
        romServiceBin(exists) != null

    /**
     * الثنائيّ الذي يجب أن يُشغَّل ويُنادى: ثنائيّ الروم إن وُجد، وإلا ثنائيّ الموديول ([SERVICE_BIN]).
     * وعلى المضيف (اختبارات الوحدة) لا يوجد ثنائيّ روم فيعود [SERVICE_BIN] كما كان.
     */
    fun serviceBin(exists: (String) -> Boolean = { File(it).exists() }): String =
        romServiceBin(exists) ?: SERVICE_BIN

    /**
     * الـAPK الذي يُشغَّل منه رفيق الجذر (`app_process -Djava.class.path=…`): نسخة الموديول [MODULE_APK]
     * أولًا إن كانت مقروءة، ثم أول APK روم موجود، وإلا يعود [MODULE_APK] (السلوك السابق بلا تغيير).
     */
    fun companionApk(exists: (String) -> Boolean = { File(it).exists() }): String =
        if (exists(MODULE_APK)) MODULE_APK else ROM_APK_PATHS.firstOrNull(exists) ?: MODULE_APK

    /**
     * `module.prop` المشحون — يقرؤه حارس هويّة الخادم (`is_kanged` في `ModuleIntegrity.c`)
     * ويقرؤه المشرف من الطرف نفسه ليقول أيّهما العطب: الملفّ أم لحظة كتابته.
     */
    const val MODULE_PROP = "$MODULE_DIR/module.prop"
    // The Manager APK shipped in the module: a normal user app outside system/ (installed by service.sh).
    const val MODULE_APK = "$MODULE_DIR/app/MaxManager.apk"
    const val MAXMANAGER_LOG = "$MODULE_CONFIG/debug/MaxManager.log"
    const val PER_APP_CPU_STATUS = "$MODULE_CONFIG/runtime/per_app_cpu_status"

    /**
     * نتيجة **كل مقبض** per-app (CPU · GPU · الحكام · الحرارة) مع رمز سببه —
     * يُكتب من مراقب الخلفية وتقرؤه الواجهة والتشخيص. التفصيل في
     * [nd.max.core.hardware.PerAppHardwareStatus].
     */
    const val PER_APP_HW_STATUS = "$MODULE_CONFIG/runtime/per_app_hw_status"

    /**
     * لقطة اكتشاف GPU — يكتبها الرفيق الجذري (يرى العقد كلها) وتتبنّاها عملية التطبيق
     * (قد تُحجب عنها) عبر أطلس. التفصيل في [nd.max.core.hardware.GpuDiscoverySnapshot].
     */
    const val GPU_DISCOVERY_SNAPSHOT = "$MODULE_CONFIG/runtime/gpu_discovery"
}

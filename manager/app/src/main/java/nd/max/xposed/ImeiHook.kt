/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import nd.max.core.spoof.ImeiSpoof

/**
 * خطّاف LSPosed الذي يجعل التطبيق يرى IMEI مزيّفًا. يُحمَّل داخل عملية كلّ تطبيق **اختاره المستخدم في نطاق
 * الوحدة** من LSPosed، ويعترض `TelephonyManager` هناك.
 *
 * ### ما يفعله
 * - يُرجع القيمة المحفوظة لهذا التطبيق (انظر [nd.max.core.spoof.ImeiSpoofStore]) من `getImei()` و`getImei(slot)`
 *   و`getDeviceId()` و`getDeviceId(slot)`، **قبل** تنفيذ الدالّة الأصلية؛ فلا يصل النداء إلى النظام ولا يُرفض بإذن.
 * - لا يفعل شيئًا لتطبيق ليس له قيمة محفوظة: القراءة تتمّ عند كلّ نداء، فتفعيل التزييف أو إطفاؤه من التطبيق
 *   يسري بلا إعادة تشغيل الجهاز (يُعاد تشغيل التطبيق الهدف فقط إن خزّن القيمة مسبقًا).
 *
 * ### حدّه المُعلَن
 * يغطّي ما يمرّ بـ`TelephonyManager` في عملية التطبيق نفسها. تطبيقٌ يقرأ IMEI من مكتبة أصلية (JNI) أو بجذر أو
 * من عملية أخرى لا يمرّ من هنا.
 *
 * لا يلمس هذا الصنفُ أيّ معرّف حقيقي، ولا يرمي: كلّ فشل يُسجَّل في سجلّ LSPosed ويُترك النداء الأصلي يعمل.
 */
class ImeiHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val target = lpparam.packageName ?: return
        // لا نلمس النظام ولا تطبيقنا نفسه.
        if (target == "android" || target == MODULE_PACKAGE) return

        val prefs = runCatching { XSharedPreferences(MODULE_PACKAGE, ImeiSpoof.PREFS_NAME) }.getOrNull() ?: return
        val key = ImeiSpoof.key(target)
        val current: () -> String? = {
            runCatching {
                if (prefs.hasFileChanged()) prefs.reload()
                prefs.getString(key, null)?.takeIf(ImeiSpoof::isValid)
            }.getOrNull()
        }

        hook(lpparam.classLoader, "getImei", withSlot = false, current = current)
        hook(lpparam.classLoader, "getImei", withSlot = true, current = current)
        hook(lpparam.classLoader, "getDeviceId", withSlot = false, current = current)
        hook(lpparam.classLoader, "getDeviceId", withSlot = true, current = current)
    }

    private fun hook(classLoader: ClassLoader, method: String, withSlot: Boolean, current: () -> String?) {
        val callback = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val imei = current() ?: return
                val slot = if (withSlot) (param.args.firstOrNull() as? Int) ?: 0 else 0
                param.result = ImeiSpoof.forSlot(imei, slot)
            }
        }
        try {
            if (withSlot) {
                XposedHelpers.findAndHookMethod(TELEPHONY_MANAGER, classLoader, method, Int::class.javaPrimitiveType, callback)
            } else {
                XposedHelpers.findAndHookMethod(TELEPHONY_MANAGER, classLoader, method, callback)
            }
        } catch (error: Throwable) {
            // دالّة غير موجودة في هذا الإصدار من أندرويد (getImei منذ API 26) ليست خطأً يوقف التطبيق.
            XposedBridge.log("MaxManager IMEI hook skipped $method(slot=$withSlot): ${error.message}")
        }
    }

    private companion object {
        const val MODULE_PACKAGE = "nd.max"
        const val TELEPHONY_MANAGER = "android.telephony.TelephonyManager"
    }
}

/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.platform

import android.content.Context
import android.provider.Settings

/**
 * كتابة/قراءة إعدادات أندرويد **بواجهة المنصّة أوّلًا، وصدفة الجذر احتياطًا**.
 *
 * **لماذا (المرحلة الخامسة من مسار «بلا جذر»):** `settings put …` كانت صدفةَ جذر في كل مكان، والإعداد نفسه تكتبه
 * واجهة `Settings` بلا جذر **متى حمل التطبيق `WRITE_SECURE_SETTINGS`** — وهو ممنوح في الروم أصلًا بقائمة
 * `privapp-permissions-nd.max.xml` (وهو يغطي الجداول الثلاثة: عند حامله لا يُطلب `WRITE_SETTINGS`). فصار التطبيق
 * المميَّز في الروم يغيّر العرض وموفّر البطارية بلا جذر ولا شيزوكو.
 *
 * **وحدّها معلن:** أيّ رفض (`SecurityException` لتطبيق غير مميَّز مثلًا، أو `false` من المزوّد) يُكمل بالصدفة كما
 * كانت حرفيًّا — فالسلوك حيث لا تُقبل الواجهة **محفوظ**، لا مُفترَض. ولم يُنقل `settings delete` (لا نظير له في
 * `Settings` يُفرِّق بين الحذف وكتابة `null`).
 */
object SettingsAccess {
    @Volatile private var appContext: Context? = null

    /** يُنادى مرّة من `MaxManagerApplication.onCreate`. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** `true` فقط إن كتبتها واجهة المنصّة نفسها (بلا صدفة). */
    fun putDirect(namespace: String, key: String, value: String): Boolean {
        val resolver = appContext?.contentResolver ?: return false
        return runCatching {
            when (namespace) {
                "global" -> Settings.Global.putString(resolver, key, value)
                "system" -> Settings.System.putString(resolver, key, value)
                "secure" -> Settings.Secure.putString(resolver, key, value)
                else -> false
            }
        }.getOrDefault(false)
    }

    /** واجهة المنصّة، ثم `settings put` بالجذر إن رُفضت. ويعيد هل نجحت إحداهما. */
    fun put(namespace: String, key: String, value: String): Boolean {
        if (putDirect(namespace, key, value)) return true
        return runCatching { nd.max.core.privilege.RootCommands.cmd("settings put $namespace $key $value").exec().isSuccess }.getOrDefault(false)
    }

    /** القيمة من واجهة المنصّة، أو `null` إن لم تُضبط/رُفضت (فيسأل المستدعي الصدفة كما كان). */
    fun getDirect(namespace: String, key: String): String? {
        val resolver = appContext?.contentResolver ?: return null
        return runCatching {
            when (namespace) {
                "global" -> Settings.Global.getString(resolver, key)
                "system" -> Settings.System.getString(resolver, key)
                "secure" -> Settings.Secure.getString(resolver, key)
                else -> null
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() && it != "null" }
    }
}

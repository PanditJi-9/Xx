/*
 * Copyright (C) 2026-2027 MaxManager contributors
 *
 * Licensed under the Apache License, Version 2.0.
 */
package nd.max.core.platform

/**
 * رموز شريط الحالة التي يقبلها `Settings.Secure.icon_blacklist`.
 *
 * **والقائمة مقيسة لا مُخمَّنة:** أسماء الرموز هذه هي ما كتبه ZenBubble على أجهزة حقيقية
 * (`settings put secure icon_blacklist 'wifi,cell,battery,clock,…'`)، وهي أسماء AOSP نفسها التي
 * يقرأها `SystemUI`؛ ومن كتب رمزًا غير معروف لا يخسر شيئًا لأن [IconBlacklist] يحفظه كما هو.
 */
enum class StatusBarIcon(val token: String) {
    WIFI("wifi"),
    CELL("cell"),
    BATTERY("battery"),
    CLOCK("clock"),
    ALARM("alarm_clock"),
    BLUETOOTH("bluetooth"),
    VOLUME("volume"),
    MUTE("mute"),
    VIBRATE("vibrate"),
    ZEN("zen"),
    DND("dnd"),
    LOCATION("location"),
    GPS("gps"),
    CAST("cast"),
    HOTSPOT("hotspot"),
    VPN("vpn"),
    AIRPLANE("airplane"),
    ;

    companion object {
        /** `null` لرمز غير معروف — والنداء لا يرمي. */
        fun of(token: String): StatusBarIcon? =
            entries.firstOrNull { it.token == token.trim().lowercase() }
    }
}

/**
 * محتوى `icon_blacklist` بعد الفكّ.
 *
 * **ولماذا يُحفظ المجهول:** الرومات المخصّصة تضيف رموزًا (`nfc`، `headset`، `rotate`، …) لا يعرفها
 * تعدادنا. لو كتبنا القائمة من جديد بالمعروف وحده، لَحُذفت إخفاءات المستخدم التي ضبطها بنفسه من
 * إعدادات النظام. فالمجهول **يُعاد كما جاء**، والمعروف وحده هو ما نُبدّله.
 *
 * @property known المخفيّ من الرموز التي نعرفها.
 * @property unknown رموز ظهرت في الإعداد ولم نعرفها — تُحفظ بترتيبها الأصليّ.
 */
data class IconBlacklist(
    val known: Set<StatusBarIcon> = emptySet(),
    val unknown: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = known.isEmpty() && unknown.isEmpty()

    fun hides(icon: StatusBarIcon): Boolean = icon in known
}

/**
 * تحكّم شريط الحالة بلا جذر — إخفاء الرموز، شارة الإشعارات، وأيقونة الإشعار، والوضع الغامر.
 *
 * **الآلية (لا الكود) من ZenBubble:** التطبيق هناك لا يرسم شريط حالة بديلًا فقط، بل يخفي رموز
 * النظام عبر `icon_blacklist` و`policy_control` ليبقى شريطًا واحدًا نظيفًا. والمفاتيح هنا هي مفاتيح
 * AOSP المقيسة، والكتابة تمرّ بـ[SettingsAccess] (واجهة المنصّة أوّلًا ثم الصدفة).
 *
 * **والقراءة والكتابة تُحقنان** ([read]/[write]) لتُقاس الدالّة بلا أندرويد: كل منطق القائمة نقيّ،
 * والوصول إلى النظام طبقة رقيقة حوله.
 */
class StatusBarIcons(
    private val read: (namespace: String, key: String) -> String?,
    private val write: (namespace: String, key: String, value: String) -> Boolean,
) {

    /** الحالة الحالية كما يراها النظام. */
    fun current(): IconBlacklist = parse(read(SECURE, KEY_BLACKLIST))

    fun isHidden(icon: StatusBarIcon): Boolean = current().hides(icon)

    /**
     * يخفي [icons] **دون أن يمسّ** ما كان مخفيًّا ولا المجهول.
     *
     * @return `true` إن قَبِل النظام الكتابة.
     */
    fun hide(icons: Set<StatusBarIcon>): Boolean {
        val before = current()
        return commit(before.copy(known = before.known + icons))
    }

    fun show(icons: Set<StatusBarIcon>): Boolean {
        val before = current()
        return commit(before.copy(known = before.known - icons))
    }

    /** يقلب الحالة — لما يفعله البلاط بضغطة واحدة. */
    fun toggle(icon: StatusBarIcon): Boolean =
        if (isHidden(icon)) show(setOf(icon)) else hide(setOf(icon))

    /**
     * إخفاء كل الرموز المعروفة (أو إظهارها) مع **الحفاظ على المجهول**.
     *
     * هذا هو «الشريط النظيف»: شاشة بلا ساعة ولا بطارية ولا شبكة، والإشعارات تبقى تعمل لأن
     * `icon_blacklist` يخفي الرسم لا الخدمة.
     */
    fun setAllHidden(hidden: Boolean): Boolean {
        val before = current()
        return commit(
            before.copy(known = if (hidden) StatusBarIcon.entries.toSet() else emptySet()),
        )
    }

    /** كل الرموز مخفيّة الآن؟ (يُسأل عنه البلاط ليرسم حالته.) */
    fun allHidden(): Boolean = StatusBarIcon.entries.all { it in current().known }

    /** `policy_control` — الوضع الغامر الكامل، مفتاح ZenBubble الآخر. */
    fun immersive(enabled: Boolean): Boolean =
        write(GLOBAL, KEY_POLICY_CONTROL, if (enabled) IMMERSIVE_FULL else "")

    /** نقطة الإشعار على الأيقونة (`notification_badging`). */
    fun notificationBadging(enabled: Boolean): Boolean =
        write(SECURE, KEY_BADGING, if (enabled) "1" else "0")

    /** أيقونة الإشعار داخل شريط الحالة (`show_notification_icon`). */
    fun notificationIcon(enabled: Boolean): Boolean =
        write(SECURE, KEY_NOTIFICATION_ICON, if (enabled) "1" else "0")

    private fun commit(state: IconBlacklist): Boolean =
        write(SECURE, KEY_BLACKLIST, encode(state))

    companion object {
        const val SECURE = "secure"
        const val GLOBAL = "global"
        const val KEY_BLACKLIST = "icon_blacklist"
        const val KEY_BADGING = "notification_badging"
        const val KEY_NOTIFICATION_ICON = "show_notification_icon"
        const val KEY_POLICY_CONTROL = "policy_control"

        /** القيمة التي يكتبها ZenBubble؛ و`immersive.status=*` يُبقي شريط التنقّل. */
        const val IMMERSIVE_FULL = "immersive.full=*"

        /** النسخة الحقيقية: قراءة/كتابة عبر [SettingsAccess] ثم الصدفة. */
        fun system(): StatusBarIcons = StatusBarIcons(
            read = { namespace, key -> SettingsAccess.getDirect(namespace, key) ?: readShell(namespace, key) },
            write = { namespace, key, value -> SettingsAccess.put(namespace, key, value) },
        )

        private fun readShell(namespace: String, key: String): String? = runCatching {
            val out = com.topjohnwu.superuser.Shell.cmd("settings get $namespace $key").exec().out
            out.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
        }.getOrNull()

        /**
         * يفكّ نصّ `icon_blacklist`. **لا يرمي على أي مدخل** — نصّ فارغ، `null`، فواصل مكرّرة،
         * أو مسافات زائدة كلها تُنتج حالة صالحة.
         */
        fun parse(raw: String?): IconBlacklist {
            if (raw.isNullOrBlank()) return IconBlacklist()
            val seen = LinkedHashSet<String>()
            for (part in raw.split(',')) {
                val token = part.trim().lowercase()
                if (token.isNotEmpty()) seen.add(token)
            }
            val known = LinkedHashSet<StatusBarIcon>()
            val unknown = ArrayList<String>()
            for (token in seen) {
                val icon = StatusBarIcon.of(token)
                if (icon != null) known.add(icon) else unknown.add(token)
            }
            return IconBlacklist(known = known, unknown = unknown)
        }

        /** يعيد بناء النصّ: المعروف بترتيب التعداد، ثم المجهول بترتيبه الأصلي. */
        fun encode(state: IconBlacklist): String {
            val tokens = ArrayList<String>(state.known.size + state.unknown.size)
            for (icon in StatusBarIcon.entries) if (icon in state.known) tokens.add(icon.token)
            tokens.addAll(state.unknown.filter { StatusBarIcon.of(it) == null })
            return tokens.joinToString(",")
        }
    }
}

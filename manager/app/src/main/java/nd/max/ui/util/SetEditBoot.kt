/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.util

import android.content.Context

/**
 * «هل تعيش هذه القيمة بعد إعادة التشغيل؟» — سؤال يُجاب من **المفتاح** لا من نجاح الأمر.
 *
 * ### السبب الجذريّ لعطب «تعديلات المحرّر لا تبقى بعد الإقلاع»
 *
 * خدمة خصائص أندرويد تحفظ `persist.*` وحدها في `/data/property/persistent_properties`؛ وما عداها
 * (‏`debug.*` · `vendor.*` · `sys.*` …) يعيش في الذاكرة وحدها. وشاشة المحرّر كانت تكتب كل خصيصة
 * بـ`setprop` ثم تعدّ رمز خروج صفر نجاحًا — فلا شيء يعيد القيمة بعد الإقلاع، ولا سطر يقول ذلك.
 * وخصائص `ro.*` أشدّ: يثبّتها `init` قبل بدء الخدمات، فمحاولة تغييرها أثناء التشغيل لا تغيّر شيئًا
 * أصلًا، ومع ذلك كانت تُعرض كصفوف قابلة للتحرير.
 *
 * ### والقاعدة هنا
 *
 * - `settings.*` و`persist.*` ⇒ [SetEditPersistence.PERSISTENT]: النظام نفسه يحفظها؛ لا إعادة كتابة.
 * - خصيصة عاديّة ⇒ [SetEditPersistence.UNTIL_REBOOT]: تُكتب الآن، وتُسجَّل في [SetEditBootStore]
 *   ليُعيدها `PersistRestorer` بعد الإقلاع. وإعادة الكتابة عند الإقلاع **لا تصادم مالكًا حيًّا**:
 *   الخصائص لا يغيّرها النظام بعد الإقلاع، بخلاف مفاتيح `settings` التي تملكها خدمات حيّة.
 * - `ro.*` ⇒ [SetEditPersistence.READ_ONLY]: لا تُكتب، وتُشرح الحقيقة بدل نجاح وهميّ.
 * - `ctl.*` ⇒ [SetEditPersistence.CONTROL]: رسالة تحكّم (`ctl.start`/`ctl.stop`) ذات أثر جانبيّ،
 *   لا حالة تُقرأ؛ تُرسل الآن ولا تُعاد عند الإقلاع (إعادتها قد تشغّل خدمة أو توقفها بلا طلب).
 */
enum class SetEditPersistence {
    /** يحفظه النظام: `settings` أو خصيصة `persist.*`. */
    PERSISTENT,

    /** خصيصة عاديّة: تبقى حتى الإقلاع، ويُعيدها التطبيق بعده إن كان مفتاح الحفظ مفعّلًا. */
    UNTIL_REBOOT,

    /** `ro.*`: يثبّتها `init` ولا تتغيّر أثناء التشغيل. */
    READ_ONLY,

    /** `ctl.*`: رسالة تحكّم بأثر جانبيّ، لا حالة تُقرأ ولا تُعاد. */
    CONTROL,
}

/** تصنيف المفتاح — نقيّ فيُختبر بلا جهاز. */
fun setEditPersistenceOf(category: SetEditCategory, key: String): SetEditPersistence {
    if (category != SetEditCategory.ANDROID_PROP) return SetEditPersistence.PERSISTENT
    val name = key.trim()
    return when {
        name.startsWith("ro.") -> SetEditPersistence.READ_ONLY
        name.startsWith("init.svc.") -> SetEditPersistence.READ_ONLY
        name.startsWith("persist.") -> SetEditPersistence.PERSISTENT
        name.startsWith("ctl.") -> SetEditPersistence.CONTROL
        else -> SetEditPersistence.UNTIL_REBOOT
    }
}

/**
 * أسماء المفاتيح المقبولة — **حرفيّتها إغلاقُ حقن**، لا تنسيق.
 *
 * الأمر يُبنى للجذر، والمفتاح يُلصق في سطر الصدفة بلا اقتباس (خلافه خصيصة لا تُقرأ). فمفتاح فيه
 * `;` أو فراغ أو `$` من ورقة «إضافة» كان يُنفّذ ما بعده بأمر جذريّ. والقاعدة هنا تعكس محارف خدمة
 * الخصائص نفسها (`A-Z a-z 0-9 _ . -`)؛ وما خرج عنها يُرفض قبل أن يُبنى أي سطر.
 */
private val SET_EDIT_KEY = Regex("[A-Za-z0-9_.-]{1,96}")

fun isValidSetEditKey(category: SetEditCategory, key: String): Boolean {
    // القاعدة واحدة للصنفين؛ والوسيط يُبقي النداء صريحًا ويمنح تصنيفًا مستقبلًا بلا تغيير نداء.
    if (category == SetEditCategory.ANDROID_PROP && key.startsWith("-")) return false
    return SET_EDIT_KEY.matches(key)
}

/** خصيصة واحدة محفوظة للإعادة بعد الإقلاع. */
data class SetEditBootOverride(val key: String, val value: String)

/**
 * ترميز الخطة في نصّ واحد — بلا JSON ولا `android.util`, فكلا الحالتين لا تُختبران على المضيف.
 *
 * الصيغة: سطر لكل خصيصة، `key<TAB>value`، والمحارف الأربعة (`\` · TAB · LF · CR) مُهرَّبة.
 */
internal object SetEditBootCodec {
    const val MAX_ENTRIES = 64
    const val MAX_VALUE_LENGTH = 2048

    fun encode(entries: List<SetEditBootOverride>): String =
        entries.takeLast(MAX_ENTRIES).joinToString("\n") { "${escape(it.key)}\t${escape(it.value)}" }

    /**
     * ويُرشَّح عند القراءة لا عند الكتابة وحدها: خطة قديمة أو تالفة قد تحمل `ro.*` أو `ctl.*`،
     * وإعادة تطبيقها بعد الإقلاع أخطر من إسقاطها. والتصفية هنا تجعل الترميز نفسه عقدًا.
     */
    fun decode(raw: String?): List<SetEditBootOverride> {
        if (raw.isNullOrEmpty()) return emptyList()
        val unique = LinkedHashMap<String, SetEditBootOverride>()
        for (line in raw.split('\n')) {
            if (line.isEmpty() || unique.size >= MAX_ENTRIES) continue
            val tab = line.indexOf('\t')
            if (tab <= 0) continue
            val key = unescape(line.substring(0, tab))
            val value = unescape(line.substring(tab + 1))
            if (!isValidSetEditKey(SetEditCategory.ANDROID_PROP, key)) continue
            if (setEditPersistenceOf(SetEditCategory.ANDROID_PROP, key) != SetEditPersistence.UNTIL_REBOOT) continue
            if (value.length > MAX_VALUE_LENGTH) continue
            unique[key] = SetEditBootOverride(key, value)
        }
        return unique.values.toList()
    }

    private fun escape(value: String): String = buildString(value.length + 8) {
        for (c in value) {
            when (c) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }

    private fun unescape(value: String): String = buildString(value.length) {
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    '\\' -> append('\\')
                    else -> append(value[i + 1])
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }
}

/**
 * خطة الإعادة بعد الإقلاع — في تخزين التطبيق الخاصّ، لا في خصائص النظام.
 *
 * والحدّ الأعلى مقصود: هذه خطة «آخر تعديلات المستخدم»، لا سجلًّا؛ والقائمة الطويلة تُبطئ الإقلاع
 * وتزيد سطح الخطر. والأحدث يفوز، والأقدم يُسقَط عند بلوغ السقف.
 */
object SetEditBootStore {
    private const val PREFS = "maxmanager_setedit_boot"
    private const val KEY_PLAN = "plan"

    fun load(context: Context): List<SetEditBootOverride> =
        SetEditBootCodec.decode(prefs(context).getString(KEY_PLAN, null))

    /** يسجّل خصيصة عاديّة بعد كتابتها بنجاح فقط — ولا يسجّل `ro.*`/`persist.*`/`ctl.*`. */
    fun record(context: Context, key: String, value: String) {
        if (setEditPersistenceOf(SetEditCategory.ANDROID_PROP, key) != SetEditPersistence.UNTIL_REBOOT) return
        if (!isValidSetEditKey(SetEditCategory.ANDROID_PROP, key)) return
        if (value.length > SetEditBootCodec.MAX_VALUE_LENGTH) return
        val next = load(context).filterNot { it.key == key } + SetEditBootOverride(key, value)
        save(context, next)
    }

    /** يحذف خطة الإعادة لخصيصة — ويعيد هل كانت مسجّلة أصلًا (فالرسالة تفرق بين الحالتين). */
    fun forget(context: Context, key: String): Boolean {
        val current = load(context)
        val next = current.filterNot { it.key == key }
        if (next.size == current.size) return false
        save(context, next)
        return true
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_PLAN).apply()
    }

    private fun save(context: Context, entries: List<SetEditBootOverride>) {
        val capped = entries.takeLast(SetEditBootCodec.MAX_ENTRIES)
        prefs(context).edit().putString(KEY_PLAN, SetEditBootCodec.encode(capped)).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** حكم الكتابة: طابقت قراءةٌ القيمةَ المطلوبة، لا «خرج الأمر بصفر». */
internal object SetEditWriteRules {
    fun honoured(desired: String, observed: String?): Boolean = observed?.trim() == desired.trim()
}

/**
 * اقتباس صدفة أحاديّ: `$` و`` ` `` و`\` تبقى حرفيّة داخل `'…'`.
 *
 * والاقتباس المزدوج السابق كان يهرّب `"` وحدها، فقيمة فيها `$(…)` تُنفَّذ بصلاحية الجذر عند
 * `settings put`/`setprop`. والاقتباس الأحاديّ يغلق ذلك بلا فقدان قيمة.
 */
internal fun setEditShellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

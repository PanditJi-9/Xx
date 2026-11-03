/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.emuhub

/**
 * وصفة تشغيل — **الملفّ الذي يجعل «أيّ محاكٍ» ممكنًا بلا كتابة مُهايئ لكل واحد**.
 *
 * المبدأ: **أمر التشغيل حقلُ بيانات لا كود**. قالبٌ لكل حزمة، يُحلّ إلى قيم صريحة، والمستخدم يرى
 * ما سيُنفَّذ قبل أوّل مرّة. **ولا سطر منقول**: هذه إعادة تنفيذ مستقلّة على نيّة أندرويد وحدها
 * (`AGENTS.md` §0.4-١/٣ — ولا يُسمّى مرجعُ فكرة).
 *
 * ### وثلاث قواعد تحكم هذا الملفّ
 * ١. **القالب يُحلّ إلى قيم صريحة أو يُعلن نقصه** — لا يُرسل `{CORE_PATH}` حرفيًّا إلى محاكٍ فيفشل
 *    بلا سبب مفهوم. النقص ⇒ [Resolution.Missing] باسم المتغيّر.
 * ٢. **لا مسار جذر جديد في المستودع** — هذه الطبقة تحلّ القوالب وتُبلّغ بالنتيجة، وأي كتابة/أمر يبقى
 *    على مسار قائم واحد. ولذلك لا يوجد هنا تنفيذ أمر `am`؛ التنفيذ كلّه في [LaunchExecutor] بالنيّة.
 * ٣. **لا ادّعاء معرفة**: [LaunchRecipes.forSystem] تُرجع وصفةً معروفة أو `null`؛ و`null` ليست فشلًا
 *    بل «لا وصفة مُدرجة» — والمنفّذ يعرض منتقي النظام بنصّه.
 */

/** مصدر الوصفة — يُعرض للمستخدم، فلا تُقدَّم وصفة مُتعلَّمة كأنّها مُدرجة. */
enum class RecipeSource { BUILTIN, LEARNED, USER }

/**
 * متغيّرات القالب المدعومة — **قائمة مغلقة**.
 *
 * والقائمة المغلقة مقصودة: متغيّر يُكتب في وصفة ولا يُحلّ يصير نصًّا يُرسل إلى محاكٍ فيرفضه بلا
 * تفسير. فكل ما هنا يُحلّ، وكل ما ليس هنا يُعلَن نقصًا.
 */
enum class LaunchVariable(val token: String) {
    ROM_PATH("{ROM_PATH}"),
    ROM_DIR("{ROM_DIR}"),
    ROM_NAME("{ROM_NAME}"),
    ROM_NAME_NO_EXT("{ROM_NAME_NO_EXT}"),
    PACKAGE("{PACKAGE}"),
    ;

    companion object {
        /** كل متغيّر ظاهر في نصّ — لفحص ما بقي غير محلول. */
        fun inText(text: String): List<LaunchVariable> = entries.filter { text.contains(it.token) }
    }
}

/**
 * وصفة واحدة.
 *
 * @param extras أسماء إضافات النيّة ⇒ قوالبها (‏RetroArch يقرأ `-e ROM` أي `putExtra("ROM", …)`).
 * @param activityName `null` يعني «أرسل بالنيّة العامّة» ([action]) لا بمكوّن صريح.
 * @param systems الأنظمة التي تخدمها؛ الفراغ يعني «لا تُختار تلقائيًّا».
 */
data class LaunchRecipe(
    val id: String,
    val packageName: String,
    val activityName: String? = null,
    val action: String? = null,
    val extras: Map<String, String> = emptyMap(),
    val systems: Set<RomSystem> = emptySet(),
    val source: RecipeSource = RecipeSource.BUILTIN,
)

/** نتيجة حلّ القالب — حالتان لا واحدة. */
sealed interface Resolution {
    /** كل القيم حاضرة: إضافات جاهزة للإرسال. */
    data class Ready(val extras: Map<String, String>) : Resolution

    /** متغيّر بلا قيمة لهذه اللعبة — يُعلن بنصّه لا يُرسل حرفيًّا. */
    data class Missing(val token: String) : Resolution
}

/**
 * يحلّ كل إضافات الوصفة بالقيم المعطاة.
 *
 * **والقيمة الغائبة تُعلن باسمها لا تُترك حرفيّة:** المتغيّر المفقود يُرجع في [Resolution.Missing]
 * بنصّه (`{CORE_PATH}`)، فتصله الشاشة برسالتها ولا يصل إلى المحاكي نصٌّ لا معنى له.
 */
fun LaunchRecipe.resolve(values: Map<LaunchVariable, String>): Resolution {
    val rendered = LinkedHashMap<String, String>(extras.size)
    for ((key, template) in extras) {
        var text = template
        for (variable in LaunchVariable.inText(template)) {
            val value = values[variable] ?: return Resolution.Missing(variable.token)
            text = text.replace(variable.token, value)
        }
        // ما بقي من أقواس ليس متغيّرًا معروفًا — نصّ خام لا يُرسل.
        Regex("""\{[A-Z_]+\}""").find(text)?.let { return Resolution.Missing(it.value) }
        rendered[key] = text
    }
    return Resolution.Ready(rendered)
}

/** ما انتهى إليه طلب التشغيل — **لا «بدأت اللعبة» في أيّ منها**. */
sealed interface LaunchReport {
    data object Idle : LaunchReport

    /** أُرسلت النيّة إلى حزمة معروفة. وهذا لا يُثبت أن المحاكي عرض اللعبة. */
    data class HandedTo(val packageName: String) : LaunchReport

    /** عُرض منتقي النظام: تطبيق آخر هو من سيشغّل. */
    data object ChooserShown : LaunchReport

    /** المحاكي يمنع الإطلاق الخارجيّ (‏`exported="false"`) — ولا مسار جذر هنا. */
    data object NeedsRoot : LaunchReport

    /** لا تطبيق مثبّت يقبل هذا الملفّ. */
    data object NoHandler : LaunchReport

    /** الوصفة تنقصها قيمة، أو رفض النظام الطلب. */
    data class Refused(val token: String?) : LaunchReport
}

/**
 * الوصفات المُدرجة — **قائمة قصيرة مقصودة**.
 *
 * والمبدأ: **وصفة واحدة مضمونة** (‏RetroArch، عقدها موثَّق كما في الخطة §٢.٢-ب) + **منتقي النظام**
 * لكل ما عداها. لا نكتب مُهايئًا لكل محاكٍ بيدنا ونحن لا نستطيع قياسه.
 */
object LaunchRecipes {

    /**
     * ‏RetroArch — العقد الموثَّق: مكوّن صريح + إضافة `ROM`.
     *
     * **ولا تُمرَّر `LIBRETRO`:** مسار النواة يختلف بحسب ما ثبّته المستخدم، وتخمينه يُنتج فشلًا
     * صامتًا. فبلا نواة يفتح RetroArch محتواه ويطلب اختيار النواة — سلوكه المُعلَن. وحدّه مُعلن:
     * بلاغ `libretro/RetroArch#17433` يقول إن الإطلاق الخارجيّ تعطّل في إصدار ٢٠٢٥ ⇒ فشلُ الإطلاق
     * يُعرض بسببه ولا يُقنَّع.
     */
    val retroArch = LaunchRecipe(
        id = "retroarch",
        packageName = "com.retroarch.aarch64",
        activityName = "com.retroarch.browser.retroactivity.RetroActivityFuture",
        action = "android.intent.action.VIEW",
        extras = mapOf("ROM" to LaunchVariable.ROM_PATH.token),
        systems = RomSystem.entries.toSet(),
    )

    val builtins: List<LaunchRecipe> = listOf(retroArch)

    /** الوصفة المُدرجة لنظام، أو `null` («لا وصفة مُدرجة» لا «فشل»). */
    fun forSystem(system: RomSystem?): LaunchRecipe? =
        if (system == null) null else builtins.firstOrNull { system in it.systems }
}

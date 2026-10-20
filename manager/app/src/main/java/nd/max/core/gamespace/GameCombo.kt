/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.gamespace

/**
 * §1.1 — **Combo: مجموعة إعدادات مترابطة تُحفظ وتُعاد وتُعدَّل وتُطبَّق دفعةً واحدة.**
 *
 * ### ليست كاتبًا ثانيًا، وهذا أصل أمانها
 *
 * كل بند فيها يُسلَّم عبر القناة نفسها التي يكتب بها ملفّ اللعبة
 * (`AppSettingsViewModel.updateSetting` → `AppConfig` → المحرّك)، فأخضعُ ما يمكن أن تخضع له
 * المجموعة هو ما يخضع له الإعداد المفرد بالحرف: قاعدة **مالك المقبض الواحد** (`gpu_profile`
 * يُفرِّغ `gpu_max_freq` لأنهما لا يجتمعان)، وحدود العتاد، وتطبيق المحرّك عند واجهة اللعبة.
 * ولا كتابة sysfs من الواجهة مهما كان الطلب (ADR-11)؛ فالمجموعة **نيّة مجمّعة** لا سلطة جديدة.
 *
 * ### وحدودها الثلاثة مقيسة لا موعودة — في [GameComboVetting]
 *
 * 1. **لا مفتاح خارج الكتالوج:** مفتاح مجهول يُرفض، فلا يُمرَّر ملفّ معدَّل يدويًّا مفاتيحَ
 *    ما صُمِّم لها `AppConfig`.
 * 2. **لا قيمة خارج المجال:** مجال كل مفتاح هو نفسه الذي تعرضه واجهة الملفّ.
 * 3. **وما لا يدعمه الجهاز يُعلَن:** مفتاحان قيمتهما من قدرات الجهاز (`refresh_rate` ·
 *    `resolution_downscale`) تُقابَل بقوائم الجهاز المقروءة؛ وما ليس فيها لا يُكتب ويُسجَّل
 *    «غير مدعوم» — ولا يُدَّعى تطبيق ما لم يُقبل.
 *
 * ### والترميز سطريّ صغير مقصود
 *
 * لا JSON: المخزن ملفّ `SharedPreferences` واحد، والترميز هنا **نقيّ** — يُختبر على JVM بلا
 * أندرويد، ويسقط السطر التالف وحده (بيانات قديمة أو معدَّلة بيد لا تُسقط المجموعات كلها).
 */
data class GameCombo(
    val id: String,
    val name: String,
    /** مفتاح `AppConfig` → القيمة بالحرف كما يقرؤها المخزن (`"true"` · `"gaming"` · `"0.67"`). */
    val entries: Map<String, String>,
) {
    companion object {
        /** معرّف قصير عشوائي — الطول 8 يكفي لملفّ محليٍّ ولا يُدَّعى أنه فريد عالميًّا. */
        fun newId(): String = java.util.UUID.randomUUID().toString().substring(0, 8)
    }
}

/**
 * **ما تقبله المجموعة** — النسخة الواحدة من القائمة، ومنها تُبنى الواجهة والتحقق معًا.
 *
 * والمفاتيح منقولة عن واجهة الملفّ القائمة (`GameProfileDialog`) لا مخترعة: كل مفتاح هنا له
 * صفّ هناك يكتبه، وله قارئ في `AppConfig`. ومفتاح اللوحة (`GamePanelPrefs`) خارج الكتالوج عن
 * قصد — مخزنه مستقلّ وله مصالحة مع ملفّ الرفيق الجذري (`SidePanelArmedStore`)، فإدخاله يعني
 * كاتبًا ثانيًا داخل مجموعة كُتبت لتخضع لكاتب واحد.
 */
object GameComboCatalog {

    val TRI_STATE_VALUES: List<String> = listOf("default", "true", "false")

    val GPU_VALUES: List<String> = listOf("default", "power", "balanced", "gaming", "performance")

    /** المفاتيح الثلاثية كما تعدّها واجهة الملفّ — وبترتيب أقسامها. */
    val TRI_STATE_KEYS: List<String> = listOf(
        "touch_boost", "haptic_feedback",
        "perf_lite_mode", "cpu_boost", "kill_bg_apps", "game_preload",
        "force_hw_ui", "wifi_no_sleep", "dnd_on_gaming", "disable_notifs", "bypass_charging",
    )

    /** مفاتيح قيمها من مجموعة يملكها التطبيق نفسه. */
    val ENUM_KEYS: Map<String, List<String>> = mapOf("gpu_profile" to GPU_VALUES)

    /**
     * مفاتيح قيمها من **الجهاز**: القائمة المتاحة تُقرأ في موضع العرض (‏`getSupportedRefreshRates`
     * و`canvasScaleOptions`) وتُقدَّم إلى [GameComboVetting]؛ والنموذج لا يفترض قيمًا بدلها.
     */
    val DEVICE_KEYS: List<String> = listOf("refresh_rate", "resolution_downscale")

    /** كل مفتاح تقبله المجموعة، بترتيب العرض (يوافق أقسام واجهة الملفّ). */
    val ALL_KEYS: List<String> = TRI_STATE_KEYS + ENUM_KEYS.keys + DEVICE_KEYS

    /** المجال الثابت للمفتاح، أو null إن كان جهازيًّا (يُسأل الجهاز — لا يُفترض). */
    fun fixedDomain(key: String): List<String>? = when (key) {
        in TRI_STATE_KEYS -> TRI_STATE_VALUES
        in ENUM_KEYS -> ENUM_KEYS.getValue(key)
        else -> null
    }
}

/** حكم بند واحد في مجموعة عند عرضها على جهاز بعينه. */
enum class ComboEntryStatus {
    /** مفتاح وقيمة يقبلهما المخزن، والجهاز يدعمهما — يُسلَّم إلى `updateSetting`. */
    Accepted,

    /** مفتاح صالح لكن قيمة الجهاز الحالية لا تعرضها (معدل تحديث أو دقة غير مدعومة هنا). */
    Unsupported,

    /** مفتاح خارج الكتالوج أو قيمة خارج مجال مفتاحها — لا يُكتب، ولا يُصلح صامتًا. */
    Rejected,
}

/** نتائج عرض مجموعة على الجهاز: المقبول للكتابة، والمُعلَن غير المدعوم، والمرفوض. */
data class ComboVerdict(
    val accepted: List<Pair<String, String>>,
    val unsupported: List<String>,
    val rejected: List<String>,
) {
    /** كم مفتاحًا قُبل من كم — الرقمان يُعرضان معًا فلا يوهم ‏«طُبِّق» بتمام. */
    val acceptedCount: Int get() = accepted.size

    val totalCount: Int get() = accepted.size + unsupported.size + rejected.size
}

/** **عرضُ مجموعة على جهاز بعينه** — دالّة نقيّة تُختبر بلا أندرويد، وواجهة الملفّ تعرض أحكامها. */
object GameComboVetting {

    /**
     * @param deviceValues قيم المفاتيح الجهازية المقروءة الآن، بمفتاح المجال: قائمة معدلات
     *   التحديث المدعومة، ومعاملات الدقة المدعومة. مفتاح جهازي بلا قائمة معروفة **لا يُقبل
     *   أعمى** — يُعلَن غير مدعوم، فالحكم بالجهل ليس حكمًا.
     */
    fun vet(combo: GameCombo, deviceValues: Map<String, Set<String>>): ComboVerdict {
        val accepted = mutableListOf<Pair<String, String>>()
        val unsupported = mutableListOf<String>()
        val rejected = mutableListOf<String>()
        for ((key, value) in combo.entries) {
            when {
                key !in GameComboCatalog.ALL_KEYS -> rejected += key
                GameComboCatalog.fixedDomain(key)?.contains(value) == true -> accepted += key to value
                key in GameComboCatalog.DEVICE_KEYS ->
                    if (deviceValues[key]?.contains(value) == true) accepted += key to value
                    else unsupported += key
                else -> rejected += key
            }
        }
        return ComboVerdict(accepted = accepted, unsupported = unsupported, rejected = rejected)
    }
}

/** ترميز وفكّ ترميز سطريّ — نقيّ، وسطر تالف يسقط وحده. */
object GameComboCodec {

    private const val FIELD = "\t"
    private const val HEAD = "c"
    private const val ENTRY = "k"

    /** سقوف المشفر: لا ينمو المخزن بلا حدّ حتى لو كُتب يدويًّا. */
    const val MAX_COMBOS = 50
    const val MAX_NAME_LENGTH = 60
    const val MAX_ENTRIES = 32

    fun encode(combos: List<GameCombo>): String = buildString {
        combos.take(MAX_COMBOS).forEach { combo ->
            append(HEAD).append(FIELD).append(clean(combo.id)).append(FIELD).append(clean(combo.name)).append('\n')
            combo.entries.entries.take(MAX_ENTRIES).forEach { (key, value) ->
                append(ENTRY).append(FIELD).append(clean(combo.id)).append(FIELD)
                    .append(clean(key)).append(FIELD).append(clean(value)).append('\n')
            }
        }
    }

    /**
     * وفكّ الترميز **لا يثق بالمدخل**: أسطر ناقصة أو بأحرف فاصلة زائدة تُسقط وحدها، ومجموعة بلا
     * سطر ترويسة لا تُخترع لها ترويسة، والحدود [MAX_COMBOS]/[MAX_NAME_LENGTH]/[MAX_ENTRIES]
     * تُطبَّق هنا أيضًا لأن الملفّ قد يُحرَّر خارج التطبيق.
     */
    fun decode(text: String?): List<GameCombo> {
        if (text.isNullOrBlank()) return emptyList()
        val names = LinkedHashMap<String, String>()
        val entries = LinkedHashMap<String, LinkedHashMap<String, String>>()
        for (line in text.lineSequence()) {
            val parts = line.split(FIELD)
            when {
                parts.size == 3 && parts[0] == HEAD -> {
                    val id = parts[1].takeIf { it.isNotBlank() && !it.contains('\n') } ?: continue
                    if (names.size >= MAX_COMBOS && id !in names) continue
                    names[id] = parts[2].take(MAX_NAME_LENGTH)
                    entries.getOrPut(id) { LinkedHashMap() }
                }

                parts.size == 4 && parts[0] == ENTRY -> {
                    val id = parts[1]
                    val bucket = entries.getOrPut(id) { LinkedHashMap() }
                    if (bucket.size < MAX_ENTRIES) bucket[parts[2]] = parts[3]
                }

                // سطر تالف: يُتجاهل وحده ولا يُسقط الملفّ.
            }
        }
        return names.entries.map { (id, name) -> GameCombo(id, name, entries[id].orEmpty()) }
    }

    /** الحقول لا تحمل الفاصل ولا سطرًا جديدًا — يُنظَّف عند الكتابة فلا يتشوّه التركيب. */
    private fun clean(field: String): String =
        field.replace(FIELD, " ").replace('\n', ' ').replace('\r', ' ')
}

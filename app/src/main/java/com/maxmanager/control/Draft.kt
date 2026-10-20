package com.maxmanager.control

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

/**
 * مسودّة الإعداد كما يحرّرها المالك — **وحالتها Compose لا كائن JSON مباشر**.
 *
 * حقل نصّ يقرأ من كائن غير مراقَب لا يُعيد الرسم، فيُكتب الحرف ثم يعود النصّ إلى قيمته
 * القديمة — عطب واجهة كلاسيكي في نمط «حرّر JSON مباشرةً».
 */

/** نافذة مجّاني واحدة أثناء التحرير. */
@Stable
internal class PromoDraft {
    var from by mutableStateOf("")
    var to by mutableStateOf("")
    var note by mutableStateOf("")
}

@Stable
internal class Draft {
    private var base = JSONObject()
    private var snapshot by mutableStateOf("")

    /**
     * ما هو منشور على الخادم **الآن** — يُقرأ في تبويب «التحديث» فيعرف المالك من أين ينطلق:
     * «المنشور حاليًا v2.2 · مراجعة 6» بدل أن يحرّر رقمًا لا يعرف سابقه.
     */
    var baseVersion by mutableStateOf("")
        private set
    var baseRevision by mutableStateOf(0L)
        private set

    var version by mutableStateOf("")
    var released by mutableStateOf("")
    var downloadUrl by mutableStateOf("")
    var subUrl by mutableStateOf("")
    var payAr by mutableStateOf("")
    var payEn by mutableStateOf("")
    var annEnabled by mutableStateOf(false)
    var annAr by mutableStateOf("")
    var annEn by mutableStateOf("")
    val promos = mutableStateListOf<PromoDraft>()
    val changelog = mutableStateListOf<JSONObject>()
    val titles = mutableStateMapOf<String, String>()
    val states = mutableStateMapOf<String, String>()
    val msgsAr = mutableStateMapOf<String, String>()
    val msgsEn = mutableStateMapOf<String, String>()

    val dirty: Boolean get() = snapshot.isNotEmpty() && build().toString() != snapshot
    val featureIds: List<String> get() = titles.keys.sortedBy { (titles[it] ?: it).lowercase() }

    fun load(c: JSONObject) {
        base = c
        val latest = c.optJSONObject("latest") ?: JSONObject()
        baseVersion = latest.optString("version")
        baseRevision = c.optLong("revision")
        version = latest.optString("version")
        released = latest.optString("released")
        downloadUrl = latest.optString("download_page")
        val sub = c.optJSONObject("subscription") ?: JSONObject()
        subUrl = sub.optString("url")
        payAr = sub.optString("payment_text_ar")
        payEn = sub.optString("payment_text_en")
        val ann = c.optJSONObject("announcement") ?: JSONObject()
        annEnabled = ann.optBoolean("enabled", false)
        annAr = ann.optString("text_ar")
        annEn = ann.optString("text_en")
        changelog.clear()
        val log = c.optJSONArray("changelog") ?: JSONArray()
        for (i in 0 until log.length()) log.optJSONObject(i)?.let { changelog.add(it) }
        promos.clear()
        val windows = c.optJSONArray("promos") ?: JSONArray()
        for (i in 0 until windows.length()) {
            val w = windows.optJSONObject(i) ?: continue
            promos.add(
                PromoDraft().apply {
                    from = Validators.momentDisplay(w.optString("from"))
                    to = Validators.momentEndDisplay(w.optString("to"))
                    note = w.optString("note")
                }
            )
        }
        titles.clear(); states.clear(); msgsAr.clear(); msgsEn.clear()
        val feats = c.optJSONObject("features") ?: JSONObject()
        feats.keys().forEach { id ->
            val f = feats.optJSONObject(id) ?: JSONObject()
            titles[id] = f.optString("title", id)
            states[id] = f.optString("state", "on")
            msgsAr[id] = f.optString("message_ar")
            msgsEn[id] = f.optString("message_en")
        }
        snapshot = build().toString()
    }

    fun discard() = load(base)

    /** Copies the loaded config and overrides only what the owner edits, so unknown keys survive. */
    fun build(): JSONObject {
        val out = JSONObject(base.toString())
        val v = version.trim()
        val latest = out.optJSONObject("latest") ?: JSONObject()
        // **رابط التنزيل كما كتبه المالك** — لا يُبنى من اسم المستودع ولا من رقم الإصدار:
        // المالك يرفع النسخة حيث يشاء (GitHub أو خادمه)، ورابطٌ مُخمَّنٌ يكذب على المستخدم.
        latest.put("version", v).put("released", released.trim())
            .put("download_page", downloadUrl.trim())
        out.put("latest", latest)

        val sub = out.optJSONObject("subscription") ?: JSONObject()
        sub.put("url", subUrl.trim()).put("payment_text_ar", payAr).put("payment_text_en", payEn)
        out.put("subscription", sub)

        val feats = out.optJSONObject("features") ?: JSONObject()
        for (id in titles.keys.toList()) {
            val f = feats.optJSONObject(id) ?: JSONObject().put("title", titles[id] ?: id)
            f.put("title", titles[id] ?: id)
                .put("state", states[id] ?: "on")
                .put("message_ar", msgsAr[id] ?: "")
                .put("message_en", msgsEn[id] ?: "")
            feats.put(id, f)
        }
        out.put("features", feats)

        val ann = out.optJSONObject("announcement") ?: JSONObject()
        ann.put("enabled", annEnabled).put("text_ar", annAr).put("text_en", annEn)
        out.put("announcement", ann)

        val log = JSONArray()
        changelog.forEach { log.put(it) }
        out.put("changelog", log)

        // نوافذ المجّاني — تُوقَّع مع الإعداد نفسه، فالخادم يقرأ منها **وحدها** عند حساب
        // `expires_at` (عدّاد المفاتيح المدفوعة يتوقف داخل النافذة). ولو كُتبت في مكان آخر
        // لما وثقت الشاشة بالخادم: الشاشة تعرض ما وقّعه الخادم لا ما ادّعاه التطبيق.
        val windows = JSONArray()
        promos.forEach { p ->
            windows.put(
                JSONObject()
                    // `from` = بداية يوم البداية، و`to` = بداية اليوم التالي للنهاية: النافذة
                    // في التطبيق نصف مفتوحة `[from, to)`، فبلا هذه الإزاحة يُحسب اليوم الأخير
                    // **خارج** الوعد بينما المالك يقرؤه داخلًا — ويومٌ كامل من اللبس في الوجهين.
                    .put("from", Validators.momentStart(p.from.trim()))
                    .put("to", Validators.momentEndExclusive(p.to.trim()))
                    .put("note", p.note.trim())
            )
        }
        out.put("promos", windows)
        return out
    }

    /** Arabic message for the first problem found, or null when the draft is safe to publish. */
    fun validate(): String? = when {
        !Validators.isVersion(version.trim()) -> "رقم الإصدار الحالي يجب أن يكون مثل 2.1"
        released.isNotBlank() && !Validators.isDate(released.trim()) -> "تاريخ الإصدار يجب أن يكون بصيغة 2026-10-14"
        // رابط التنزيل تكتبه أنت، ويُرفض الفارغ: بلا رابط يفقد شاشة التحديث زرّ التنزيل عند
        // كل المستخدمين، وهذا أسوأ من رابط قديم — فالغياب عطب لا اختيار.
        !Validators.isHttpsUrl(downloadUrl.trim()) -> "رابط التنزيل يجب أن يبدأ بـ https:// (اكتبه بنفسك)"
        subUrl.isNotBlank() && !Validators.isHttpsUrl(subUrl.trim()) -> "رابط الاشتراك يجب أن يبدأ بـ https://"
        states.values.any { it == "subscribe" } && subUrl.isBlank() -> "هناك ميزة بوضع «اشتراك» لكن رابط الاشتراك فارغ"
        annEnabled && annAr.isBlank() && annEn.isBlank() -> "الإعلان مفعَّل لكن نصّه فارغ"
        promos.any { !Validators.isMoment(it.from.trim()) || !Validators.isMoment(it.to.trim()) } ->
            "تواريخ فترة المجّاني يجب أن تكون 2026-10-14 أو لحظة ISO كاملة"
        promos.any { !Validators.isWindowOrdered(it.from.trim(), it.to.trim()) } ->
            "في فترة المجّاني: النهاية يجب أن تكون بعد البداية"
        promos.size > 64 -> "أكثر من 64 فترة: التطبيق يتجاهل ما زاد، فلا تنشر ما لن يُقرأ"
        else -> null
    }

    /**
     * فرق مختصر بين المسودّة والمنشور — يُعرض في تأكيد النشر.
     *
     * وُجد لأن «نشر التغييرات» في اللوحة القديمة كان زرًّا أعمى: الضغط يُنشر **كل شيء** بلا
     * معرفة ماذا سيصل. وهذه القائمة تقول بالعربية ما سيتغيّر فعلًا، سطرًا لكل مجال تغيّر.
     */
    fun summarise(): List<String> {
        val after = build()
        val before = runCatching { JSONObject(base.toString()) }.getOrDefault(JSONObject())
        val out = mutableListOf<String>()

        val latestBefore = before.optJSONObject("latest") ?: JSONObject()
        val latestAfter = after.optJSONObject("latest") ?: JSONObject()
        if (latestBefore.optString("version") != latestAfter.optString("version")) {
            out += "الإصدار: ${latestBefore.optString("version").ifEmpty { "—" }} ← ${latestAfter.optString("version")}"
        }
        if (latestBefore.optString("released") != latestAfter.optString("released")) out += "تاريخ الإصدار تغيّر"
        if (latestBefore.optString("download_page") != latestAfter.optString("download_page")) out += "رابط التنزيل تغيّر"

        val featsBefore = before.optJSONObject("features") ?: JSONObject()
        val featsAfter = after.optJSONObject("features") ?: JSONObject()
        val changedFeatures = titles.keys.filter { id ->
            val b = featsBefore.optJSONObject(id) ?: JSONObject()
            val a = featsAfter.optJSONObject(id) ?: JSONObject()
            b.toString() != a.toString()
        }.map { titles[it] ?: it }
        if (changedFeatures.isNotEmpty()) out += "ميزات متغيّرة: ${changedFeatures.joinToString("، ")}"

        if (before.optJSONObject("subscription")?.toString() != after.optJSONObject("subscription")?.toString()) {
            out += "بيانات الاشتراك تغيّرت"
        }
        if (before.optJSONObject("announcement")?.toString() != after.optJSONObject("announcement")?.toString()) {
            out += "الإعلان تغيّر"
        }
        val promosBefore = before.optJSONArray("promos")?.toString()
        val promosAfter = after.optJSONArray("promos")?.toString()
        if (promosBefore != promosAfter) out += "فترات المجّاني: ${after.optJSONArray("promos")?.length() ?: 0}"

        if (before.optJSONArray("changelog")?.toString() != after.optJSONArray("changelog")?.toString()) {
            out += "سجل التغييرات: ${changelog.size} إصدارًا"
        }
        return out
    }

    /** ملاحظات الإصدار المحرَّر الآن (بالعربية إن وُجدت)، لعرضها في معاينة نافذة التحديث. */
    fun previewNotes(): List<String> {
        val entry = changelog.firstOrNull { it.optString("version") == version.trim() } ?: return emptyList()
        val ar = entry.optJSONArray("notes_ar") ?: JSONArray()
        val en = entry.optJSONArray("notes_en") ?: JSONArray()
        val source = if (ar.length() > 0) ar else en
        val out = mutableListOf<String>()
        for (i in 0 until source.length()) {
            source.optString(i).takeIf { it.isNotBlank() }?.let { out += it }
        }
        return out
    }

    /** هل في السجل ملاحظات للإصدار المحرَّر؟ — تُستخدم لتنبيه «بلا الجديد» في المعاينة. */
    fun hasNotesForCurrentVersion(): Boolean =
        changelog.any { it.optString("version") == version.trim() }
}

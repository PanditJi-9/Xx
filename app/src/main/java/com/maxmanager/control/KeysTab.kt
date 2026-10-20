package com.maxmanager.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * قسم المفاتيح — كل نداءاته تمرّ بدالّة `maxctl` وحدها (`Api.license*`).
 *
 * ثلاث حقائق تحكم رسمه:
 * ① **نصّ المفتاح يُعرض مرّة واحدة**: الخادم يخزّن بصمة `sha256` لا المفتاح، فلو أُغلق
 *    الصندوق بلا نسخٍ فلا سبيل لاستعادته — يُقال ذلك صراحةً فوق النصّ.
 * ② **العمليات المدمِّرة تُؤكَّد**: الإلغاء وتصفير الأجهزة لا رجعة لهما من هنا.
 * ③ **كل زرّ يعيد الجلب بعد نجاحه**: الشاشة تعرض ما في الخادم لا ما تظنّ أنها أرسلته.
 *
 * وأُعيد ترتيبه في جولة إعادة التصميم: قيم الإنشاء كلها في **بطاقة واحدة موسّعة** (قوالب
 * مدّةٍ وحدٍّ وعدد، وسطر حسابي يُرى قبل الضغط، وحدود الخادم تُفحص قبله)، والقائمة في بطاقة
 * ثانية مستقلة تُجلب عند فتح التبويب لا بضغطة يدوية.
 */

/** قوالب المدّة بالأيام (نفس حدود الخادم: 1..3650). */
private val DAY_PRESETS = listOf(30, 90, 365, 3650)

/** قوالب حدّ الأجهزة (1..50). */
private val LIMIT_PRESETS = listOf(1, 2, 3, 5)

/** قوالب عدد المفاتيح (1..200). */
private val COUNT_PRESETS = listOf(1, 5, 10, 25)

private fun presetLabel(days: Int): String = when (days) {
    30 -> "شهر"
    90 -> "3 أشهر"
    365 -> "سنة"
    else -> "10 سنوات"
}

/**
 * حالة قسم المفاتيح **مرفوعة خارج التبويب** عمدًا: تبديل التبويبات يفكّك تركيب الشاشة المخفيّة،
 * فلو كانت الحالة داخلها لضاع ما أنشأه المالك للتوّ بمجرّد نظرةٍ على تبويب آخر.
 */
@Stable
internal class KeysState {
    var plan by mutableStateOf("")
    var days by mutableStateOf("30")
    var limit by mutableStateOf("1")
    var count by mutableStateOf("1")
    var note by mutableStateOf("")
    var created by mutableStateOf<List<Pair<String, String>>>(emptyList())
    var rows by mutableStateOf<List<JSONObject>>(emptyList())
    var query by mutableStateOf("")
    var loadedOnce by mutableStateOf(false)
    var detail by mutableStateOf<JSONObject?>(null)
    var confirmRevoke by mutableStateOf<JSONObject?>(null)
    var confirmReset by mutableStateOf<JSONObject?>(null)
    var error by mutableStateOf("")
    var busy by mutableStateOf(false)
}

@Composable
internal fun KeysTab(session: Session, state: KeysState, onAuthExpired: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    fun run(block: suspend () -> Unit) {
        scope.launch {
            state.busy = true
            state.error = ""
            try {
                block()
            } catch (e: ApiException) {
                if (e.authExpired) onAuthExpired(e.message.orEmpty()) else state.error = e.message.orEmpty()
            }
            state.busy = false
        }
    }

    // معلّقة لا مُطلِقة: نداؤها داخل `run` ينفّذ في **نفس** الكوروتين، فلا كوروتينان يتزاحمان
    // على `busy` (الأول ينتهي فيُطفئه والثاني ما زال يعمل ⇒ أزرار تُفتح في نصف العمل).
    suspend fun loadRows() {
        state.rows = withContext(Dispatchers.IO) { Api.licenseList(session, state.query) }
    }

    // القائمة تُجلب عند أول فتح للتبويب — لا تنتظر ضغطة «تحديث القائمة» لتعرض شيئًا.
    LaunchedEffect(Unit) {
        if (!state.loadedOnce) run { loadRows(); state.loadedOnce = true }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionCard(
            "إنشاء مفاتيح",
            "نصّ المفتاح يُعرض مرّة واحدة: الخادم يحفظ بصمته لا نصّه — انسخه قبل أن تغلق القائمة."
        ) {
            if (state.error.isNotEmpty()) {
                Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Field(state.plan, { state.plan = it }, "اسم الخطة (اختياري — يظهر في تفاصيل المفتاح)")

            Text("المدّة — تبدأ من أول تفعيل للمفتاح", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DAY_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = state.days == preset.toString(),
                        onClick = { state.days = preset.toString() },
                        label = { Text(presetLabel(preset), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Field(
                state.days, { state.days = it },
                "المدّة بالأيام (1 إلى ${Validators.MAX_KEY_DAYS})",
                latin = true, single = true, keyboard = KeyboardType.Number
            )

            Text("حدّ الأجهزة لكل مفتاح", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LIMIT_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = state.limit == preset.toString(),
                        onClick = { state.limit = preset.toString() },
                        label = { Text(preset.toString(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Field(
                state.limit, { state.limit = it },
                "حدّ الأجهزة (1 إلى ${Validators.MAX_KEY_DEVICES})",
                latin = true, single = true, keyboard = KeyboardType.Number
            )

            Text("عدد المفاتيح", style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                COUNT_PRESETS.forEach { preset ->
                    FilterChip(
                        selected = state.count == preset.toString(),
                        onClick = { state.count = preset.toString() },
                        label = { Text(preset.toString(), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Field(
                state.count, { state.count = it },
                "العدد (1 إلى ${Validators.MAX_KEY_COUNT})",
                latin = true, single = true, keyboard = KeyboardType.Number
            )

            Field(state.note, { state.note = it }, "ملاحظة داخلية: لمن بِعت هذا المفتاح (اختياري)")

            // سطر حسابي قبل الضغط + فحص حدود الخادم — الخادم يستبدل الخارج بالافتراضي صامتًا،
            // فلا يُرسل ما سيُصحَّح: يُقال الخطأ هنا، حيث يمكن تصحيحه.
            val daysN = state.days.trim().toIntOrNull()
            val limitN = state.limit.trim().toIntOrNull()
            val countN = state.count.trim().toIntOrNull()
            val problem = when {
                daysN == null || limitN == null || countN == null -> "المدّة وحدّ الأجهزة والعدد: أرقام صحيحة بلا فراغات"
                else -> Validators.keyDraftProblem(daysN, limitN, countN)
            }
            Text(
                "سيُنشأ ${countN ?: 0} مفتاحًا · صالح ${daysN ?: 0} يومًا من أول تفعيل · حتى ${limitN ?: 0} جهاز لكل مفتاح.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (problem != null) {
                Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = {
                    run {
                        state.created = withContext(Dispatchers.IO) {
                            Api.licenseCreate(
                                session,
                                plan = state.plan.trim(),
                                days = daysN ?: 30,
                                deviceLimit = limitN ?: 1,
                                count = countN ?: 1,
                                note = state.note.trim(),
                            )
                        }
                        loadRows()
                        state.loadedOnce = true
                    }
                },
                enabled = !state.busy && problem == null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("إنشاء") }
        }

        if (state.created.isNotEmpty()) {
            SectionCard("المفاتيح الجديدة", "احفظها الآن — لا تُعرض مرّة ثانية") {
                Text(
                    "انسخها الآن: الخادم يحفظ البصمة لا النصّ، ولا سبيل لاستعادة نصّ مفتاح بعد إغلاق هذه القائمة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                state.created.forEach { (key, hint) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(key, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { clipboard.setText(AnnotatedString(key)) }) { Text("نسخ") }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { clipboard.setText(AnnotatedString(state.created.joinToString("\n") { it.first })) }) {
                        Text("نسخ كل المفاتيح")
                    }
                    TextButton(onClick = { state.created = emptyList() }) { Text("إخفاء") }
                }
            }
        }

        SectionCard("القائمة", "كل زرّ يعيد الجلب بعد نجاحه — الشاشة تعرض ما في الخادم لا ما فُترض أنه أُرسل") {
            Field(state.query, { state.query = it }, "بحث ببداية المفتاح أو نهايته", latin = true, single = true)
            OutlinedButton(
                onClick = { run { loadRows(); state.loadedOnce = true } },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تحديث القائمة")
            }
            if (state.rows.isEmpty()) {
                Text(
                    if (!state.loadedOnce) "جارٍ تحميل القائمة…"
                    else "لا مفاتيح في القائمة — أنشئ أول مفتاح من البطاقة أعلاه.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            state.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                val badge = keyBadgeOf(row)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.optString("hint"), style = MaterialTheme.typography.titleSmall)
                    Pill(badgeLabel(badge), badgeColor(badge))
                }
                Text(
                    "خطة: ${row.optString("plan").ifEmpty { "—" }} · ${row.optInt("duration_days")} يومًا · " +
                        "أجهزة ${row.optInt("devices_used")}/${row.optInt("device_limit")} · " +
                        "ينتهي: ${row.optString("expires_at").take(10).ifEmpty { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val rowNote = row.optString("note")
                if (rowNote.isNotEmpty()) {
                    Text("ملاحظة: $rowNote", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val lastSeen = row.optString("last_seen")
                if (lastSeen.isNotEmpty()) {
                    Text("آخر ظهور لجهاز: ${lastSeen.take(10)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { state.confirmRevoke = row }) {
                        Text(if (row.optBoolean("revoked", false)) "إلغاء الإلغاء" else "إلغاء")
                    }
                    TextButton(onClick = { run { Api.licenseExtend(session, row.optString("id"), 30); loadRows() } }) {
                        Text("+30 يومًا")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { state.confirmReset = row }) { Text("تصفير الأجهزة") }
                    TextButton(onClick = {
                        run { state.detail = withContext(Dispatchers.IO) { Api.licenseDetail(session, row.optString("id")) } }
                    }) {
                        Text("تفاصيل")
                    }
                }
            }
        }
    }

    state.confirmRevoke?.let { row ->
        val revoked = row.optBoolean("revoked", false)
        AlertDialog(
            onDismissRequest = { state.confirmRevoke = null },
            title = { Text(if (revoked) "إلغاء الإلغاء؟" else "إلغاء المفتاح؟") },
            text = {
                Text(
                    if (revoked) "سيعود المفتاح سارٍ لأجهزته المسجَّلة (إن لم تكن منتهية)."
                    else "الأجهزة المفعَّلة ستفقد الوصول عند أول تحقق، ولا رجعة إلا بإلغاء الإلغاء."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = row
                    state.confirmRevoke = null
                    run { Api.licenseRevoke(session, target.optString("id"), !revoked); loadRows() }
                }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick = { state.confirmRevoke = null }) { Text("رجوع") } }
        )
    }

    state.confirmReset?.let { row ->
        AlertDialog(
            onDismissRequest = { state.confirmReset = null },
            title = { Text("تصفير أجهزة المفتاح؟") },
            text = {
                Text(
                    "كل الأجهزة تُعلَّم غير نشطة فتصير المقاعد فارغة، والأثر يبقى في سجلّ المفتاح. " +
                        "استعمله حين يغيّر المشتري هاتفه."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val target = row
                    state.confirmReset = null
                    run { Api.licenseResetDevices(session, target.optString("id")); loadRows() }
                }) { Text("تصفير") }
            },
            dismissButton = { TextButton(onClick = { state.confirmReset = null }) { Text("رجوع") } }
        )
    }

    state.detail?.let { info ->
        val license = info.optJSONObject("license") ?: JSONObject()
        val devices = info.optJSONArray("devices") ?: JSONArray()
        val events = info.optJSONArray("events") ?: JSONArray()
        AlertDialog(
            onDismissRequest = { state.detail = null },
            title = { Text("تفاصيل ${license.optString("key_hint")}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "الحالة: ${if (license.optBoolean("revoked")) "ملغى" else "غير ملغى"} · " +
                            "${license.optInt("duration_days")} يومًا · حدّ ${license.optInt("device_limit")} جهاز",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val starts = license.optString("starts_at")
                    if (starts.isNotEmpty()) {
                        Text("البداية: ${starts.take(10)}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("الأجهزة (${devices.length()})", style = MaterialTheme.typography.titleSmall)
                    for (i in 0 until devices.length()) {
                        val d = devices.optJSONObject(i) ?: continue
                        val active = d.optBoolean("active", true)
                        Text(
                            "${d.optString("device").take(8)}… ${d.optString("label")} · " +
                                "${if (active) "نشط" else "مُصفَّر"} · ${d.optString("last_seen").take(10)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("السجلّ (${events.length()})", style = MaterialTheme.typography.titleSmall)
                    for (i in 0 until events.length()) {
                        val e = events.optJSONObject(i) ?: continue
                        Text(
                            "${e.optString("at").take(10)} · ${e.optString("kind")} ${e.optString("detail")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { state.detail = null }) { Text("إغلاق") } }
        )
    }
}

/** حالات المفتاح الأربع كما تُقرأ من صفّ الخادم — والوسم لونٌ ونصّ لا نصّ وحده. */
private enum class KeyBadge { ACTIVE, NOT_ACTIVATED, EXPIRED, REVOKED }

private fun keyBadgeOf(row: JSONObject): KeyBadge {
    if (row.optBoolean("revoked", false)) return KeyBadge.REVOKED
    val expires = row.optString("expires_at")
    if (expires.isEmpty()) return KeyBadge.NOT_ACTIVATED
    val expired = runCatching { Instant.parse(expires) }.getOrNull()?.isBefore(Instant.now()) == true
    return if (expired) KeyBadge.EXPIRED else KeyBadge.ACTIVE
}

private fun badgeLabel(badge: KeyBadge): String = when (badge) {
    KeyBadge.ACTIVE -> "سارٍ"
    KeyBadge.NOT_ACTIVATED -> "لم يُفعَّل بعد"
    KeyBadge.EXPIRED -> "منتهٍ"
    KeyBadge.REVOKED -> "ملغى"
}

@Composable
private fun badgeColor(badge: KeyBadge): Color = when (badge) {
    KeyBadge.ACTIVE -> OkColor
    KeyBadge.REVOKED -> MaterialTheme.colorScheme.error
    KeyBadge.NOT_ACTIVATED, KeyBadge.EXPIRED -> MaterialTheme.colorScheme.onSurfaceVariant
}

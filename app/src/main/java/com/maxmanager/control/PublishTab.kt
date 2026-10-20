package com.maxmanager.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * تبويب «التحديث» — كل ما يخصّ نشر إصدار: الرقم، الرابط، السجل، **ومعاينة النافذة**.
 *
 * ## ما أُصلح هنا (شكوى المالك: «قسم التحديث غير مترابط، وفيه عيوب منطقية»)
 *
 * ① **المعاينة قبل النشر**: بطاقة تُرسم من نفس الحقول التي ستُنشر (لا نسخة موازية)، فيرى
 *    المالك نصّ النافذة كما سيقرؤه المستخدم قبل أن يضغط «نشر».
 * ② **«المنشور حاليًا» ظاهر فوق الحقول**: كان المالك يحرّر رقمًا لا يعرف سابقه.
 * ③ **تحذير الملاحظات الناقصة**: إصدارٌ بلا مدخلٍ في السجل يُقال عنه صراحةً — لا يُترك
 *    المستخدم أمام نافذة بلا «الجديد» بلا سبب مفهوم.
 * ④ **تاريخ اليوم بزرّ**: أقل كتابة يدوية على هاتف.
 * ⑤ **زرّ نشر داخل التبويب** يمرّ بالتأكيد نفسه أسفل الشاشة (مسار واحد للنشر لا اثنان).
 */
@Composable
internal fun UpdateTab(
    d: Draft,
    loaded: Boolean,
    busy: Boolean,
    onPublish: () -> Unit,
    onAddRelease: (String, String, List<String>, List<String>) -> String?,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (!loaded) {
            SectionCard("التحديث", "الإعداد لم يُقرأ من الخادم بعد") {
                Text(
                    "اضغط «تحديث» في أعلى الشاشة ثم أعد المحاولة. لا يُنشر شيء وأنت لا ترى الحالة " +
                        "الحقيقية المنشورة.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        SectionCard(
            "الإصدار والرابط",
            "المنشور الآن على الخادم: v${d.baseVersion.ifEmpty { "—" }} · مراجعة ${d.baseRevision}. " +
                "تطبيق المستخدم يقارن رقمك بنسخته المثبَّتة، فإن كان رقمك أحدث ظهرت له نافذة التحديث."
        ) {
            Field(d.version, { d.version = it }, "رقم الإصدار مثل 2.2", latin = true, single = true, keyboard = KeyboardType.Decimal)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Field(d.released, { d.released = it }, "تاريخ الإصدار (2026-10-15)", latin = true, single = true)
                }
                TextButton(onClick = { d.released = LocalDate.now().toString() }) { Text("اليوم") }
            }
            Field(
                d.downloadUrl,
                { d.downloadUrl = it },
                "رابط التنزيل (https://…)",
                latin = true,
                single = true,
                keyboard = KeyboardType.Uri
            )
            Text(
                "أي رابط https تريده: GitHub أو خادمك — والزرّ في نافذة المستخدم **يفتحه في المتصفّح**، " +
                    "لا تنزيل داخل التطبيق.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onPublish,
                enabled = d.dirty && !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (d.dirty) "نشر التحديث الآن" else "لا تغييرات منذ آخر نشر")
            }
        }

        SectionCard("معاينة نافذة المستخدم", "هكذا تُبنى النافذة في MaxManager — من هذه الحقول نفسها") {
            UpdatePreview(d)
        }

        ChangelogSection(d, onAddRelease)
    }
}

/** معاينة مطابقة لما يرسمه `ConfigUpdateHost` في التطبيق: العنوان · السطران · «الجديد» · الزرّان. */
@Composable
private fun UpdatePreview(d: Draft) {
    val notes = remember(d.version, d.changelog.size) { d.previewNotes() }
    val hasEntry = remember(d.version, d.changelog.size) { d.hasNotesForCurrentVersion() }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("يتوفّر تحديث", style = MaterialTheme.typography.titleMedium)
        Text(
            "الإصدار ${d.version.ifEmpty { "—" }} متاح، وأنت على نسختك الحالية.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (d.released.isNotBlank()) {
            Text(
                "تاريخ الإصدار: ${d.released}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (notes.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text("الجديد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            notes.forEach { note ->
                Text("• $note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            // الزرّان رسمٌ لا فعل: هما في التطبيق، وهذا معرض للألوان والنصوص فقط.
            Text("لاحقًا", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("حدّث الآن", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (d.version.trim().isNotEmpty() && !hasEntry) {
        Text(
            "لا ملاحظات لهذا الإصدار في السجل — المستخدم سيرى الرقم والرابط بلا «الجديد». " +
                "أضف مدخلًا أدناه ليظهر.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * سجلّ التغييرات — يُنشر مع الإعداد نفسه، ويقرؤه التطبيق لعرض «الجديد» في نافذة التحديث.
 * (وكان قبل هذه الجولة يُنشر ويُهمَل: العقد في MaxManager لم يكن يعرف الحقل.)
 */
@Composable
private fun ChangelogSection(d: Draft, onAdd: (String, String, List<String>, List<String>) -> String?) {
    val today = remember { LocalDate.now().toString() }
    var newVersion by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf(today) }
    var newNotesAr by remember { mutableStateOf("") }
    var newNotesEn by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    SectionCard("سجل التغييرات", "الأحدث أولًا — ويظهر «الجديد» في النافذة من هنا") {
        d.changelog.forEachIndexed { i, e ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("v${e.optString("version")}", style = MaterialTheme.typography.titleSmall)
                    val notes = (e.optJSONArray("notes_ar")?.length() ?: 0).coerceAtLeast(e.optJSONArray("notes_en")?.length() ?: 0)
                    Text("${e.optString("date")} · $notes ملاحظة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { d.changelog.removeAt(i) }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Text("إصدار جديد", style = MaterialTheme.typography.titleSmall)
        Field(newVersion, { newVersion = it }, "رقم الإصدار الجديد", latin = true, single = true, keyboard = KeyboardType.Decimal)
        Field(newDate, { newDate = it }, "التاريخ (2026-10-14)", latin = true, single = true)
        Field(newNotesAr, { newNotesAr = it }, "ملاحظات بالعربية، سطر لكل بند", minLines = 3)
        Field(newNotesEn, { newNotesEn = it }, "Notes in English, one per line", latin = true, minLines = 3)
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = {
                val result = onAdd(newVersion, newDate, Validators.noteLines(newNotesAr), Validators.noteLines(newNotesEn))
                error = result.orEmpty()
                if (result == null) {
                    newVersion = ""; newDate = today; newNotesAr = ""; newNotesEn = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("إضافة إلى السجل") }
    }
}

package com.maxmanager.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * تبويب البيانات — الأربعة التي يقرؤها المالك من الخادم: المستخدمون، التوافق، صحّة التحكّم،
 * وسجلّ النشر.
 *
 * ## ولماذا «لا يعمل» كان تشخيصًا خاطئًا — عولج بالشرح والفصل
 *
 * قِيس حيًّا أن الدوالّ الأربع موجودة على الخادم وتتطلّب مصادقة المالك (‏`42501` لمجهول)،
 * فالعطب لم يكن غيابًا. الاحتمال الأشيع لما رآه المالك: **جداول فارغة** — والتطبيق المثبَّت
 * عنده كان بلا اتصال بالخادم أصلًا، فلا تثبيت يُسجَّل ولا إحصاء يُرسل. فالبطاقة الأولى هنا
 * تقول هذا صراحةً بلحظة القراءة، وكل قسم يقول لماذا هو فارغ بدل أن يبدو معطّلًا.
 *
 * ## والفصل عن الإعداد
 *
 * في اللوحة القديمة كان كل شيء تحت `if (loaded)` — فلو تعذّر تحميل الإعداد اختفت أقسام
 * البيانات والمفاتيح معه، فيُقرأ العطب في الإعداد كعطبٍ في البيانات. الآن لكل تبويب حاله:
 * هذه الشاشة تُقرأ من `admin_*` وحدها، ولا تنتظر `app_config`.
 */
@Composable
internal fun DataTab(
    stats: List<CountryStat>, statsError: String,
    compat: List<CompatStat>, compatError: String,
    health: List<HealthStat>, healthError: String,
    audit: List<AuditRow>, auditError: String,
    lastLoadedAt: String,
    busy: Boolean,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SectionCard(
            "لحظة القراءة",
            "الأرقام تُجلب من الخادم عند الضغط — ليست حيّةً، والفراغ ليس عطبًا: قد لا يكون وصل شيء بعد."
        ) {
            Text(
                if (lastLoadedAt.isEmpty()) "لم تُقرأ بعد — اضغط الزرّ."
                else "آخر قراءة: $lastLoadedAt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onRefresh, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("تحديث البيانات")
            }
            Text(
                "متى تمتلئ؟ بعد أن يحدّث المستخدمون إلى نسخة تتّصل بالخادم، وتُمنح الموافقة على " +
                    "الإحصاءات في إعدادات التطبيق. حتى ذلك الحين هذه الأقسام تبقى فارغة عن صدق.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        UsersSection(stats, statsError)
        CompatSection(compat, compatError)
        HealthSection(health, healthError)
        AuditSection(audit, auditError)
    }
}

@Composable
private fun UsersSection(stats: List<CountryStat>, error: String) {
    var showAll by remember { mutableStateOf(false) }
    val total = stats.sumOf { it.total }
    val active = stats.sumOf { it.active }
    SectionCard("المستخدمون", "تثبيتات مجمَّعة بحسب الدولة — بلا أي معرّف شخصي") {
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("إجمالي التثبيتات", total.toString(), Modifier.weight(1f))
                StatTile("نشطة خلال 30 يومًا", active.toString(), Modifier.weight(1f))
            }
            if (stats.isEmpty()) {
                Text(
                    "لا تثبيتات مسجَّلة بعد — يُسجَّل التثبيت بعد أن يفتح مستخدمٌ نسخةً متّصلة " +
                        "بالخادم ويمنح الموافقة على الإحصاءات.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val shown = if (showAll) stats else stats.take(8)
            shown.forEach { s ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${flag(s.code)}  ${countryName(s.code)}", style = MaterialTheme.typography.bodyMedium)
                        Text("${s.total}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (total > 0) s.total.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                    )
                }
            }
            if (stats.size > 8) {
                TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "عرض أقل" else "عرض الكل (${stats.size})") }
            }
        }
    }
}

@Composable
private fun CompatSection(rows: List<CompatStat>, error: String) {
    var showAll by remember { mutableStateOf(false) }
    SectionCard("التوافق والأجهزة", "أعلى ٢٠٠ تركيبة شركة/طراز/إصدار — يجمعها الخادم بلا معرّفات") {
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else if (rows.isEmpty()) {
            Text(
                "لا توجد بيانات توافق بعد — تصل مع أول إشارة إحصاء بعد موافقة المستخدم.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val shown = if (showAll) rows else rows.take(10)
            shown.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            listOf(row.manufacturer, row.model).filter { it.isNotBlank() }
                                .joinToString(" ").ifBlank { "غير معلوم" },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Android ${if (row.sdk > 0) row.sdk.toString() else "—"} · ${envLabel(row.env)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${row.total}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${row.active} نشط",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
            if (rows.size > 10) {
                TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "عرض أقل" else "عرض الكل (${rows.size})") }
            }
        }
    }
}

@Composable
private fun HealthSection(rows: List<HealthStat>, error: String) {
    SectionCard("صحّة التحكّم", "أعداد لا قيم: ما أُثبت · ما ارتدّ · ما بقي مجهولًا (من إشارات الموافقين)") {
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else if (rows.isEmpty()) {
            Text(
                "لا توجد عدّادات بعد — أول إجراء تحكّم بعد الموافقة يُعدّ هنا.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(domainLabel(row.domain), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        "أُثبت ${row.confirmed} · ارتدّ ${row.rolledBack} · مجهول ${row.unknown}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    "من ${row.installs} تثبيتًا أرسل هذا المجال",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AuditSection(rows: List<AuditRow>, error: String) {
    SectionCard("سجلّ نشر الإعداد", "آخر ٥٠ نشرة — والسجلّ يُلحق فقط: لا تعديل ولا حذف") {
        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else if (rows.isEmpty()) {
            Text(
                "لا نشرات مسجَّلة بعد — أول «نشر» يظهر هنا.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("مراجعة ${row.revision}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        shortStamp(row.at),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (row.changed.isNotEmpty()) {
                    Text(
                        "تغيّر: ${row.changed.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

package com.maxmanager.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/**
 * تبويب «التخصيص» — ما يراه المستخدم فعلًا: الشاشات بأسمائها ورسائلها، الاشتراك، الإعلان،
 * وفترات المجّاني.
 *
 * ## وما أُضيف في إعادة التصميم
 *
 * * **اسم السطح صار قابلًا للتحرير** (`title`): كان يُقرأ ويُرسل ولا يُحرَّر من اللوحة، وقد صار
 *   هو **عنوان نافذة الترخيص** في MaxManager — يكتب المالك «Max AI» أو أي اسم يشاء فيراه
 *   المستخدم حرفيًّا في نافذة القفل.
 * * **ورسالة الميزة** (`message_ar`/`message_en`) تظهر الآن في نافذة القفل نفسها — لا في
 *   مكان آخر فقط.
 * * ورابط الاشتراك ونصّه يصيران زرّا اشتراك في نافذة القفل — يُقال ذلك في تلميح القسم.
 */

/** حالات مفتاح الميزة الثلاث كما تفهمها اللوحة والتطبيق معًا. */
private val STATES = linkedMapOf("on" to "تشغيل", "subscribe" to "اشتراك", "off" to "إيقاف")

@Composable
internal fun CustomizeTab(d: Draft, loaded: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        if (!loaded) {
            SectionCard("التخصيص", "الإعداد لم يُقرأ من الخادم بعد") {
                Text(
                    "اضغط «تحديث» في أعلى الشاشة ثم أعد المحاولة — التحرير بلا الحالة الحقيقية " +
                        "قد ينشر ما لا تتوقّعه.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }
        FeaturesSection(d)
        SubscriptionSection(d)
        AnnouncementSection(d)
        PromosSection(d)
    }
}

@Composable
private fun FeaturesSection(d: Draft) {
    SectionCard(
        "الشاشات والميزات",
        "تشغيل: متاح للجميع · اشتراك: مقفل بنافذة ترخيص · إيقاف: مخفي مع رسالة. " +
            "الاسم والرسالة يظهران حرفيًّا في نافذة القفل داخل التطبيق."
    ) {
        d.featureIds.forEachIndexed { index, id ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Field(d.titles[id] ?: id, { d.titles[id] = it }, "اسم السطح كما يظهر في التطبيق")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                STATES.forEach { (value, label) ->
                    FilterChip(
                        selected = d.states[id] == value,
                        onClick = { d.states[id] = value },
                        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if ((d.states[id] ?: "on") != "on") {
                Field(d.msgsAr[id] ?: "", { d.msgsAr[id] = it }, "الرسالة بالعربية (تظهر في نافذة القفل)")
                Field(d.msgsEn[id] ?: "", { d.msgsEn[id] = it }, "Message in English", latin = true)
            }
        }
    }
}

@Composable
private fun SubscriptionSection(d: Draft) {
    SectionCard("الاشتراك والدفع", "يظهر زرّ اشتراك في نافذة القفل داخل التطبيق حين يكون الرابط هنا غير فارغ") {
        Field(d.subUrl, { d.subUrl = it }, "رابط الاشتراك", latin = true, single = true, keyboard = KeyboardType.Uri)
        Field(d.payAr, { d.payAr = it }, "نص الدفع بالعربية (سطر يظهر فوق زرّ الاشتراك)")
        Field(d.payEn, { d.payEn = it }, "Payment text in English", latin = true)
    }
}

@Composable
private fun AnnouncementSection(d: Draft) {
    SectionCard("إعلان داخل التطبيق", "يظهر لكل المستخدمين عند تفعيله") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(if (d.annEnabled) "الإعلان مفعَّل" else "الإعلان متوقف", style = MaterialTheme.typography.bodyLarge)
            Switch(checked = d.annEnabled, onCheckedChange = { d.annEnabled = it })
        }
        if (d.annEnabled) {
            Field(d.annAr, { d.annAr = it }, "نص الإعلان بالعربية")
            Field(d.annEn, { d.annEn = it }, "Announcement text in English", latin = true)
        }
    }
}

@Composable
private fun PromosSection(d: Draft) {
    SectionCard(
        "فترات المجّاني للجميع",
        "داخل الفترة: كل الميزات مفتوحة للجميع، وعدّاد المفاتيح المدفوعة يتوقف (يحسبه الخادم من الإعداد الموقّع). اليوم الأخير مشمول."
    ) {
        if (d.promos.isEmpty()) {
            Text(
                "لا فترات الآن. أضف واحدة فيصير التطبيق مجّانيًّا مؤقتًا بلا لمس المفاتيح المدفوعة.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        d.promos.forEachIndexed { index, p ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("فترة ${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { d.promos.removeAt(index) }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            }
            Field(p.from, { p.from = it }, "من (2026-10-14)", latin = true, single = true)
            Field(p.to, { p.to = it }, "إلى (2026-10-20) — اليوم الأخير مشمول", latin = true, single = true)
            Field(p.note, { p.note = it }, "ملاحظة تظهر للمستخدم (اختياري)")
        }
        OutlinedButton(
            onClick = {
                val today = LocalDate.now().toString()
                d.promos.add(PromoDraft().apply { from = today; to = today; note = "" })
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("إضافة فترة") }
    }
}

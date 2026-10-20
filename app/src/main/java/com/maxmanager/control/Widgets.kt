package com.maxmanager.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * مكوّنات اللوحة المشتركة — كانت كلها في `MainActivity.kt` الواحد، ونُقلت هنا مع تقسيم
 * الشاشة إلى تبويبات: الأقسام الأربعة صارت ملفات، وبقيت هذه الطبقة واحدة يشترك فيها الجميع.
 */

/** نبرة السطر الواحد: معلومة · نجاح · خطأ — واللون من مكان واحد. */
internal enum class Tone { Info, Ok, Error }

/** سطر الحالة العالمي أعلى الشاشة. */
internal data class Status(val text: String, val tone: Tone)

/** لون النجاح — لا يملكه MaterialTheme في اللوحة. */
internal val OkColor = Color(0xFF6FD99A)

/** النصّ اللاتيني (بريد، رابط، رقم إصدار) يبقى LTR داخل شاشة RTL. */
@Composable
internal fun ltr(): TextStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)

/** بطاقة قسم موحّدة: عنوان بلون primary + تلميح اختياري + محتوى بحشو موحّد. */
@Composable
internal fun SectionCard(title: String, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

/** بلاطة عدّاد: الرقم كبير والوصف تحته. */
@Composable
internal fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

/** سطر حالة ملوّن — يظهر في كل تبويب لأن الحالة عالمية لا خاصّة بقسم. */
@Composable
internal fun Banner(text: String, tone: Tone, loading: Boolean = false) {
    val color = when (tone) {
        Tone.Ok -> OkColor
        Tone.Error -> MaterialTheme.colorScheme.error
        Tone.Info -> MaterialTheme.colorScheme.primary
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.14f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = color)
            Spacer(Modifier.size(10.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

/** حقل نصّ موحّد. */
@Composable
internal fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    latin: Boolean = false,
    single: Boolean = false,
    minLines: Int = 1,
    keyboard: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = single,
        minLines = if (single) 1 else minLines,
        textStyle = if (latin) ltr() else LocalTextStyle.current,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth()
    )
}

/** وسم حالة صغير (سارٍ · منتهٍ · ملغى) — خلفية من لونه بنسبة خفيفة، بلا حدود. */
@Composable
internal fun Pill(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

internal fun countryName(code: String): String =
    if (code.length == 2 && code != "XX") Locale("", code).getDisplayCountry(Locale("ar")).ifBlank { code } else "غير معروف"

internal fun flag(code: String): String =
    if (code.length == 2 && code != "XX" && code.all { it in 'A'..'Z' }) {
        code.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    } else {
        "🌐"
    }

/** اسم المجال كما يفهمه المالك — والمفاتيح `cpu`… تبقى في المخطّط لا في الشاشة. */
internal fun domainLabel(domain: String): String = when (domain) {
    "cpu" -> "المعالج"
    "gpu" -> "الرسوميات"
    "memory" -> "الذاكرة"
    "storage" -> "التخزين"
    "thermal" -> "الحرارة"
    "audio" -> "الصوت"
    "display" -> "العرض"
    "touch" -> "اللمس"
    "charging" -> "الشحن"
    "spoof" -> "التنكّر"
    else -> "أخرى"
}

/** بيئة التحكّم كما تُعلنها الإشارة (§١٠.١) — والقيمة الخامّة هي المفتاح في القاعدة. */
internal fun envLabel(env: String): String = when (env) {
    "none" -> "بلا صلاحيات"
    "shizuku" -> "Shizuku"
    "magisk" -> "Magisk"
    "kernelsu" -> "KernelSU"
    "apatch" -> "APatch"
    else -> "جذر (أخرى)"
}

/** «2026-10-19T14:22:05…» ⇒ «2026-10-19 14:22» — والزمن زمن خادم لا زمن الجهاز. */
internal fun shortStamp(iso: String): String =
    if (iso.length < 16) iso else iso.take(16).replace('T', ' ')

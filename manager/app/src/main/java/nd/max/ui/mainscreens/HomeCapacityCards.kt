/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * بطاقات السعة في الرئيسية: **مصفوفة الذاكرة** ورموز وضع الوصول.
 *
 * أُخرجت من `LegendaryHomeDashboard.kt` لسقف حجم الملفّ (`tools/code_health.py`)، ولسبب
 * أصدق من العدد: هذان يقرآن **سعة** (كم بقي، أي طبقة امتياز تعمل)
 * بينما البقية في تلك الشاشة تقرأ **حالة آنية** (حرارة، تردّد، نشاط). والفصل يجعل الحدّ
 * مقروءًا: من عدّل مصفوفة الذاكرة لم يلمس موجة التردّد.
 *
 * **والنقل نقل لا إعادة تصميم:** النصوص والشروط والعتبات كما كانت حرفيًّا، والوظائف صارت
 * `internal` ليراها الملفّان، ولم يُغيَّر رقم واحد.
 */
package nd.max.ui.mainscreens

import nd.max.ui.design.MaxSize

import androidx.compose.material3.MaterialTheme

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.core.privilege.PrivilegeLevel
import nd.max.ui.component.NeuralCaption
import nd.max.ui.component.NeuralPalette
import nd.max.ui.component.NeuralPanel
import nd.max.ui.component.NeuralSectionHeader
import nd.max.ui.component.NeuralTile
import nd.max.ui.component.NeuralTrack
import nd.max.ui.component.NeuralValue
import nd.max.ui.component.neuralPalette
import nd.max.ui.design.MaxSpace
import nd.max.ui.navigation.MaxDestination
import nd.max.ui.theme.MonoValueStyleSmall
import nd.max.ui.viewmodel.DashboardState

/*
 * رموز طبقة الوصول — ثلاثة رموز لكل شيء، فلا نصّ محليّ مكتوب في الرسم.
 *
 * **والاسم من سجلّ الصلاحيات** (`PrivilegeLevel.labelRes`) لطبقتَي الجذر والشيزوكو، لأنهما
 * مسمّيتان في المستودع أصلًا؛ و«أساسي» رمز جديد لأن `PrivilegeLevel.NONE` عنوانه «بلا امتياز»
 * — وهو وصف لغياب لا اسم لوضع، والخطة تسمّي هذا الوضع `Basic` (§3.2 و§16.1).
 */
@StringRes
internal fun accessLabelRes(level: PrivilegeLevel): Int = when (level) {
    PrivilegeLevel.ROOT -> R.string.max_privilege_level_root
    PrivilegeLevel.SHIZUKU -> R.string.max_privilege_level_shizuku
    PrivilegeLevel.NONE -> R.string.home_access_basic
}

/** لون الشارة: الإيجابي للجذر، والأكسنت البديل لشيزوكو، والرمادي للأساسي. */
internal fun accessAccent(level: PrivilegeLevel, p: NeuralPalette) = when (level) {
    PrivilegeLevel.ROOT -> p.ok
    PrivilegeLevel.SHIZUKU -> p.accentAlt
    PrivilegeLevel.NONE -> p.muted
}

/**
 * مصفوفة الذاكرة — سعة RAM وZRAM والتخزين الداخلي في بطاقة واحدة.
 *
 * **والفعل ليس هنا:** كان للبطاقة صفّ Boost يُنفّذ ويقيس، ثم صار زرّ **Boost** مالكَه الوحيد في
 * `HomeActionRow` فوق — فبقاء الزرّ في الموضعين كان سيرسم فعلًا واحدًا مرّتين على شاشة واحدة.
 * وكل صفّ هنا بابٌ إلى شاشة مالكة (مركز الذاكرة · مدير ZRAM · تفصيل التخزين).
 * ونتيجة آخر Boost تُكتب تحت زرّها لا هنا.
 *
 * **ما هي وما ليست:** تعرض **حقائق سعة** (المستخدَم من الإجمالي، والمتاح)، ولا تُصدر حُكم ضغط.
 * وهذا ليس تحفّظًا شكليًّا: `ADR-34` يقرّر أن ضغط الذاكرة يُقاس بPSI لا بنسبة الامتلاء،
 * وأجهزة بنسبة امتلاء متقاربة تختلف في أثرها على الأداء اختلافًا كبيرًا. فالحُكم في هذه الشاشة
 * يبقى في `FocusCard` وحدها، وهذه البطاقة تجيب السؤال الآخر: «كم بقي؟».
 *
 * **ولذلك لا عتبات ولا ألوان إنذار هنا:** لون كلّ صفّ هوية (الأزرق/التركوا/الثانوي) لا حكم،
 * والمقارنة تكفلها الأشرطة والرقم المكتوب. ولون تحذير مستحدث هنا يعني عتبة امتلاء هي بالضبط
 * ما نهى عنه ADR-34 — والقارئ ينسى أن العتبة أُضيفت في الواجهة.
 *
 * **والمصادر أوعية موجودة، لا أوعية جديدة:** الأرقام من نفس لقطة اللوحة، والإجراءات إلى
 * الشاشتين المالكين للرقم (`ZramManager` · `StorageDetail`) — ولهذا صار كل صفّ قابلًا للنقر
 * بذاته: نقر بطاقة كاملة كان سيوصل صفّ التخزين إلى شاشة الذاكرة، وهي كذبة صغيرة.
 *
 * **وما لم يُقرأ لا يُصاغ:** غياب التبديل أو إجمالي الذاكرة يُكتب نصًّا («غير متاح»)
 * وبلا شريط، لا أحد عشرًا صفرًا ولا شريطًا فارغًا يُقرأ كـ«فارغ».
 */
@Composable
internal fun MemoryMatrixCard(
    dashboard: DashboardState,
    onNavigate: (String) -> Unit,
) {
    val p = neuralPalette()
    val ramTotal = dashboard.ramTotalMb
    val ramUsed = dashboard.ramUsedMb
    val swapTotal = dashboard.swapTotalMb
    val swapUsed = dashboard.swapUsedMb

    NeuralPanel(accent = p.accent) {
        NeuralSectionHeader(
            title = stringResource(R.string.home_memory_storage),
            caption = stringResource(R.string.home_memory_storage_desc),
            accent = p.accent,
        )

        MemoryFactRow(
            label = stringResource(R.string.ram_label),
            detail = if (ramTotal > 0) "${gigabytes(ramUsed)} / ${gigabytes(ramTotal)}" else null,
            status = if (ramTotal > 0) {
                stringResource(R.string.home_available_memory, gigabytes(ramTotal - ramUsed))
            } else {
                stringResource(R.string.max_home_unavailable)
            },
            fraction = if (ramTotal > 0) fractionOf(ramUsed, ramTotal) else null,
            accent = p.accent,
            // صفّ RAM كان يفتح **مدير ZRAM** — عطب مقصود (نسخ الصفّ المجاور) لا خيار: من
            // يضغط «RAM» يسأل عن الذاكرة العشوائية، ومدير ZRAM شاشةٌ أخرى. الصحيح مركز
            // الذاكرة (`MemoryHub`) الذي يضمّ RAM وZRAM معًا؛ وصفّ ZRAM تحت يبقى على مديره.
            onClick = { onNavigate(MaxDestination.MemoryHub.route) },
        )

        MemoryFactRow(
            label = stringResource(R.string.home_memory_swap_label),
            detail = if (swapTotal != null && swapTotal > 0 && swapUsed != null) {
                "${gigabytes(swapUsed)} / ${gigabytes(swapTotal)}"
            } else {
                null
            },
            status = if (swapTotal != null && swapTotal > 0 && swapUsed != null) {
                stringResource(R.string.home_available_swap, gigabytes(swapTotal - swapUsed))
            } else {
                stringResource(R.string.home_zram_unavailable)
            },
            fraction = if (swapTotal != null && swapTotal > 0 && swapUsed != null) {
                fractionOf(swapUsed, swapTotal)
            } else {
                null
            },
            accent = p.accentAlt,
            onClick = { onNavigate(MaxDestination.ZramManager.route) },
        )

        val storageUsed = dashboard.storageUsedGb
        val storageTotal = dashboard.storageTotalGb
        val storageFree = (storageTotal - storageUsed).coerceAtLeast(0f)

        MemoryFactRow(
            label = stringResource(R.string.home_internal_storage),
            detail = if (storageTotal > 0f) "${storageUsed.oneDecimal()} GB / ${storageTotal.oneDecimal()} GB" else null,
            status = if (storageTotal > 0f) {
                stringResource(R.string.home_available_storage, storageFree.oneDecimal())
            } else {
                stringResource(R.string.max_home_unavailable)
            },
            fraction = if (storageTotal > 0f) (storageUsed / storageTotal).coerceIn(0f, 1f) else null,
            accent = p.ok,
            onClick = { onNavigate(MaxDestination.StorageDetail.route) },
        )
    }
}

/**
 * صفّ سعة واحد: اسم الوعاء · المستخدَم/الإجمالي · المتاح · شريط.
 *
 * والمتاح هو السطر الأبرز لأنه سؤال المستخدم فعلًا («كم بقي؟»)، والمستخدَم/الإجمالي يبقى
 * بجانب الاسم لأن بدون إجمالي لا يُقرأ المتاح على أنه كثير أو قليل.
 */
@Composable
private fun MemoryFactRow(
    label: String,
    detail: String?,
    status: String,
    fraction: Float?,
    accent: Color,
    onClick: () -> Unit
) {
    val p = neuralPalette()
    NeuralTile(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        verticalSpacing = MaxSpace.xs,
        contentPadding = PaddingValues(
            horizontal = MaxSpace.rowPaddingHorizontal,
            vertical = MaxSpace.rowPaddingVertical,
        ),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(MaxSpace.sm).clip(RoundedCornerShape(50)).background(accent))
                Spacer(Modifier.width(MaxSpace.sm))
                NeuralCaption(label)
            }
            if (detail != null) {
                NeuralValue(detail, style = MonoValueStyleSmall, color = p.text)
            }
            // السهم في نهاية الصفّ: هذا الصفّ بابٌ إلى شاشته، لا بيان.
            Spacer(Modifier.width(MaxSpace.xs))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                Modifier.size(MaxSize.iconGlyphSmall),
                tint = accent,
            )
        }
        Text(status, color = accent, style = MaterialTheme.typography.bodySmall)
        fraction?.let { NeuralTrack(it, accent.copy(alpha = .85f), height = MaxSize.barHeight) }
    }
}

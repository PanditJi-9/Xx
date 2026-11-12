/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * لغة الحكّام — عرضٌ لا حكم.
 *
 * **العطب الذي وُلد منه الملفّ (تشخيص UX §٢):** مَن يملك مفتاح العتاد كان يُعرَف من
 * `DiagnosticsScreen` وحدها: عنوانٌ إنجليزي مثبّت، و`lease.key` و`ownerToken` خامّان،
 * وبطاقة مدفونة خلف شاشتين. فالمستخدم يفتح مقبضًا، يُرفض، ولا يعرف أن ملف تطبيق أو
 * الحماية الحرارية منعه.
 *
 * **وحدّه المُعلن:** لا يقرأ هذا الملفّ عتادًا ولا يستدعي المراجِع ولا يغيّر مالكًا.
 * `MaxGovernanceLadder` سلسلة أولوية **ثابتة** من `ControlOwnership.Owner` (مصدر واحد)،
 * و`MaxGovernanceCard` تعرض ما يمرّره صاحبها من `ControlOwnership.snapshot()`.
 * ولهذا لا «مؤقّت تحديث» هنا: بطاقة التشخيص تُفتح فتُقرأ لحظتها، والسلّم لا يتقادم.
 *
 * **ولا رمز آلي في واجهة المستخدم:** `ownerToken` لا يُطبع. و`lease.key` يبقى في بطاقة
 * التشخيص وحدها لأنها سطح فحص (كود الخريطة في `RouteVerdictLabel` نفسه)، لا شاشة إعداد.
 *
 * **وبلا مستدعٍ بقرار مالك (تكملة ٨٥):** `OwnerDecisionsGuardTest` يمنع عودة `MaxGovernorStrip`
 * و`MaxGovernanceLadder` إلى «الآن» و«التحكّم»، ويمنع `MaxGovernanceCard` في الإعدادات والتحكّم،
 * وشاشة التشخيص التي كانت سطحها حُذفت بأمر المالك. فالملفّ محفوظ مرجعًا للسلّم، لا يُعرض ولا يُوصَل
 * بلا أمر جديد — وحذفه بلا أمر يخالف «احذف فقط ما اخبرتك ان تحذفه».
 */
package nd.max.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.annotation.StringRes
import nd.max.R
import nd.max.core.hardware.ControlOwnership
import nd.max.ui.theme.MonoFontFamily

/**
 * سلّم الأولوية من الأعلى إلى الأدنى — مرتّب على `priority` الأصلي في `ControlOwnership`.
 *
 * ولا يُشتقّ هنا بـ`sortedByDescending`: الترتيب مقروءٌ بالعين في هذا الموضع، وتغييره في
 * السجلّ يجب أن يُقرأ في هذا السطر لا أن يقع صامتًا عند التركيب.
 *
 * `internal` لأن اختبار JVM يقيسه مقابل `priority` (انظر `MaxGovernanceOrderTest`).
 */
internal val maxGovernanceLadder: List<ControlOwnership.Owner> = listOf(
    ControlOwnership.Owner.RECOVERY,
    ControlOwnership.Owner.SAFETY,
    ControlOwnership.Owner.PER_APP,
    ControlOwnership.Owner.MAX_AI,
    ControlOwnership.Owner.GLOBAL_PROFILE,
    ControlOwnership.Owner.SYSTEM,
)

@StringRes
private fun ownerLabelRes(owner: ControlOwnership.Owner): Int = when (owner) {
    ControlOwnership.Owner.RECOVERY -> R.string.max_gov_owner_recovery
    ControlOwnership.Owner.SAFETY -> R.string.max_gov_owner_safety
    ControlOwnership.Owner.PER_APP -> R.string.max_gov_owner_per_app
    ControlOwnership.Owner.MAX_AI -> R.string.max_gov_owner_max_ai
    ControlOwnership.Owner.GLOBAL_PROFILE -> R.string.max_gov_owner_global
    ControlOwnership.Owner.SYSTEM -> R.string.max_gov_owner_system
}

/** اسم الحاكم بلغة بشرية — الموضع الوحيد الذي يُترجم فيه هذا الاسم. */
@Composable
fun maxOwnerLabel(owner: ControlOwnership.Owner): String = stringResource(ownerLabelRes(owner))

/**
 * نغمة الحاكم. الحماية والاسترداد خطرٌ (لأنهما يتجاوزان اختيار المستخدم)، والملف وMAX AI
 * تمييز، واليدوي حياد، والنظام خامل. واللون لا يحمل المعنى وحده: الشارة تحمل النص دائمًا.
 */
private fun ownerTone(owner: ControlOwnership.Owner): MaxTone = when (owner) {
    ControlOwnership.Owner.RECOVERY,
    ControlOwnership.Owner.SAFETY -> MaxTone.Critical

    ControlOwnership.Owner.PER_APP,
    ControlOwnership.Owner.MAX_AI -> MaxTone.Accent

    ControlOwnership.Owner.GLOBAL_PROFILE -> MaxTone.Neutral
    ControlOwnership.Owner.SYSTEM -> MaxTone.Inactive
}

/** شارة اسم الحاكم: نصّ + حدّ + خلفية نغمة، وليست لونًا مجرّدًا. */
@Composable
fun MaxOwnerBadge(owner: ControlOwnership.Owner, modifier: Modifier = Modifier) {
    val tone = ownerTone(owner)
    val label = maxOwnerLabel(owner)
    val description = stringResource(R.string.max_gov_owner_a11y, label)
    Surface(
        // `clearAndSetSemantics` لا `semantics`: الشارة عقدة واحدة تُقرأ بجملة واحدة، فلا
        // يقرأ TalkBack الاسم مرتين (نصّ الشارة + الوصف) ولا يسقط الوصف حين يُدمج النصّ.
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = RoundedCornerShape(MaxRadius.pill),
        color = tone.container(),
        border = BorderStroke(MaxSize.hairlineBorder, tone.border()),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tone.content(),
            modifier = Modifier.padding(horizontal = MaxSpace.sm, vertical = MaxSpace.xs),
        )
    }
}

/**
 * سلّم الحكّام في صفحة التحكّم: ثلاث جمل قصيرة ورابط.
 *
 * لماذا هنا لا في «الآن»: القرار اليدوي يُتخذ في التحكّم، وهذا السطر يجيب السؤال قبل اللمس.
 * ولماذا ليس لوحة أرقام: السلّم **سياسة ثابتة**، لا حالة لحظية؛ ولذلك لا يدّعي أنه يعرض
 * «الحاكم الآن». من يملك المفتاح فعلًا يُقرأ في التشخيص لحظة فتحه.
 */
@Composable
fun MaxGovernanceLadder(
    modifier: Modifier = Modifier,
    onOpenDetails: (() -> Unit)? = null,
) {
    val chain = maxGovernanceLadder.map { stringResource(ownerLabelRes(it)) }
        .joinToString(separator = stringResource(R.string.max_gov_separator))
    MaxCardShell(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpenDetails,
        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
        ) {
            Icon(
                imageVector = Icons.Rounded.Gavel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MaxSize.iconGlyph),
            )
            Text(
                text = stringResource(R.string.max_gov_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = chain,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.max_gov_order_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onOpenDetails != null) {
            Text(
                text = stringResource(R.string.max_gov_open_details),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * أعلى حاكم نشط الآن — دالّة نقيّة تُقاس في `MaxGovernanceOrderTest` بدل أن تُختبَر بالعين.
 *
 * ولماذا `maxByOrNull { priority }` لا أوّل عنصر: `snapshot()` تعيد خريطة مرتّبة بمفاتيحها لا
 * بأولوياتها، فالأولوية وحدها هي الحكم — كما يفعل الـarbiter عند التنازع.
 */
internal fun maxGovernorTop(leases: List<ControlOwnership.Lease>): ControlOwnership.Owner? =
    leases.maxByOrNull { it.owner.priority }?.owner

/**
 * شريط «من يحكم الآن» على شاشة البداية — سطر واحد قبل البطل.
 *
 * وهو **حالة لحظية** لا سياسة: يقول فقط ما في `ControlOwnership.snapshot()` التي مرّرها صاحب
 * الشاشة. ولا يقرأ عتادًا ولا يستقصي بنفسه؛ والرئيسية تُعاد بناؤها مع كل دورة قراءة، فتتجدّد
 * القيمة معها بلا مؤقّت جديد. ولمن يملك المفتاح فعلًا: الضغط يفتح بطاقة الحكّام في التشخيص.
 *
 * **وحدّ التجدّد معلن:** لا مؤقّت هنا، فالتحديث يتبع دورة قراءة اللوحة (٢ ثانية أثناء ظهور
 * «الآن»). وإن توقّفت *كل* قيم اللوحة عن التغيّر (جهاز ساكن تمامًا) فقد يتأخّر حتى تتغيّر —
 * وهي حالٌ نادرة، وأصدق من مؤقّت يوحي بمراقبة مستمرة لا وجود لها.
 */
@Composable
fun MaxGovernorStrip(
    leases: List<ControlOwnership.Lease>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = maxGovernorTop(leases)
    val summary = when {
        top == null -> stringResource(R.string.max_gov_now_none)
        leases.size == 1 -> stringResource(R.string.max_gov_now_one, maxOwnerLabel(top))
        else -> stringResource(R.string.max_gov_now_many, maxOwnerLabel(top), leases.size)
    }
    val tone = top?.let(::ownerTone) ?: MaxTone.Neutral
    MaxCardShell(
        modifier = modifier.fillMaxWidth(),
        onClick = onOpen,
        contentPadding = MaxSpace.md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.md),
        ) {
            Icon(
                imageVector = Icons.Rounded.Gavel,
                contentDescription = null,
                tint = tone.content(),
                modifier = Modifier.size(MaxSize.iconGlyph),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
            ) {
                Text(
                    text = stringResource(R.string.max_gov_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MaxSize.iconGlyph),
            )
        }
    }
}

/**
 * الحكّام النشطون — تُغذّى من `ControlOwnership.snapshot()` كما هي.
 *
 * `lease.key` يبقى ظاهرًا (سطح فحص)، و`desired` يُقرأ بجانبه. أما `ownerToken` فلا يُطبع:
 * هو معرّف داخلي لا يضيف للمستخدم شيئًا، ووجوده في اللقطة كان من مفردات المطوّر.
 */
@Composable
fun MaxGovernanceCard(
    leases: List<ControlOwnership.Lease>,
    modifier: Modifier = Modifier,
) {
    MaxCardShell(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
    ) {
        Text(
            text = stringResource(R.string.max_gov_active_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = if (leases.isEmpty()) {
                stringResource(R.string.max_gov_none)
            } else {
                stringResource(R.string.max_gov_count, leases.size)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        leases.forEach { lease ->
            Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
                ) {
                    MaxOwnerBadge(owner = lease.owner)
                    Text(
                        text = lease.key,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = MonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.max_gov_requested, lease.desired),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

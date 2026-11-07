/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import nd.max.R
import nd.max.core.hardware.RecipePlan
import nd.max.core.hardware.RecipeRun
import nd.max.core.policy.DangerConsent
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.design.maxRadius
import nd.max.ui.design.maxSpace
import nd.max.ui.viewmodel.RecipeViewModel

/**
 * سطح الوصفة: نصّ ← مراجعة ← تطبيق، مع حكم كل سطر قبل أي كتابة.
 *
 * **ولماذا قسم لا شاشة:** لا يُضاف `MaxDestination` جديد (٦٨ قائمة)، والسطح هنا وظيفة داخل
 * شاشة متقدّمة لا عالمٌ قائم بذاته. و[MaxSection] هو وحدة التجميع في التطبيق كلّه، فيرث
 * كشف الحركة والوصف والطيّ بلا سطر إضافيّ.
 *
 * **وثلاث حالات لا تُخلَط:**
 * 1. **المراجعة** حكمٌ بنيويّ على النصّ وحده (مفتاح معروف؟ مسار حرفيّ؟ صلاحية كتابة؟ قيمة من النوع؟)
 *    ولا تلمس عقدة.
 * 2. **التطبيق** مقفل بإقرار خطر مرّة واحدة (ADR-90) ثم يمرّ بالـarbiter سطرًا سطرًا.
 * 3. **التقرير** نتيجة الجهاز: قُرئت، أو اختلفت فاستُعيدت، أو رُفضت فتُراجَع السابقة بالعكس.
 *
 * ومن هنا: زرّ التطبيق لا يظهر إلا لخطة **قابلة للتطبيق بالكامل** — نصف وصفة حالةٌ لم يخترها أحد.
 */
@Composable
fun RecipeCard(viewModel: RecipeViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { viewModel.load() }

    val plan = viewModel.plan
    val report = viewModel.report
    val summary = when {
        plan == null -> stringResource(R.string.recipe_summary_empty)
        plan.applicable -> stringResource(R.string.recipe_summary_ready, plan.ready.size, plan.steps.size)
        else -> stringResource(R.string.recipe_summary_blocked, plan.blocked.size, plan.steps.size)
    }
    val applyRecipe = rememberDangerGate(
        id = DangerConsent.RECIPE_APPLY,
        title = stringResource(R.string.recipe_danger_title),
        message = stringResource(R.string.recipe_danger_body),
        confirmLabel = stringResource(R.string.recipe_danger_confirm),
    ) { viewModel.apply() }

    MaxSection(
        title = stringResource(R.string.recipe_section_title),
        description = stringResource(R.string.recipe_section_desc),
        collapsible = true,
        summary = summary,
    ) {
        MaxGroup {
            Column(
                modifier = Modifier.padding(
                    horizontal = MaxSpace.rowPaddingHorizontal,
                    vertical = maxSpace(MaxSpace.rowPaddingVertical),
                ),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                OutlinedTextField(
                    value = viewModel.text,
                    onValueChange = viewModel::edit,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.recipe_input_label)) },
                    placeholder = { Text(stringResource(R.string.recipe_input_placeholder)) },
                    // خطّ ثابت العرض: المسارات والأرقام تُقارن عموديًّا، والخطّ النسبيّ يزحزحها.
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    shape = RoundedCornerShape(maxRadius(MaxRadius.row)),
                    minLines = 4,
                    maxLines = 8,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm)) {
                    StudioTonalButton(onClick = viewModel::review) {
                        Text(stringResource(R.string.recipe_review))
                    }
                    StudioOutlinedButton(onClick = { viewModel.edit("") }) {
                        Text(stringResource(R.string.recipe_clear))
                    }
                }
                if (viewModel.text.isBlank()) {
                    Text(
                        text = stringResource(R.string.recipe_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (viewModel.catalogLoaded && viewModel.catalog.isEmpty()) {
                    Text(
                        text = stringResource(R.string.recipe_catalog_missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                // ما لم يُفهم لا يُذكر كأنه سيُطبَّق: الأسطر التي ليست `key=value` تُسقَط من الخطة،
                // فتُعدّ هنا وتُعلَن — وإلا قال «٣ أسطر جاهزة» عن نصّ فيه خمسة.
                val ignored = if (plan != null) RecipePlan.ignored(viewModel.text) else 0
                if (ignored > 0) {
                    Text(
                        text = stringResource(R.string.recipe_lines_ignored, ignored),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        if (plan != null && plan.steps.isNotEmpty()) {
            MaxGroup {
                plan.steps.forEachIndexed { index, step ->
                    if (index > 0) MaxGroupDivider()
                    VerdictRow(step)
                }
            }
        }

        if (plan != null && plan.applicable && report == null) {
            StudioTonalButton(
                onClick = applyRecipe,
                enabled = !viewModel.running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (viewModel.running) R.string.recipe_running else R.string.recipe_apply,
                    ),
                )
            }
        }

        if (report != null) {
            val tone = if (report.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Text(
                text = if (report.ok) {
                    stringResource(R.string.recipe_result_ok, report.applied)
                } else {
                    stringResource(
                        R.string.recipe_result_failed,
                        report.applied,
                        report.rolledBack,
                        report.failed,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tone,
            )
            MaxGroup {
                report.results.forEachIndexed { index, result ->
                    if (index > 0) MaxGroupDivider()
                    OutcomeRow(result)
                }
            }
        }
    }
}

/** سطر مراجعة: النصّ كما كُتب، والمسار الذي حُسم، والحكم. */
@Composable
private fun VerdictRow(step: RecipePlan.Step) {
    MaxRow(
        title = "${step.key} = ${step.value}",
        subtitle = step.path,
        icon = verdictIcon(step.verdict),
        iconTone = verdictTone(step.verdict),
        trailing = {
            Text(
                text = stringResource(verdictLabel(step.verdict)),
                style = MaterialTheme.typography.labelMedium,
                color = verdictColor(step.verdict),
            )
        },
    )
}

/** سطر نتيجة: ما فعله الجهاز فعلًا، لا ما توقّعناه. */
@Composable
private fun OutcomeRow(result: RecipeRun.StepResult) {
    MaxRow(
        title = "${result.step.key} = ${result.step.value}",
        subtitle = stringResource(outcomeLabel(result.outcome)),
        icon = outcomeIcon(result.outcome),
        iconTone = outcomeTone(result.outcome),
    )
}

private fun verdictLabel(verdict: RecipePlan.Verdict): Int = when (verdict) {
    RecipePlan.Verdict.READY -> R.string.recipe_verdict_ready
    RecipePlan.Verdict.UNKNOWN_KEY -> R.string.recipe_verdict_unknown_key
    RecipePlan.Verdict.NO_NODE -> R.string.recipe_verdict_no_node
    RecipePlan.Verdict.NOT_WRITABLE -> R.string.recipe_verdict_not_writable
    RecipePlan.Verdict.PROTECTED -> R.string.recipe_verdict_protected
    RecipePlan.Verdict.BAD_VALUE -> R.string.recipe_verdict_bad_value
    RecipePlan.Verdict.TEMPLATED -> R.string.recipe_verdict_templated
}

private fun verdictIcon(verdict: RecipePlan.Verdict): ImageVector = when (verdict) {
    RecipePlan.Verdict.READY -> Icons.Rounded.CheckCircle
    RecipePlan.Verdict.UNKNOWN_KEY -> Icons.Rounded.HelpOutline
    RecipePlan.Verdict.NO_NODE -> Icons.Rounded.ErrorOutline
    RecipePlan.Verdict.NOT_WRITABLE -> Icons.Rounded.Lock
    RecipePlan.Verdict.PROTECTED -> Icons.Rounded.Lock
    RecipePlan.Verdict.BAD_VALUE -> Icons.Rounded.ErrorOutline
    RecipePlan.Verdict.TEMPLATED -> Icons.Rounded.Tune
}

private fun verdictTone(verdict: RecipePlan.Verdict): MaxTone = when (verdict) {
    RecipePlan.Verdict.READY -> MaxTone.Positive
    RecipePlan.Verdict.UNKNOWN_KEY -> MaxTone.Neutral
    RecipePlan.Verdict.NO_NODE -> MaxTone.Critical
    RecipePlan.Verdict.NOT_WRITABLE -> MaxTone.Inactive
    RecipePlan.Verdict.PROTECTED -> MaxTone.Inactive
    RecipePlan.Verdict.BAD_VALUE -> MaxTone.Critical
    RecipePlan.Verdict.TEMPLATED -> MaxTone.Caution
}

/**
 * لون حكم السطر. `READY` يأخذ لون الشاشة (فعلٌ مشروع)، والقالبيّ لون «يحتاج قرارًا»،
 * والمرفوض لون الخطأ — والقراءة المحجوبة (محميّة/غير قابلة للكتابة) لا تُلوَّن بالأحمر لأنها
 * ليست عطبًا في الوصفة بل حكمٌ ثابت في الكتالوج.
 */
@Composable
private fun verdictColor(verdict: RecipePlan.Verdict): Color = when (verdict) {
    RecipePlan.Verdict.READY -> MaterialTheme.colorScheme.primary
    RecipePlan.Verdict.TEMPLATED -> MaterialTheme.colorScheme.tertiary
    RecipePlan.Verdict.NOT_WRITABLE, RecipePlan.Verdict.PROTECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
}

private fun outcomeLabel(outcome: RecipeRun.Outcome): Int = when (outcome) {
    RecipeRun.Outcome.APPLIED -> R.string.recipe_outcome_applied
    RecipeRun.Outcome.MISMATCH_RESTORED -> R.string.recipe_outcome_mismatch_restored
    RecipeRun.Outcome.RESTORE_FAILED -> R.string.recipe_outcome_restore_failed
    RecipeRun.Outcome.WRITE_FAILED -> R.string.recipe_outcome_write_failed
    RecipeRun.Outcome.ROLLED_BACK -> R.string.recipe_outcome_rolled_back
    RecipeRun.Outcome.ROLLBACK_FAILED -> R.string.recipe_outcome_rollback_failed
    RecipeRun.Outcome.BLOCKED -> R.string.recipe_outcome_blocked
}

private fun outcomeIcon(outcome: RecipeRun.Outcome): ImageVector = when (outcome) {
    RecipeRun.Outcome.APPLIED -> Icons.Rounded.CheckCircle
    RecipeRun.Outcome.MISMATCH_RESTORED -> Icons.Rounded.Restore
    RecipeRun.Outcome.RESTORE_FAILED -> Icons.Rounded.ErrorOutline
    RecipeRun.Outcome.WRITE_FAILED -> Icons.Rounded.ErrorOutline
    RecipeRun.Outcome.ROLLED_BACK -> Icons.Rounded.Undo
    RecipeRun.Outcome.ROLLBACK_FAILED -> Icons.Rounded.ErrorOutline
    RecipeRun.Outcome.BLOCKED -> Icons.Rounded.Block
}

private fun outcomeTone(outcome: RecipeRun.Outcome): MaxTone = when (outcome) {
    RecipeRun.Outcome.APPLIED -> MaxTone.Positive
    RecipeRun.Outcome.MISMATCH_RESTORED -> MaxTone.Caution
    RecipeRun.Outcome.ROLLED_BACK -> MaxTone.Caution
    RecipeRun.Outcome.BLOCKED -> MaxTone.Inactive
    else -> MaxTone.Critical
}

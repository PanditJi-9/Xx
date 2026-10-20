/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.license

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import nd.max.R
import nd.max.core.access.PromoState
import nd.max.core.access.RemoteAccess
import nd.max.core.remote.RemoteFlags
import nd.max.ui.design.MaxBullets
import nd.max.ui.design.MaxCapsule
import nd.max.ui.design.MaxCausalStage
import nd.max.ui.design.MaxCondition
import nd.max.ui.design.MaxConditionKind
import nd.max.ui.design.MaxConditionNotice
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxTone
import nd.max.ui.navigation.MaxDestination
import java.util.Date

/**
 * **شاشة «ما يفعله Max AI»** (§٥): تعريفُ المحرّك بحلقته، وجدولُ ما هو **متاح الآن فعلًا**،
 * وسطورُ ما لا نفعله. ومن هنا يجد من لا يملك مفتاحًا سببًا واضحًا للاكتشاف — لا وعدًا بيعيًّا.
 *
 * ## ومصادرها — كلّها حيّة، ولا رقم واحد مكتوب بيد
 *
 * 1. **الحلقة**: عنوانان لكل مرحلة من مراحل النموذج القائمة (`MaxCausalStage`) — تصف ما يفعله
 *    المحرّك بالترتيب، وتُقرأ من نصوص المواردة لا من محفور.
 * 2. **جدول الأسطح السبعة**: معرّفاته من [RemoteFlags] (سجلّ المفاتيح — لا نسخة ثانية له)،
 *    وأسماؤه من موارد الشاشات القائمة (نفس الاسم الذي يراه المستخدم في مكانه)، وحالة كل صفٍّ
 *    من `RemoteAccess.access` **الآن** — تُترجم بـ[PromoState] بلا حكم ثانٍ. والصفوف هي
 *    `RemoteFlags.ALL.filter { it.showInMaxAiIntro }`؛ فمفتاح [RemoteFlags.LICENSE] يحكم
 *    ظهور صفٍّ في الإعدادات لا سطحَ قدرة، فلا يُقرأ هنا كأنه ميزة.
 * 3. **الإعلان** (مجّانيّ حتى تاريخ): من نافذة المالك في اللقطة نفسها، وبالتاريخ نفسه الذي
 *    تعرضه شاشة الترخيص (`DateFormat` الوسط) — مصدرٌ واحد لا نسختان تتباعدان.
 *
 * ## وما لا تدّعيه
 *
 * لا نسبة تحسّن هنا، ولا «تسريع» بلا قياس، ولا صفُّ سطحٍ لم يُقرأ حكمه من السجلّ. والمقيس في
 * الاختبار: تطابق الجدول مع السجلّ في الاتجاهين، وتغطية مفردات الحالة، ووجود النصوص بلغتين،
 * ومسارُ الوصول (مفتوحٌ لكلٍّ حتى وهو مقفل — فالغرض أن يُعرَض لا أن يُخفى).
 *
 * **والحدّ المُعلَن:** «هل الشاشة مقنعةٌ وجميلة؟» — UAT على جهاز؛ ولا يُدّعى هنا ذوقٌ ولا بكسل.
 */
@Composable
fun MaxAiIntroScreen(navController: NavHostController) {
    val snapshot by RemoteAccess.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val access = remember(snapshot) {
        introSurfaces.associate { surface ->
            surface.flagId to PromoState.of(RemoteAccess.access(surface.flagId))
        }
    }

    MaxScreen(
        title = stringResource(R.string.maxai_intro_title),
        subtitle = stringResource(R.string.maxai_intro_subtitle),
        onBack = { navController.popBackStack() },
        accentIcon = Icons.Rounded.Insights,
        accent = MaterialTheme.colorScheme.tertiary,
    ) {
        val promo = snapshot.promo
        if (promo != null) {
            val until = remember(promo.toMillis) {
                android.text.format.DateFormat.getMediumDateFormat(context)
                    .format(Date(promo.toMillis))
            }
            MaxConditionNotice(
                condition = MaxCondition(
                    kind = MaxConditionKind.Applied,
                    title = stringResource(R.string.maxai_intro_promo_title, until),
                    detail = stringResource(R.string.maxai_intro_promo_detail),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        MaxSection(
            title = stringResource(R.string.maxai_intro_loop_title),
            description = stringResource(R.string.maxai_intro_loop_desc),
        ) {
            introStages.forEachIndexed { index, stage ->
                MaxCausalStage(
                    order = index + 1,
                    title = stringResource(stage.titleRes),
                    body = stringResource(stage.bodyRes),
                    tone = stage.tone,
                    isLast = index == introStages.lastIndex,
                )
            }
        }

        MaxSection(
            title = stringResource(R.string.maxai_intro_access_title),
            description = stringResource(R.string.maxai_intro_access_desc),
        ) {
            MaxGroup {
                introSurfaces.forEach { surface ->
                    val state = access[surface.flagId] ?: PromoState.OFF
                    MaxRow(
                        title = stringResource(surface.nameRes),
                        trailing = {
                            MaxCapsule(
                                text = stringResource(state.labelRes()),
                                tone = state.tone(),
                            )
                        },
                    )
                }
            }
        }

        MaxSection(title = stringResource(R.string.maxai_intro_honesty_title)) {
            MaxBullets(
                lines = listOf(
                    stringResource(R.string.maxai_intro_honest_measured),
                    stringResource(R.string.maxai_intro_honest_readback),
                    stringResource(R.string.maxai_intro_honest_unknown),
                    stringResource(R.string.maxai_intro_honest_hardware),
                ),
            )
        }

        MaxGroup {
            MaxNavigationRow(
                title = stringResource(R.string.maxai_intro_license_row),
                subtitle = stringResource(R.string.maxai_intro_license_sub),
                icon = Icons.Rounded.Key,
                iconTone = MaxTone.Accent,
                onClick = { navController.navigate(MaxDestination.License.launchRoute) },
            )
        }
    }
}

/** مرحلة في حلقة المحرّك: عنوانٌ وسطرُ شرح ونبرةُ رسم — والأرقام يرسمها [MaxCausalStage]. */
internal data class IntroStage(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val tone: MaxTone,
)

/**
 * الحلقة في أربع مراحل — بترتيب المحرّك نفسه: قراءةٌ ثم تشخيصٌ ثم تنفيذٌ مقيَّد ثم تحقُّقٌ
 * بالقراءة، ومن التحقُّق تعود القراءة. ولا مرحلة خامسة: ما لا يفعله المحرّك لا يُكتَب هنا.
 */
internal val introStages = listOf(
    IntroStage(R.string.maxai_intro_stage1_title, R.string.maxai_intro_stage1_body, MaxTone.Neutral),
    IntroStage(R.string.maxai_intro_stage2_title, R.string.maxai_intro_stage2_body, MaxTone.Accent),
    IntroStage(R.string.maxai_intro_stage3_title, R.string.maxai_intro_stage3_body, MaxTone.Caution),
    IntroStage(R.string.maxai_intro_stage4_title, R.string.maxai_intro_stage4_body, MaxTone.Positive),
)

/** سطحٌ يُعرض في جدول «المتاح الآن»: معرّفه من [RemoteFlags] واسمه من مورد قائم. */
internal data class IntroSurface(
    val flagId: String,
    @StringRes val nameRes: Int,
)

/**
 * الأسطح السبعة — **واحدًا واحدًا كما في [RemoteFlags.ALL] بعد استبعاد ما ليس سطح قدرة**:
 * والاختبار يقيس التطابق في الاتجاهين، فلا يسقط سطحٌ من الجدول ولا يُخترع صفٌّ بلا مفتاح.
 */
internal val introSurfaces = listOf(
    IntroSurface(RemoteFlags.MAXAI, R.string.max_nav_max_ai),
    IntroSurface(RemoteFlags.AUDIO, R.string.max_hub_audio),
    IntroSurface(RemoteFlags.GAMESPACE, R.string.game_lobby_title),
    IntroSurface(RemoteFlags.ATLAS, R.string.max_atlas_section_title),
    IntroSurface(RemoteFlags.SPOOF, R.string.spoof_title),
    IntroSurface(RemoteFlags.EMUHUB, R.string.emu_hub_title),
    IntroSurface(RemoteFlags.BYPASS_CHARGE, R.string.bcharging),
)

/** نبرة الشارة: «متاح» مؤكَّد، و«مجّانيّ الآن» لون الشاشة، و«يحتاج مفتاحًا» تنبيهٌ هادئ. */
private fun PromoState.tone(): MaxTone = when (this) {
    PromoState.AVAILABLE -> MaxTone.Positive
    PromoState.FREE_NOW -> MaxTone.Accent
    PromoState.NEEDS_KEY -> MaxTone.Caution
    PromoState.OFF -> MaxTone.Neutral
}

/** نصّ الشارة — مفردات الإعلان لا مفردات الحكم. */
@StringRes
private fun PromoState.labelRes(): Int = when (this) {
    PromoState.AVAILABLE -> R.string.maxai_intro_state_available
    PromoState.FREE_NOW -> R.string.maxai_intro_state_free
    PromoState.NEEDS_KEY -> R.string.maxai_intro_state_key
    PromoState.OFF -> R.string.maxai_intro_state_off
}

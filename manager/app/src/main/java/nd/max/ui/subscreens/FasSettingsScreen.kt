/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.subscreens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.PerAppRefreshRateController
import nd.max.core.gamespace.GameProfileRepository
import nd.max.R
import nd.max.ui.component.ScreenAccentProvider
import nd.max.ui.design.MaxBullets
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxNavigationRow
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxTone
import nd.max.ui.navigation.MaxDestination

/**
 * إعدادات جدولة FAS (`frame-aware scheduling`) — صفحة حقائق لا صفحة وعد.
 *
 * **ولماذا لم تبقَ شرحًا فارغًا:** كانت الشاشة عنوانًا ونصًّا لا يفعل شيئًا، بينما الجدولة
 * لكل تطبيق **قائمة فعلًا** في هذا التطبيق: `AppMonitor` يطبّق معدّل التحديث المحفوظ في ملفّ
 * التطبيق عند مجيئه إلى المقدّمة، ويعيد القيمة السابقة عند مغادرته عبر
 * `PerAppRefreshRateController`. فالمعروض هنا هو **حالة ذلك المسار** لا مسار ثانٍ.
 *
 * **وحدّها المعلن:** ما يظهر أعلاه هو ما يعلنه النظام الآن (`currentEnforcedRate`)، لا ما
 * طُلب. فإن لم يعلن النظام شيئًا قلنا «لا يعلنها هذا النظام» ولم نقل «مطبَّق».
 */
@Composable
fun FasScreen(navController: NavController) {
    val context = LocalContext.current
    // مصدر الإعدادات المحفوظة نفسه؛ العدد ليس شهادة تطبيق أو تحقّق قراءة مرتجعة.
    val library by GameProfileRepository.state.collectAsState()
    LaunchedEffect(library.loading) {
        if (library.loading) GameProfileRepository.load()
    }
    val customCount = remember(library.profiles, library.loading, library.failed) {
        customRefreshCount(library.profiles, library.loading, library.failed)
    }
    // القراءة تمرّ بمسار الصدفة على بعض الأنظمة؛ فتُنفَّذ مرة عند الدخول لا في كل إعادة تركيب.
    val state by produceState<FasState?>(initialValue = null, key1 = context) {
        value = withContext(Dispatchers.IO) {
            FasState(
                current = PerAppRefreshRateController.currentEnforcedRate(context),
                supported = PerAppRefreshRateController.supportedRates(context),
            )
        }
    }

    // التنسيق يُبنى خارج التركيب لأن `joinToString` ليس سياقًا قابلًا للتركيب.
    val supportedText = remember(state?.supported) {
        val rates = state?.supported.orEmpty()
        if (rates.isEmpty()) null
        else rates.joinToString(" · ") { context.resources.getString(R.string.fas_rate_value, it) }
    }

    ScreenAccentProvider(MaterialTheme.colorScheme.secondary) {
        MaxScreen(
            title = stringResource(R.string.str_frame_aware_scheduling),
            onBack = { navController.popBackStack() },
            subtitle = stringResource(R.string.str_fas_is_a_user_space_implementa),
        ) {
            MaxSection(title = stringResource(R.string.fas_display_title)) {
                MaxGroup {
                    MaxRow(
                        title = stringResource(R.string.fas_current_rate_title),
                        subtitle = when (val snapshot = state) {
                            null -> stringResource(R.string.fas_rate_reading)
                            else -> snapshot.current?.let {
                                stringResource(R.string.fas_rate_value, it)
                            } ?: stringResource(R.string.fas_rate_unavailable)
                        },
                    )
                    MaxGroupDivider()
                    MaxRow(
                        title = stringResource(R.string.fas_supported_rates_title),
                        subtitle = supportedText ?: stringResource(R.string.fas_rate_unavailable),
                    )
                }
            }
            MaxSection(
                title = stringResource(R.string.fas_per_app_title),
                description = stringResource(R.string.fas_per_app_desc),
            ) {
                MaxGroup {
                    // **العدد قبل الرابط:** من رأى «اختر تطبيقًا» وحدها لم يعرف إن كان في
                    // السجلّ شيء أصلًا. والعدد يقول الحقيقة من المصدر مباشرة — وصفرًا يعني
                    // «لا شيء بعد» لا «لا يعمل».
                    MaxRow(
                        title = stringResource(R.string.fas_custom_rates_title),
                        subtitle = when {
                            library.loading -> stringResource(R.string.fas_custom_rates_reading)
                            // **والفشل لا يُقرأ صفرًا:** «لا شيء بعد» معلومة، و«تعذّرت القراءة»
                            // أخرى — وخلطهما يجعل المستخدم يظنّ سجلّه فارغًا وهو غير مقروء.
                            library.failed -> stringResource(R.string.fas_custom_rates_failed)
                            customCount == 0 -> stringResource(R.string.fas_custom_rates_none)
                            else -> stringResource(R.string.fas_custom_rates_count, customCount)
                        },
                        icon = Icons.Rounded.Speed,
                        iconTone = if ((customCount ?: 0) > 0) MaxTone.Accent else MaxTone.Neutral,
                    )
                    MaxGroupDivider()
                    MaxNavigationRow(
                        title = stringResource(R.string.fas_open_apps),
                        subtitle = stringResource(R.string.fas_open_apps_desc),
                        onClick = { navController.navigate(MaxDestination.Apps.route) },
                    )
                }
            }
            MaxSection(title = stringResource(R.string.fas_limits_title)) {
                MaxBullets(
                    lines = listOf(stringResource(R.string.fas_limits_body)),
                    tone = MaxTone.Caution
                )
            }
        }
    }
}

/** حالة واحدة تُقرأ مرة: المعدّل الذي يعلنه النظام الآن، والمعدّلات التي تدعمها الشاشة. */
private data class FasState(
    val current: Int?,
    val supported: List<Int>,
)

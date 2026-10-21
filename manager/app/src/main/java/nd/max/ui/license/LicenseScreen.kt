/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * شاشة الترخيص — الباب الوحيد الذي يدخل منه المفتاح إلى الجهاز.
 *
 * ## ما تقوله هذه الشاشة وما لا تقوله
 *
 * كل جملة هنا تصف **ما يفعله الكود فعلًا**: المفتاح يُتحقَّق منه مرّة عند الكتابة، وشهادة
 * الخادم الموقّعة تعمل بعدها بلا إنترنت حتى انتهائها، والجهاز مربوط بمعرّف تثبيت عشوائي.
 * ولا كلمة «مستحيل الاختراق» ولا «حماية مطلقة»: ما يُمنع فعلًا هو نسخ الشهادة إلى جهاز آخر
 * وإعادة استخدام مفتاح ملغى — وكلاهما يقيسه الخادم، لا هذه الشاشة.
 *
 * ## ولماذا لا واجهة «تسجيل دخول»
 *
 * لا حساب ولا كلمة مرور ولا بريد: مفتاحٌ يُكتب مرّة. وأي حقلٍ زائد في هذا الموضع هو سطح هجوم
 * زائد بلا مقابل — فالشاشة تعرض الحالة، وحقلًا واحدًا، وزرًّا.
 */
@file:OptIn(ExperimentalMaterial3Api::class)

package nd.max.ui.license

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import nd.max.R
import nd.max.core.access.RemoteAccess
import nd.max.core.license.LicenseFault
import nd.max.core.license.LicenseStates
import nd.max.core.license.LicenseStatus
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxHelpAction
import nd.max.ui.design.MaxInkLoop
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.MaxTone
import nd.max.ui.component.StudioTextButton
import nd.max.ui.component.StudioTonalButton
import nd.max.ui.navigation.MaxNavActions
import java.util.Date

@Composable
fun LicenseScreen(navController: NavHostController) {
    val navActions = MaxNavActions(navController)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val snapshot by RemoteAccess.state.collectAsStateWithLifecycle()
    val activation by RemoteAccess.activation.collectAsStateWithLifecycle()
    val license = snapshot.license

    var key by rememberSaveable { mutableStateOf("") }

    // نتيجة التفعيل تُعلن مرّة عند تغيّرها — والرمز يُترجم إلى جملة بالاسم نفسه.
    LaunchedEffect(activation) {
        when (val current = activation) {
            is RemoteAccess.ActivationStatus.Failed ->
                snackbarHostState.showSnackbar(context.getString(licenseErrorRes(current.code)))
            RemoteAccess.ActivationStatus.Succeeded -> {
                key = ""
                snackbarHostState.showSnackbar(context.getString(R.string.max_license_activated))
            }
            else -> Unit
        }
    }

    MaxScreen(
        title = stringResource(R.string.max_license_title),
        onBack = navActions::back,
        accentIcon = Icons.Rounded.Key,
        snackbarHostState = snackbarHostState,
        actions = {
            MaxHelpAction(
                title = stringResource(R.string.max_license_title),
                body = listOf(stringResource(R.string.max_license_subtitle), stringResource(R.string.max_license_help_body)).joinToString(separator = "\n\n"),
            )
        },
    ) {
        // ① الحالة — قبل أي حقل: أوّل ما يريد المستخدم معرفته هو «هل أنا داخل؟».
        MaxSection(title = stringResource(R.string.max_license_status_section)) {
            MaxGroup {
                MaxRow(
                    title = stringResource(licenseStatusRes(license.status)),
                    subtitle = licenseStatusDetail(license.status, LicenseStates.remainingDays(license)),
                    icon = when (license.status) {
                        LicenseStatus.ACTIVE -> Icons.Rounded.CheckCircle
                        LicenseStatus.EXPIRED -> Icons.Rounded.Schedule
                        LicenseStatus.NONE -> Icons.Rounded.Lock
                    },
                    iconTone = when (license.status) {
                        LicenseStatus.ACTIVE -> MaxTone.Positive
                        LicenseStatus.EXPIRED -> MaxTone.Caution
                        LicenseStatus.NONE -> MaxTone.Inactive
                    },
                )
                if (license.status == LicenseStatus.ACTIVE && license.plan.isNotBlank()) {
                    MaxGroupDivider()
                    MaxRow(
                        title = stringResource(R.string.max_license_plan_title),
                        subtitle = license.plan,
                        iconTone = MaxTone.Neutral,
                    )
                }
                if (license.status == LicenseStatus.ACTIVE && license.deviceLimit > 0) {
                    MaxGroupDivider()
                    MaxRow(
                        title = stringResource(R.string.max_license_devices_title),
                        subtitle = stringResource(
                            R.string.max_license_devices_detail,
                            license.devicesUsed,
                            license.deviceLimit,
                        ),
                    )
                }
                val device = RemoteAccess.devicePreview()
                if (device.isNotBlank()) {
                    MaxGroupDivider()
                    MaxRow(
                        title = stringResource(R.string.max_license_device_id_title),
                        subtitle = stringResource(R.string.max_license_device_id_detail, device),
                    )
                }
                // نافذة المجّاني داخل المجموعة نفسها: وعدٌ يخصّ الحالة، فلا قسمٌ ثانٍ بعنوانها.
                val promo = snapshot.promo
                if (promo != null) {
                    val until = remember(promo.toMillis) {
                        android.text.format.DateFormat.getMediumDateFormat(context).format(Date(promo.toMillis))
                    }
                    MaxGroupDivider()
                    MaxRow(
                        title = stringResource(R.string.max_license_promo_active, until),
                        subtitle = promo.note.takeIf { it.isNotBlank() },
                        icon = Icons.Rounded.CheckCircle,
                        iconTone = MaxTone.Positive,
                    )
                }
            }
        }

        // ② التفعيل — حقل واحد وزرّان: تفعيل، وتحقق يدويّ ممّن له مفتاح مخزَّن بالفعل.
        MaxSection(title = stringResource(R.string.max_license_key_section)) {
            MaxGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaxSpace.rowPaddingHorizontal,
                            vertical = MaxSpace.rowPaddingVertical,
                        ),
                    verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
                ) {
                    OutlinedTextField(
                        value = key,
                        onValueChange = { key = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.max_license_key_label)) },
                        placeholder = { Text(stringResource(R.string.max_license_key_placeholder)) },
                        supportingText = { Text(stringResource(R.string.max_license_key_helper)) },
                        singleLine = true,
                        enabled = activation != RemoteAccess.ActivationStatus.Running,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MaxSpace.md)) {
                        StudioTonalButton(
                            onClick = { scope.launch { RemoteAccess.activate(key) } },
                            enabled = key.isNotBlank() && activation != RemoteAccess.ActivationStatus.Running,
                        ) {
                            Text(
                                stringResource(
                                    if (activation == RemoteAccess.ActivationStatus.Running) {
                                        R.string.max_license_activating
                                    } else {
                                        R.string.max_license_activate
                                    },
                                ),
                            )
                        }
                        StudioTextButton(onClick = { RemoteAccess.refresh("manual") }) {
                            Text(stringResource(R.string.max_license_refresh))
                        }
                    }
                    // «الحبر»: موجة تُكتب ما دام الطلب في الجوّ وتزول بزواله — عمرها هو الغرض.
                    // ومعها الزرّ يقول «جارٍ التفعيل…» والحقل مُعطَّل: ثلاث إشارات لحالة واحدة.
                    if (activation == RemoteAccess.ActivationStatus.Running) {
                        MaxInkLoop(modifier = Modifier.fillMaxWidth().height(MaxSize.iconGlyph))
                    }
                }
            }
        }
    }
}

@StringRes
private fun licenseStatusRes(status: LicenseStatus): Int = when (status) {
    LicenseStatus.ACTIVE -> R.string.max_license_status_active
    LicenseStatus.EXPIRED -> R.string.max_license_status_expired
    LicenseStatus.NONE -> R.string.max_license_status_none
}

/** تفصيل الحالة: المتبقّي بالأيام لسارٍ، وسطرٌ يقول ما العمل لمنتهٍ أو غير مُفعَّل. */
@Composable
private fun licenseStatusDetail(status: LicenseStatus, remainingDays: Long?): String = when (status) {
    LicenseStatus.ACTIVE -> stringResource(R.string.max_license_active_detail, remainingDays ?: 0L)
    LicenseStatus.EXPIRED -> stringResource(R.string.max_license_expired_detail)
    LicenseStatus.NONE -> stringResource(R.string.max_license_none_detail)
}

/**
 * رمز العطب → جملته. والمجهول يُسمّى «مجهول» ولا يُلبَّس جملةً عامّة تُخفي السبب:
 * سببٌ مسمّى هو الفرق بين تذكرة دعمٍ تُحلّ من أول ردّ وأخرى تدور أسبوعًا.
 */
@StringRes
private fun licenseErrorRes(code: String): Int = when (code) {
    RemoteAccess.REJECT_INVALID_KEY -> R.string.max_license_error_invalid_key
    LicenseFault.NOT_CONFIGURED.code -> R.string.max_license_error_not_configured
    LicenseFault.NETWORK.code -> R.string.max_license_error_network
    LicenseFault.TIMEOUT.code -> R.string.max_license_error_timeout
    LicenseFault.CLIENT.code -> R.string.max_license_error_client
    LicenseFault.SERVER.code -> R.string.max_license_error_server
    LicenseFault.FORMAT.code -> R.string.max_license_error_format
    "expired" -> R.string.max_license_error_expired
    "not_found" -> R.string.max_license_error_not_found
    "revoked" -> R.string.max_license_error_revoked
    "device_limit" -> R.string.max_license_error_device_limit
    "device_unknown" -> R.string.max_license_error_device_unknown
    "device_removed" -> R.string.max_license_error_device_removed
    "rate_limited" -> R.string.max_license_error_rate_limited
    else -> R.string.max_license_error_unknown
}

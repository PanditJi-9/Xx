/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
/*
 * بوّابة الميزة — الموضع **الوحيد** الذي يُترجم فيه حكم الوصول إلى واجهة.
 *
 * ## ثلاث حالات، لا حالتان
 *
 * `OPEN`/`PROMO` ⇒ يُرسم المحتوى. `LOCKED` ⇒ تُرسم نافذة «يحتاج ترخيصًا» — **والسطح يبقى
 * ظاهرًا**. `OFF` ⇒ لا شيء إطلاقًا (المالك أطفأه). والفرق بين `LOCKED` و`OFF` مقصود: ميزةٌ
 * يمكن شراؤها لا تُخفى، وميزةٌ أُطفئت لا تُعلَّق على الشاشة.
 *
 * ## ولماذا تُغلَّف الوجهة كاملة لا كلُّ زرٍّ فيها
 *
 * لو حُكم على الأزرار واحدًا واحدًا لصار كل زرٍّ سؤالًا جديدًا («هل هذا الزرّ داخل الميزة؟»)،
 * وأول زرٍّ يُنسى هو ثغرة. البوّابة عند مدخل الوجهة تجعل الحكم واحدًا: إمّا تدخل السطح كاملًا
 * وإمّا ترى نافذة الترخيص — ولا حالة وسطى.
 *
 * ## نافذة القفل — أعيد تصميمها (طلب المالك: «في منتصف الشاشة، باحتراف، وأكثر تخصيصًا»)
 *
 * * طبقة كاملة (`surfaceContainerLowest`) وبطاقة واحدة مركزيّة بحافة مُمَيّزة: تُقرأ **نافذة
 *   عائمة** لا بطاقةً داخل صفحة — والفارق بصريّ بحت، والحكم نفسه لم يتغيّر.
 * * والعنوان والنصّ **يُقرآن من إعداد اللوحة**: `features.<id>.title` و`message_ar`/`message_en`
 *   — وقد كانا يُنشران من Max Control ثم لا يقرؤهما هذا الملفّ، فيكتب المالك ويُهمَل كلامه.
 * * و`subscription` (الرابط + نصّ الدفع) صار له أول مستهلك في التطبيق: زرّ اشتراك في النافذة
 *   يظهر فقط إن نشره المالك.
 * * وأيقونة السطح من مفرداتنا (خريطة في أسفل الملفّ)، والمجهول يأخذ القفل.
 *
 * ## والقياس
 *
 * حكم الوصول من `RemoteAccess.access(featureId)` — دالّة نقية مقيسة على JVM (`FeatureAccessTest`،
 * إحدى عشرة خلية مصفوفة). وما لا يُقاس هنا — رسم النافذة نفسه (ترتيب، RTL، مقاسات على شاشة
 * حقيقية) — لا يُدَّعى قياسه: يُرى على جهاز.
 */
package nd.max.ui.license

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import nd.max.R
import nd.max.core.access.FeatureAccess
import nd.max.core.access.RemoteAccess
import nd.max.core.license.LicenseStatus
import nd.max.core.remote.RemoteFlags
import nd.max.ui.component.StudioTextButton
import nd.max.ui.component.StudioTonalButton
import nd.max.ui.design.MaxCardShell
import nd.max.ui.design.MaxRadius
import nd.max.ui.design.MaxSize
import nd.max.ui.design.MaxSpace
import nd.max.ui.design.maxRadius
import nd.max.ui.navigation.MaxDestination

/**
 * يرسم [content] حين يكون السطح مفتوحًا (أو داخل نافذة مجّاني)، ونافذة الترخيص حين يكون مقفلًا،
 * ولا شيء حين يكون مُطفأً. والاختبار يُبنى على حال الإعداد المجموعة (فيُعاد الرسم عند تغيّرها)
 * لا على قراءة واحدة عند التركيب.
 */
@Composable
fun FeatureGate(
    featureId: String,
    navController: NavController,
    content: @Composable () -> Unit,
) {
    val snapshot by RemoteAccess.state.collectAsStateWithLifecycle()
    val access = remember(snapshot) { RemoteAccess.access(featureId) }
    when (access) {
        FeatureAccess.OPEN, FeatureAccess.PROMO -> content()
        FeatureAccess.LOCKED -> LockedSurfacePanel(navController, featureId)
        FeatureAccess.OFF -> Unit
    }
}

/** نافذة القفل: طبقة كاملة + بطاقة مركزيّة واحدة، ونصوصها من إعداد اللوحة حين كُتبت. */
@Composable
private fun LockedSurfacePanel(navController: NavController, featureId: String) {
    val context = LocalContext.current
    val snapshot by RemoteAccess.state.collectAsStateWithLifecycle()
    val feature = snapshot.config.features[featureId]
    val arabic = LocalConfiguration.current.locales[0].language.lowercase().startsWith("ar")

    // ① العنوان: ما كتبه المالك لسطحه، وإلا نصّنا الافتراضي.
    val title = feature?.title?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.max_license_gate_title)

    // ② النصّ: رسالة الميزة (بلغة القارئ، وإلا بالأخرى)، وإلا نصّ الحالة الفعليّة — والانتهاء
    //    يُميَّز عن عدم التفعيل لأن أوّل خطوةٍ تختلف (تجديد مقابل تفعيل أول).
    val message = (if (arabic) feature?.messageAr else feature?.messageEn)
        ?.takeIf { it.isNotBlank() }
        ?: (if (arabic) feature?.messageEn else feature?.messageAr)?.takeIf { it.isNotBlank() }
    val detail = message ?: when (snapshot.license.status) {
        LicenseStatus.EXPIRED -> stringResource(R.string.max_license_expired_detail)
        else -> stringResource(R.string.max_license_gate_detail)
    }

    // ③ الاشتراك — أول مستهلك لـ`subscription` في التطبيق. ويظهر الزرّ فقط إن نُشر رابط
    //    صالح: لا زرّ اشتراك يشير إلى فراغ.
    val subscription = snapshot.config.subscription
    val paymentText = (if (arabic) subscription?.paymentTextAr else subscription?.paymentTextEn)
        ?.takeIf { it.isNotBlank() }
        ?: (if (arabic) subscription?.paymentTextEn else subscription?.paymentTextAr)
            ?.takeIf { it.isNotBlank() }
    val subscribeUrl = subscription?.url?.trim()?.takeIf { it.startsWith("http", ignoreCase = true) }

    // §٥: من رأى القفل على **سطح Max AI** يُفتح له بابٌ يشرح المحرّك قبل أي قرار شراء، ولا
    // يُوجَّه إليه من قفلِ سطحٍ آخر (فيُقرأ كلامٌ عن الذكاء في موضع الصوت مثلًا). وباب الشرح
    // نفسه غير مقفل — غايته أن يُعرَض.
    val intro = featureId == RemoteFlags.MAXAI

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center,
    ) {
        MaxCardShell(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = MaxSize.readingMaxWidth)
                .padding(horizontal = MaxSpace.gutter),
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            accent = MaterialTheme.colorScheme.primary,
            verticalArrangement = Arrangement.spacedBy(MaxSpace.lg),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(MaxSize.iconContainer)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(maxRadius(MaxRadius.inset)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = featureIcon(featureId),
                        contentDescription = null,
                        modifier = Modifier.size(MaxSize.iconGlyph),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.max_license_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (paymentText != null) {
                    Text(
                        text = paymentText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            StudioTonalButton(
                onClick = { navController.navigate(MaxDestination.License.launchRoute) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.max_license_gate_action))
            }
            if (subscribeUrl != null) {
                StudioTextButton(
                    onClick = { openSubscriptionPage(context, subscribeUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.max_license_gate_subscribe))
                }
            }
            if (intro) {
                StudioTextButton(
                    onClick = { navController.navigate(MaxDestination.MaxAiIntro.launchRoute) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.maxai_intro_gate_action))
                }
            }
        }
    }
}

/**
 * أيقونة السطح — لكل ميزة رمزها من مفرداتنا، والمجهول يأخذ القفل.
 *
 * و`Max AI` تأخذ **الدرع** لا النجمة: عقد الهوية البصرية (`VisualIdentityTest`) يقصر
 * `AutoAwesome` على أسطح الدخول الثلاثة (الزرّ · الاختصار · وجهة الملاحة)، وبوّابة القفل ليست
 * سطح دخول — والرمز فيها يقول «هذا محميّ بمفتاح» لا «هذا ذكاء».
 */
private fun featureIcon(featureId: String): ImageVector = when (featureId) {
    RemoteFlags.MAXAI -> Icons.Rounded.Shield
    RemoteFlags.AUDIO -> Icons.Rounded.GraphicEq
    RemoteFlags.GAMESPACE -> Icons.Rounded.SportsEsports
    RemoteFlags.EMUHUB -> Icons.Rounded.Memory
    RemoteFlags.BYPASS_CHARGE -> Icons.Rounded.BatteryChargingFull
    RemoteFlags.SPOOF -> Icons.Rounded.Security
    RemoteFlags.ATLAS -> Icons.Rounded.Insights
    else -> Icons.Rounded.Lock
}

/** فتح صفحة الاشتراك في المتصفّح — لا شراء داخل التطبيق ولا حساب. */
private fun openSubscriptionPage(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

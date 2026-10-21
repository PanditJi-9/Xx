/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.ui.license

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import nd.max.R
import nd.max.ui.design.MaxScreen

/**
 * **شاشة «ما يفعله Max AI»** — صورةٌ واحدة هي شجرة قدرات المحرّك ([MaxAiCapabilityTree]).
 *
 * كانت الشاشة نصوصًا وجدولًا وقائمة «ما لا نفعله» وصفَّ ترخيص، فأُزيلت كلها بطلب المالك
 * (٢٠٢٦-١٠-٢٢): «غير مفيد». ويبقى للشاشة بابان كما كانا: من شاشة المحرّك، ومن لوح قفله —
 * وهي غير مقفلة في الرسم الملاحيّ، فيراها من لا يملك مفتاحًا أيضًا.
 */
@Composable
fun MaxAiIntroScreen(navController: NavHostController) {
    MaxScreen(
        title = stringResource(R.string.maxai_intro_title),
        subtitle = stringResource(R.string.maxai_tree_subtitle),
        onBack = { navController.popBackStack() },
        accentIcon = Icons.Rounded.Insights,
        accent = MaterialTheme.colorScheme.tertiary,
    ) {
        MaxAiCapabilityTree(modifier = Modifier.fillMaxWidth())
    }
}

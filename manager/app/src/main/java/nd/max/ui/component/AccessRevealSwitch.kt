/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import nd.max.R
import nd.max.core.privilege.AccessFilter
import nd.max.core.privilege.AccessFilterStore

/**
 * **مفتاح الكشف: «إظهار الميزات التي تحتاج صلاحية أعلى».**
 *
 * حين يكون وضع التشغيل Shizuku أو Basic، يُخفي [AccessFilter.SUPPORTED_ONLY] كل وجهة تحتاج
 * صلاحية أعلى — أدوات في التحكّم، وبطاقات في الرئيسية، وشاشات في المُوجِّد. فيرى المستخدم
 * تطبيقًا ناقصًا ولا يعرف أن ما ينقصه موجودٌ ومنتظرٌ صلاحية. وهذا المفتاح يقلب الفلتر إلى
 * [AccessFilter.EVERYTHING] فتظهر الأدوات والبطاقات والشاشات موسومةً بمستواها.
 *
 * **ولماذا مفتاح لا خيار داخل نافذة:** النافذة [AccessModeDialog] تُطلب من شاشة الامتياز، ومن
 * رأى ميزةً اختفت لا يعرف أن الخيار هناك. فالباب نفسه يُقرأ من مكانين يمرّ بهما كل مستخدم:
 * شاشة البداية والإعدادات.
 *
 * **والحدّ:** الكشف لا يمنح صلاحية ولا يشغّل شيئًا. الوجهةُ الظاهرة تعمل فقط إذا كان مستواها
 * متاحًا؛ وما دون ذلك يُعرض بوسمه كما يفعل [PrivilegePanel]. ولذلك يبقى المصدر واحدًا:
 * [AccessFilterStore] — لا تفضيل ثانٍ يُكتب بجواره فينفصل الحالان.
 */
@Composable
fun AccessRevealSwitch() {
    val context = LocalContext.current
    val filter by AccessFilterStore.state.collectAsState()
    ExpressiveSwitchItem(
        icon = Icons.Rounded.Visibility,
        title = stringResource(R.string.access_reveal_title),
        summary = stringResource(R.string.access_reveal_desc),
        checked = filter == AccessFilter.EVERYTHING,
        onCheckedChange = { reveal ->
            AccessFilterStore.save(
                context,
                if (reveal) AccessFilter.EVERYTHING else AccessFilter.SUPPORTED_ONLY,
            )
        },
    )
}

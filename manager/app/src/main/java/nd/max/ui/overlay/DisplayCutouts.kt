/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * قصّات الشاشة وشريط الحالة — قراءة واحدة يشترك فيها التراكب والمعاينة والإعداد.
 *
 * ### لماذا مكان واحد
 *
 * كانت قراءة القصّة داخل `OverlayWindow` وحدها، والمعاينة تحتاجها لترسم الكاميرا وتضع الكبسولة
 * أسفلها. وقراءتان لشيء واحد تفترقان عند أوّل تعديل: الخدمة تقيس قصّة، والمعاينة تقيس غيرها،
 * فيصير «أسفل الكاميرا» موضعين مختلفين. فصار للقراءة مكان واحد يستدعيه الجميع.
 */
package nd.max.ui.overlay

import android.content.res.Resources
import android.os.Build
import android.view.View

object DisplayCutouts {

    /**
     * القصّات بإحداثيّات الشاشة. فارغة قبل أندرويد ١٠ (لا مفهوم قصّة قبله)، وفارغة قبل أن تُركَّب
     * النافذة (لا عرض يُقرأ منها بعد).
     */
    fun of(view: View): List<OverlayGeometry.Cutout> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()
        val cutout = view.display?.cutout ?: return emptyList()
        return cutout.boundingRects.map {
            OverlayGeometry.Cutout(it.left, it.top, it.right, it.bottom)
        }
    }

    /** ارتفاع شريط الحالة بالبكسل من مورد النظام، و`0` إن لم يُعلن. */
    fun statusBarPx(resources: Resources): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else 0
    }
}

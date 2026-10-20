/*
 * Ported from Islandify (https://github.com/bughunter11/Islandify) — MIT License,
 * Copyright (c) 2026 bughunter11. Full license text: THIRD_PARTY_NOTICES.md.
 * Adapted for MaxManager (packages, resources, integration). Modifications (C) 2026 Nader Magdy.
 */
package nd.max.island.core

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * يبني نظام ألوان Material 3 حقيقيًّا (لوحات HCT) من لون بذرة واحد.
 *
 * **ولماذا انتقل هذا الملف من `island/ui/theme` إلى `island/core`:**
 * كان سكنه السابق داخل حزمة واجهة الاستوديو المستقلّ (`AppTheme`/`Palette`)، وهي الحزمة التي
 * أُلغيت بدمج الاستوديو في شاشة الجزيرة الديناميكية. لكن **المستهلك الحقيقي لم يكن الاستوديو**:
 * هو `IslandUi` و`IslandUiWidgets` — أي أن الطبقة المرسومة فوق التطبيقات نفسها. فلمّا حُذفت
 * الواجهة، بقي المحرّك محتاجًا إليها؛ فوُضعت في `core` حيث تنتمي: عقدٌ مشترك بين المحرّك
 * والواجهة، لا زينةُ نشاط.
 */
fun seedScheme(seed: Int, dark: Boolean): ColorScheme =
    dynamicColorScheme(
        seedColor = Color(seed),
        isDark = dark,
        isAmoled = false, // required by material-kolor 2.x; MaxManager never requests AMOLED here
        style = PaletteStyle.TonalSpot,
    )

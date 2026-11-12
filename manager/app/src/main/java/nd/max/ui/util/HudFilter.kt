/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.ui.util

/**
 * ماذا يعرض المراقب: النصّ وحده، المنحنى وحده، أو الاثنين.
 *
 * والفصل مقصود: مراقب صغير فوق لعبة يريد المنحنى بلا أرقام تزحم الشاشة، ومراقب
 * تشخيصي يريد الأرقام بلا رسم. و`Chart` بلا حقل إطارات يسقط إلى النصّ — لا صندوق فارغ.
 */
enum class HudFilter { Text, Chart, All }

/** هل يُرسم النصّ؟ `Chart` يُخفيه فقط إن كان المنحنى ممكنًا فعلًا. */
fun hudTextVisible(filter: HudFilter, chartPossible: Boolean): Boolean =
    filter != HudFilter.Chart || !chartPossible

/** هل يُرسم المنحنى؟ `Chart` يفرضه، و`All` يتبعه إن كان الحقل مطلوبًا. */
fun hudChartVisible(filter: HudFilter, showGraph: Boolean, chartPossible: Boolean): Boolean =
    chartPossible && (filter == HudFilter.Chart || (filter == HudFilter.All && showGraph))

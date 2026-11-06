/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.platform

/**
 * الميزات التي **تعلنها** حزمة في مانيفستها (`<uses-feature>`) مقابل ما يعلنه الجهاز فعلًا.
 *
 * **ولماذا هذا حكمٌ لا قائمة:** سؤال المستخدم الحقيقي عند تثبيت تطبيق هو «هل يعمل هنا؟»؛ و
 * `PackageManager` يعطي الطرفين — ما تطلبه الحزمة وما يقدّمه الجهاز — فالمقارنة بينهما جوابٌ
 * مقيس، لا تخمينٌ من الاسم. وما يطلبه التطبيق ولا يوجد هنا يُعرض صريحًا بدل أن يظهر لاحقًا
 * كسلوك غريب.
 *
 * **وحدوده:** الإعلان وحده؛ بعض الحزم تطلب ميزة بلا `required="true"` فتعمل منقوصة، وهذا
 * لا يُستنتج من هنا. ولا تُقرأ حالة الإذن ولا دعم العتاد الفعليّ — فقط ما يعلنه مدير الحزم.
 */
data class PackageFeatures(
    val required: List<String>,
    val unsupported: List<String>
) {
    val supported: List<String> get() = required - unsupported.toSet()
}

object PackageFeatures {

    /**
     * يقارن المطلوب بالمتاح. الأسماء تُشذَّب وتُنزع تكرارها وتُرتَّب، والفارغ يُسقط — لأن
     * `FeatureInfo.name` قد يكون `null` أو فراغًا، وعرضه سطرًا فارغًا يوهم بميزة بلا اسم.
     */
    fun split(required: List<String>, available: Set<String>): PackageFeatures {
        val names = required
            .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .distinct()
            .sorted()
        return PackageFeatures(
            required = names,
            unsupported = names.filterNot { it in available }
        )
    }
}

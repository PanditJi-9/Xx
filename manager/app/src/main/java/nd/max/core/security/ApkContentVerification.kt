/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

import android.content.Context
import com.android.apksig.ApkVerifier
import java.io.File

/**
 * التحقّق من **محتوى الحزمة** مقابل البصمات المكتوبة في توقيعها — لا من موقّعها فقط.
 *
 * ## الثغرة التي تُغلقها هذه الطبقة — وهي أخطر ما قوبل في هذه الجولة
 *
 * فحص «هل البصمة هي بصمتنا؟» يسقط كلّه في سيناريو واحد مشهور: أن يُبطَل **تحقّق النظام**
 * لا توقيعنا. على جهاز متجذّر يُثبَّت مُبطِل تحقّق التوقيع (نظير `core_patch`/LSPatch)،
 * فتُعدَّل الحزمة (DEX أو موارد) **وتُترك بايتات التوقيع كما هي**. هنا:
 *
 * * بصمة الموقّع **لم تتغيّر** ⇒ [SecurityPin] يقول «سليم».
 * * والأصول المختومة والمكتبة الأصلية تُكشف ([AssetVault] و[IntegrityProbe]) — إن طُالت.
 * * **وأمّا الـDEX فتغيّر بلا أن يراه شيء.** أي أنّ تعديل منطق القلب (المحرّك، الهوك،
 *   الخوارزميات) كان يمرّ صامتًا.
 *
 * والحلّ الصحيح الوحيد هو ما تفعله هذه الطبقة: إعادة حساب بصمات المحتوى ومواجهتها
 * بالبصمات الموقّعة. وهذا هو **تحقّق توقيع كامل** لا استدعاء مجرّد.
 *
 * ## ولماذا مكتبة جوجل لا مُتحقِّق بيدنا
 *
 * صيغة `APK Signature Scheme v2` تتضمّن حساب بصمة **مقطّعة** لكل مدخل (شرائح ‎1‎ميجا،
 * بادئة ومحاذاة محدّدتان، واستثناءات `META-INF`)، وأي انحراف بحرف واحد في التنفيذ يُنتج
 * أحد عطبين: **رفض نسخة سليمة** (فعطب بالغ)، أو **قبول نسخة معدّلة** (فعطب بلا مقابل).
 * و`com.android.apksig` هي نفسها التي تُنتج التوقيع في AGP وتتحقّق منه في `apksigner`،
 * فاستعمالها يجعل المُتحقِّق والمُوقِّع من مصدر واحد — وهذا أمتن ما يمكن بلوغه محليًّا.
 *
 * ## وحدودها المُعلنة
 *
 * * **الكلفة:** تحقّق كامل يعني قراءة الحزمة كلها وحساب بصمات مدخلاتها. ولهذا **لا يُنادى
 *   على كل شاشة**، بل مرّة واحدة عند الإقلاع (انظر [SecurityGate]) — والزمن المقيس على
 *   حزمة حقيقية في هذه الجولة مسجّل في `docs/ai/VALIDATION.md`.
 * * **والثلاث حالات لا اثنتان:** «متحقَّق» و«مرفوض» و«**لم يتسنَّ الفحص**». والحالة الثالثة
 *   ليست تلاعبًا: فشل المكتبة نفسها (نسخة نظام غير متوقّعة، أو حزمة غير موقّعة في debug)
 *   لا يجوز أن يتحوّل إلى رفض — من يفعل ذلك يحوّل كل عطب بيئيّ إلى «نسخة معدّلة».
 */
object ApkContentVerification {

    /** حصيلة الفحص. */
    enum class Outcome {
        /** التوقيع سليم ومحتوى الحزمة مطابق لبصماته الموقّعة. */
        VERIFIED,

        /** التوقيع **موجود ومقروء** لكن التحقّق فشل: محتوى مُعدَّل أو توقيع معطوب. */
        REJECTED,

        /** لم يتسنَّ الفحص أصلًا (حزمة بلا موقّع، أو فشل داخل المكتبة). **ليست تلاعبًا.** */
        UNAVAILABLE,
    }

    /**
     * أدنى منصّة يُتحقّق من أجلها حين لا يُعرف غيرها: **٢٤** هو أوّل إصدار لا يُطلب فيه
     * توقيع v1 (`minSdk 24` ⇒ لا حاجة لتوقيع JAR). ومن دونه يُطلب v1 فيُرفض ما لم يُوقّع به.
     */
    const val DEFAULT_MIN_CHECKED_PLATFORM_VERSION = 24

    /**
     * يتحقّق من حزمة التطبيق المثبَّتة (`sourceDir`).
     *
     * **وعلى المنصّة المُثبِّتة لا على ثابت في الكود:** `applicationInfo.minSdkVersion`
     * هي المنصّة التي طلبتها الحزمة نفسها، وعليها يعتمد ما إذا كان `v1` مشروطًا أصلًا.
     * ومن يفحص بثابت (مثلًا ٢٤ دائمًا) يُخطئ في الاتجاهين: يرفض حزمة سليمة على منصّة
     * قديمة تطلب v1، أو يمرّر ما لا يُطلب عليها.
     *
     * ويُستعمل [ApkVerifier.Result.errors] للتمييز: تحقّق فاشل **بأخطاء مسجَّلة** هو «مرفوض»،
     * وتحقّق فاشل بلا أي خطأ (أو استثناء) هو «لم يتسنَّ الفحص».
     */
    fun verify(context: Context): Outcome {
        val apk = File(context.applicationInfo.sourceDir)
        if (!apk.isFile) return Outcome.UNAVAILABLE
        return verifyFile(apk, context.applicationInfo.minSdkVersion)
    }

    /**
     * الجوهر مُفصول عن `Context` حتى يُقاس في JVM عادية على حزمة مبنيّة فعلًا.
     *
     * و[ApkVerifier.Builder.setMinCheckedPlatformVersion] هو ما يجعل الحكم مطابقًا لمنصّة
     * التثبيت؛ وبدونه يستعمله apksig بافتراضه الداخلي — وهو ما لا يُقاس في هذه الطبقة.
     */
    fun verifyFile(
        apk: File,
        minCheckedPlatformVersion: Int = DEFAULT_MIN_CHECKED_PLATFORM_VERSION,
    ): Outcome = runCatching {
        if (!apk.isFile) return Outcome.UNAVAILABLE
        val result = ApkVerifier.Builder(apk)
            .setMinCheckedPlatformVersion(minCheckedPlatformVersion)
            .build()
            .verify()
        when {
            result.isVerified -> Outcome.VERIFIED
            result.containsErrors() -> Outcome.REJECTED
            else -> Outcome.UNAVAILABLE
        }
    }.getOrDefault(Outcome.UNAVAILABLE)
}

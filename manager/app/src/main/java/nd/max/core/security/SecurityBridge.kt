/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.security

/**
 * المرساة الأصلية: **نفس الرصد من كود مُصرَّف، لا من DEX**.
 *
 * ## لماذا نقل الرصد نفسه إلى native — وليس نقل «كلمة مرور» إلى native
 *
 * القاعدة التي بُني عليها هذا الجسر، وهي الردّ على أشهر توهّم في حماية الحزم: نقلُ منطق
 * إلى C++ **لا يحمي بذاته**؛ ما يحمي هو أن يصير لكل حكم **مساران مستقلّان** يُبطلان معًا
 * لا واحدًا. وكل ما يلي في DEX يمكن تعطيله من Java: `Debug.isDebuggerConnected` تُستبدل،
 * و`File("/proc/self/maps")` تُحوَّل إلى ملفّ نظيف، و`System.loadLibrary` تُعترض. أمّا
 * الرصد هنا فيقع داخل المكتبة الأصلية، ويُعاد **بتٌّ واحد** عبر JNI — فتعطيله لا يحتاج
 * إخفاء أثر في Java فقط، بل تعديل ذاكرة عملية مُصرَّفة.
 *
 * ## وحدّها المعلن — بلا تهويل
 *
 * المكتبة الأصلية تُبنى في CI وحدها (`cargo ndk`)، وغيابها **ليس عطبًا**: كل مسار هنا
 * يعود `null` ويمضي المتصل بمساره. ولا يجوز أن يُبنى الرفض على غياب المكتبة، لأن بناءً
 * محليًّا بلا NDK — وهو المتاح دائمًا للمالك — سيصير «مخترقًا» بالمعنى نفسه. ولذلك غياب
 * المرساة **يُسجَّل** في التشخيص ولا يُحتسب دليلًا، والرفض لا يُبنى إلا على ما رُصد فعلًا.
 */
object SecurityBridge {

    /**
     * هل حُمِّلت المكتبة الأصلية؟ يُحسب مرّة واحدة.
     *
     * وغيابها يُسجَّل في التشخيص مرة واحدة من [SecurityGate] لا من هنا — تسجيلُه في كل
     * نداء يصنع ضجيجًا يوهم بعطب متكرّر، وهو ما وقع فعلًا في هذا المستودع مع
     * `PredictorBridge` قبل أن يُنقل التسجيل إلى موضع واحد.
     */
    val nativeAvailable: Boolean = runCatching {
        System.loadLibrary("maxmanager_native")
    }.isSuccess

    /** بتات [nativeRuntimeProbe] — العقود بين الجانبين، والقيم مطابقة حرفيًّا في Rust. */
    private const val BIT_TRACER = 1 shl 0

    /** مكتبة هوك/ريث ظاهرة في خريطة ذاكرة العملية. */
    private const val BIT_HOOK_LIBRARY = 1 shl 1

    /** المتتبّع مُعلن من نواة Linux نفسها، لا من `/proc` المقروء من Java. */
    private const val BIT_DEBUG_PORT = 1 shl 2

    /**
     * رصد من داخل native. تُعاد `null` إن غابت المكتبة.
     *
     * والعائد كائن مُفكَّك لا رقم: القارئ لا يفكّ بتات، فلا يُخطئ أحدُهما في قراءة الآخر.
     */
    fun runtimeProbe(): NativeProbe? {
        if (!nativeAvailable) return null
        val mask = runCatching { nativeRuntimeProbe() }.getOrNull() ?: return null
        return NativeProbe(
            tracerAttached = (mask and BIT_TRACER) != 0,
            hookLibraryMapped = (mask and BIT_HOOK_LIBRARY) != 0,
            debugPortOpen = (mask and BIT_DEBUG_PORT) != 0,
        )
    }

    /**
     * بصمة SHA-256 لملفّ، محسوبة في native.
     *
     * وتُستعمل في مقارنة ملفّ المكتبة الأصلية المستخرج على القرص بنسخته داخل الحزمة:
     * الحسابان (هنا وفي [IntegrityProbe]) مستقلّان بمُحرّكين مختلفين — فمن أراد إخفاء
     * استبدال المكتبة عليه أن يُبطل `java.security.MessageDigest` **و** كود SHA الأصلية.
     */
    fun fileDigestHex(path: String): String? {
        if (!nativeAvailable || path.isEmpty()) return null
        return runCatching { nativeFileDigest(path) }.getOrNull()?.takeIf { it.length == 64 }
    }

    /** تفصيل الرصد الأصلي — مُفكَّك لأن القارئ البشري لا يفكّ الأقنعة. */
    data class NativeProbe(
        val tracerAttached: Boolean,
        val hookLibraryMapped: Boolean,
        val debugPortOpen: Boolean,
    ) {
        /** هل رُصد شيء؟ `false` تعني «مرّ نظيفًا»، و`null` خارجيًّا تعني «لم تُقس». */
        val anyEvidence: Boolean get() = tracerAttached || hookLibraryMapped || debugPortOpen
    }

    private external fun nativeRuntimeProbe(): Int

    private external fun nativeFileDigest(path: String): String
}

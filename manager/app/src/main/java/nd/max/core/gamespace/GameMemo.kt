/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.gamespace

/**
 * مذكّرة قصيرة تبقى **مرئية** فوق اللعبة، لا تنبيهًا لحظيًّا ينتهي.
 *
 * [PanelReminder] ينبّه مرّة عند انتهاء مدّة؛ وهذه تحمل سطرًا يبقى ما دامت اللعبة أمام المستخدم
 * («المرحلة ٤ — الباب الأيسر»). وليست سطحًا ثانيًا للمؤقّت: المؤقّت يبقى مؤقّتًا، والمذكّرة نصٌّ
 * يُثبَّت ويُطفأ يدويًّا.
 *
 * والقاعدة الصادقة هنا: النصّ الفارغ لا يُعرض أبدًا (`visible=false`)، والمجهول لا يُخمَّن؛ ومن
 * كتب سطرًا طويلًا يُقصّ عند [MAX_LENGTH] بدل أن يغطّي نصف الشاشة.
 */
data class GameMemo(val text: String = "", val visible: Boolean = false) {

    /** ما يُرسم فعلًا: لا فراغ ولا أسطر متعدّدة (تراكب سطر واحد). */
    val shownText: String get() = if (visible) Companion.sanitize(text) else ""

    companion object {
        /** حدّ سطرٍ واحد مقروء أعلى اللعبة؛ يبقى تحت سطرَين على ٣٢٠dp. */
        const val MAX_LENGTH = 120

        /**
         * تنظيف النصّ من **منبعٍ واحد**: الأسطر الجديدة تصير مسافة (لا تراكب متعدّد الأسطر)،
         * والفراغات المتتالية تُطوى، والطول يُقصّ. والدالّة نقيّة كي تُقاس في JVM بلا جهاز.
         *
         * والقصّ لا يترك نصف زوج بديل (إيموجي في آخره): حرف بديل وحيد يُرسم مربّعًا فارغًا
         * على الشاشة، فيُسقط الحرف الأخير بدل أن يُعرض عطبًا.
         */
        fun sanitize(raw: String?): String {
            val flat = raw
                .orEmpty()
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim()
                .replace(Regex("\\s{2,}"), " ")
            val clipped = if (flat.length <= MAX_LENGTH) {
                flat
            } else {
                val head = flat.take(MAX_LENGTH)
                if (head.lastOrNull()?.isHighSurrogate() == true) head.dropLast(1) else head
            }
            return clipped.trim()
        }

        /**
         * حالة من نصّ خام + رغبة العرض. نصٌّ فارغ بعد التنظيف يعني **مذكّرة غير موجودة**،
         * فلا يُترك [visible] صحيحًا على شاشة فارغة.
         */
        fun of(raw: String?, visible: Boolean): GameMemo {
            val clean = sanitize(raw)
            return GameMemo(text = clean, visible = visible && clean.isNotEmpty())
        }
    }
}

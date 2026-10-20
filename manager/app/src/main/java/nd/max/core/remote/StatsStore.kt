/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import java.io.File

/**
 * **ذاكرة قناة الإحصاءات على القرص** — ملفّ نصّيّ صغير (ستّة أسطر) داخل `filesDir`.
 *
 * ولماذا ملفّ لا `SharedPreferences`: الحالة ملكُ القناة وحدها، وتُمسح بضغطة السحب كاملةً
 * ([clear])، ويجب أن تُختبر على JVM بلا سياق أندرويد — فكان الملفّ أصدق. والكتابة **ذرّيّة**
 * (مؤقّت ثم إعادة تسمية، نمط `ConfigStore` نفسه)، فلا يقرأ تشغيلٌ تالٍ ملفًّا نصفَ مكتوب.
 *
 * والقراءة متسامحة عمدًا: ملفّ مفقود أو تالف = حالة فارغة، لا عطب يُوقف التشغيل. أسوأ
 * تصرّف ممكن عند سوء الفهم هو إشارة زائدة تُحجب خادميًّا، لا تكرار أبديّ.
 */
class StatsStore(private val file: File) {

    /** حالة القناة — أو الافتراضيّة إن غاب الملفّ أو لم يُقرأ. */
    fun read(): StatsState {
        val lines = runCatching { file.readLines(Charsets.UTF_8) }.getOrNull() ?: return StatsState()
        val fields = lines.mapNotNull { line ->
            val at = line.indexOf('=')
            if (at <= 0) null else line.substring(0, at) to line.substring(at + 1)
        }.toMap()
        return StatsState(
            lastSentAt = fields["sent_at"]?.toLongOrNull() ?: 0,
            lastSentVersion = fields["sent_version"].orEmpty(),
            lastSentEnv = fields["sent_env"].orEmpty(),
            lastSentHealth = fields["sent_health"].orEmpty(),
            attempts = (fields["attempts"]?.toIntOrNull() ?: 0).coerceAtLeast(0),
            lastAttemptAt = fields["attempt_at"]?.toLongOrNull() ?: 0,
        )
    }

    /** يكتب الحالة ذرّيًّا؛ `false` تعني «لم تستقرّ على قرص» — تُسجَّل ولا تُخفي. */
    fun write(state: StatsState): Boolean {
        val text = buildString {
            append("sent_at=").append(state.lastSentAt).append('\n')
            append("sent_version=").append(state.lastSentVersion).append('\n')
            append("sent_env=").append(state.lastSentEnv).append('\n')
            // سطرٌ واحد دائمًا: أيّ محرف سطر يُطوى كي لا يُقرأ الملفّ سطرين في تشغيلٍ تالٍ.
            append("sent_health=").append(state.lastSentHealth.replace('\n', ' ')).append('\n')
            append("attempts=").append(state.attempts).append('\n')
            append("attempt_at=").append(state.lastAttemptAt).append('\n')
        }
        val temporary = File(file.parentFile, "${file.name}.tmp")
        return runCatching {
            file.parentFile?.mkdirs()
            temporary.writeText(text, Charsets.UTF_8)
            if (temporary.renameTo(file)) return@runCatching true
            if (!file.delete()) return@runCatching false
            temporary.renameTo(file)
        }.getOrDefault(false)
    }

    /** السحب: يُمسح السجلّ المحلّيّ كاملًا (ولا يبقى منه سطر). */
    fun clear(): Boolean = runCatching { !file.exists() || file.delete() }.getOrDefault(false)
}

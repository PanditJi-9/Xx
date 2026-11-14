/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * ألبوم المقاطع — **الجزء النقيّ** الذي يُقاس بلا جهاز.
 *
 * ### ولماذا نموذج قبل شاشة
 *
 * المقاطع تُنتَج فعلًا من لوحة اللعب (`PanelScreenRecorder` يكتب
 * `/sdcard/Movies/MaxManager/Max_*.mp4`)، ولا شيء داخل التطبيق يعرضها. فالفجوة ليست
 * «تسجيلًا» بل **مرآة** لما سُجّل. وقبل أن تُكتب الشاشة، تُكتب قواعدها هنا: الترتيب،
 * وصياغة الحجم والمدة، وحالات القراءة — لأن هذه هي التي تُخطئ بصمت، لا الرسم.
 *
 * ### والقاعدة الواحدة الموروثة من `FileHomeModel`
 *
 * **غياب الجواب ليس صفرًا.** `permitted=false` ليست «لا مقاطع» بل «لم يُسمح بالقراءة»،
 * و`read=false` ليست «لا مقاطع» بل «المعرّض لم يجب». والفرق هو نفسه الذي جعل
 * `ProcessSample.failed` نوعًا مستقلًّا: الصفر الكاذب يجعل المستخدم يظنّ أنه لم يسجّل.
 */
data class ClipEntry(
    /** معرّف `MediaStore` — يُبنى منه العنوان في الشاشة. */
    val id: Long,
    /** اسم الملفّ كما كتبه المسجّل، لا اسمًا نبنيه من جديد. */
    val name: String,
    /** الحجم بالبايت، أو `null` حين لم يُقرأ. */
    val sizeBytes: Long?,
    /** المدة بالمللي ثانية، أو `null` حين لم تُقرأ. */
    val durationMs: Long?,
    /** وقت الإضافة بالمللي ثانية، أو `null` حين لم يُقرأ — فيُرتَّب في الآخر لا في الصدر. */
    val addedAtMs: Long?,
)

/** حالات الألبوم الخمس — والفراغ ليس إحداها إلا حين يكون صفرًا حقيقيًّا. */
enum class ClipAlbumState { Loading, Ready, Empty, NoPermission, Unreadable }

object ClipAlbumMath {

    private const val KB = 1_024L
    private const val MB = 1_048_576L
    private const val GB = 1_073_741_824L

    /**
     * الأحدث أولًا، **وما لا وقت له في الآخر**.
     *
     * والاعتماد على الوقت لا على المعرّف وحده: إعادة تسمية أو نسخ ملفّ قد تُبقي معرّفًا
     * أحدث من وقته. والوقت المجهول لا يُعامَل صفرًا — الصفر وقتٌ حقيقي (1970) وكان سيقفز
     * فوق كل شيء أو تحته بلا سبب؛ فيُعزل في الذيل ثم يُرتَّب بالمعرّف.
     */
    fun sortNewestFirst(clips: List<ClipEntry>): List<ClipEntry> = clips.sortedWith(
        compareByDescending<ClipEntry> { it.addedAtMs ?: Long.MIN_VALUE }
            .thenByDescending { it.id }
    )

    /** حجم مقروء — والشرطة وحدها تعني «لم يُقرأ»، لا صفر بايت. */
    fun sizeText(bytes: Long?): String {
        if (bytes == null || bytes < 0L) return "—"
        return when {
            bytes >= GB -> "%.1f GB".format(bytes / GB.toDouble())
            bytes >= MB -> "%.1f MB".format(bytes / MB.toDouble())
            bytes >= KB -> "%.0f KB".format(bytes / KB.toDouble())
            else -> "$bytes B"
        }
    }

    /** مدة مقروءة `m:ss`، وبالساعات حين تتجاوزها — والمجهولة شرطة. */
    fun durationText(ms: Long?): String {
        if (ms == null || ms <= 0L) return "—"
        val totalSeconds = ms / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /**
     * الحالة من ثلاثة أسئلة مقيسة — لا من عدد الصفوف وحده.
     *
     * والترتيب مقصود: الإذن أوّلًا، فالقراءة، ثم العدد. فقائمة فارغة بلا إذن ليست
     * «لا مقاطع»، وقائمة فارغة بعد فشل قراءة ليست فراغًا.
     */
    fun stateOf(permitted: Boolean, read: Boolean, count: Int): ClipAlbumState = when {
        !permitted -> ClipAlbumState.NoPermission
        !read -> ClipAlbumState.Unreadable
        count <= 0 -> ClipAlbumState.Empty
        else -> ClipAlbumState.Ready
    }
}

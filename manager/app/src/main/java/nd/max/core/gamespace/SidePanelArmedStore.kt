/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.core.gamespace

import nd.max.MaxManagerPaths
import nd.max.core.hardware.RootFileAccess

/**
 * يكتب قائمة اللوحة الجانبية إلى الملف الذي يقرؤه الرفيق الجذري ([SidePanelArmed]).
 *
 * يُستدعى من الردهة عند كل تفعيل أو تعطيل، ومن بدء التطبيق مزامنةً (`SidePanelArmedStore`). ولا تُعاد الكتابة
 * إن كان المحتوى كما هو — **وتُقرأ النتيجة بعد الكتابة**، فلا يُعلن نجاح لم يصل.
 *
 * ### ولماذا «حصيلة» لا `Boolean`
 *
 * كان العائد `true/false` ويُرمى في الفراغ في الموضعين: تفشل الكتابة فيقرأ المستخدم المفتاح «مُفعَّلًا» في
 * الردهة بينما الملف الذي يقرؤه الرفيق الجذري **غير موجود** — فلا تُشغَّل اللوحة عند فتح اللعبة، ولا سطر
 * واحد في أي سجل يقول لماذا. وهي الحال المقيسة في حزمة سجل المستخدم (POCO X6 Pro · ٢٠٢٦-١٠-١٥):
 * `side_panel_armed.txt` غائب، ولا أثر لكلمة `SIDE_PANEL` في السجل كله.
 *
 * والفرق بين «رُفضت الكتابة» و«كُتبت ولم تُقرأ كما كُتبت» هو الفرق بين مشكلة صلاحية جذر ومشكلة
 * نظام ملفّات — ولذلك حالتان لا واحدة.
 */
object SidePanelArmedStore {

    /** كُتب المحتوى المطلوب، وقرأته القراءة العكسية كما هو. */
    const val REASON_WRITTEN = "written"

    /** الملف يحمل المطلوب أصلًا: لا كتابة، ولا ادّعاء كتابة. */
    const val REASON_UNCHANGED = "already-in-sync"

    /** الجذر رفض الكتابة (`atomicWriteText` رجع `false`). */
    const val REASON_WRITE_REFUSED = "write-refused"

    /** الكتابة رجعت بنجاح والقراءة العكسية لا تحمل المطلوب — لا يُقال «كُتب». */
    const val REASON_WRITE_UNVERIFIED = "write-unverified"

    /** لا لعبة مُفعَّلة: لا شيء يُوضع في الملف (وإن كان فيه محتوى قديم فقد أُفرغ). */
    const val REASON_EMPTY = "no-armed-games"

    /**
     * حصيلة المزامنة: هل صار الملف يحمل المطلوب، وبأيّ رمز، ولكم لعبة.
     *
     * و`ok` تعني «الملف الآن يحمل ما تقوله التفضيلات» — لا «نجح أمر الكتابة».
     */
    data class Outcome(
        val ok: Boolean,
        val reason: String,
        val packages: Int,
        val wrote: Boolean,
    ) {
        /** سطر سجل واحد يحمل الأربعة، فيُقرأ السبب بلا تخمين. */
        fun logDetail(): String = "reason=$reason packages=$packages wrote=$wrote ok=$ok"
    }

    fun sync(enabledPackages: Set<String>): Outcome {
        val wanted = SidePanelArmed.encode(enabledPackages)
        val count = SidePanelArmed.decode(wanted).size
        val current = RootFileAccess.read(MaxManagerPaths.SIDE_PANEL_ARMED).orEmpty()
        if (current.trim() == wanted.trim()) {
            return Outcome(ok = true, reason = if (count == 0) REASON_EMPTY else REASON_UNCHANGED, packages = count, wrote = false)
        }
        if (!RootFileAccess.atomicWriteText(MaxManagerPaths.SIDE_PANEL_ARMED, wanted)) {
            return Outcome(ok = false, reason = REASON_WRITE_REFUSED, packages = count, wrote = false)
        }
        // والقراءة العكسية شرط النجاح: «رجع الأمر بنجاح» ليس «وصل المحتوى» — والفرق يُخفى عن المستخدم
        // كلّه إن لم يُقرأ، لأن الملف يقرؤه غيره (الرفيق الجذري) في وقت لاحق بلا شاهد.
        val back = RootFileAccess.read(MaxManagerPaths.SIDE_PANEL_ARMED).orEmpty()
        if (back.trim() != wanted.trim()) {
            return Outcome(ok = false, reason = REASON_WRITE_UNVERIFIED, packages = count, wrote = true)
        }
        return Outcome(ok = true, reason = if (count == 0) REASON_EMPTY else REASON_WRITTEN, packages = count, wrote = true)
    }
}

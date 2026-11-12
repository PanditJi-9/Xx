/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.subscreens

import androidx.annotation.StringRes
import nd.max.R
import nd.max.core.gamespace.InAppKnob
import nd.max.core.gamespace.InAppRun
import nd.max.core.gamespace.InAppRunReason

/**
 * **ما تقوله الشاشة عن آخر تشغيل لبروفايل التطبيق.**
 *
 * والفصل هنا مقصود: القرار «أي صفّ يظهر وماذا يعني» دالّة صافية تُقاس في JVM، و`Composable`
 * يرسمها فقط. فلو تغيّر منطق التخطّي غدًا لا ينكسر العرض بصمت، ولو انكسر العرض لا يُتّهم المنطق.
 *
 * **وقاعدة واحدة تحكم الترتيب:** ما مُنع يُقال أوّلًا — لأن «لم يُطبَّق» خبر أهمّ من «طُبِّق».
 */
sealed interface InAppRunRow {
    /** مسار الأوامر غير جاهز: لم تُقرأ القيم أصلًا، فلا يُقال «طُبِّق» ولا «رُفض». */
    data class Blocked(val reason: InAppRunReason) : InAppRunRow

    /** كتابة جرت: كم كُتب من كم، وكم قُرئ مرتجًعًا. والاكتمال ليس افتراضًا. */
    data class Applied(
        val written: Int,
        val total: Int,
        val verified: Int,
        val complete: Boolean,
    ) : InAppRunRow

    /** مفتاح لم يُطبَّق وسببه مذكور. */
    data class Skipped(val knob: InAppKnob, val reason: InAppRunReason) : InAppRunRow

    /** لا مفتاح داخليّ مضبوط لهذا التطبيق — لا عطب ولا نجاح. */
    data object NothingAsked : InAppRunRow

    /** لا نتيجة محفوظة لهذا التطبيق بعد. */
    data object Never : InAppRunRow
}

/**
 * يحوّل المحفوظ إلى صفوف معروضة. `run == null` تعني «لا سجلّ لهذا التطبيق» لا «لا شيء مطلوب».
 * والترتيب: المحجوب، ثم المنفَّذ، ثم الرُّفض، ثم «لا شيء مضبوط».
 */
fun inAppRunRows(run: InAppRun?): List<InAppRunRow> {
    if (run == null) return listOf(InAppRunRow.Never)
    val rows = buildList<InAppRunRow> {
        when {
            run.blocked != null -> add(InAppRunRow.Blocked(InAppRunReason.of(run.blocked)))
            run.writes > 0 -> add(
                InAppRunRow.Applied(
                    written = run.written,
                    total = run.writes,
                    verified = run.verified,
                    complete = run.allVerified,
                ),
            )
            run.skipped.isEmpty() -> add(InAppRunRow.NothingAsked)
        }
        // ترتيب ثابت بالـenum: الخريطة تُحفظ بترتيبها، والشاشة لا تُعاد ترتيبها بالحظّ.
        InAppKnob.entries.forEach { knob ->
            val reason = run.skipped[knob] ?: return@forEach
            add(InAppRunRow.Skipped(knob, InAppRunReason.of(reason)))
        }
    }
    return rows
}

/** اسم المقبض كما يراه المستخدم، لا كما تسمّيه الشيفرة. */
@StringRes
fun knobLabelRes(knob: InAppKnob): Int = when (knob) {
    InAppKnob.DND -> R.string.profile_knob_dnd
    InAppKnob.REFRESH_RATE -> R.string.profile_knob_refresh
    InAppKnob.HAPTICS -> R.string.profile_knob_haptics
}

/** سبب التخطّي بجملة تُفهم: ماذا حدث، وماذا لم يُفعل. */
@StringRes
fun reasonTextRes(reason: InAppRunReason): Int = when (reason) {
    InAppRunReason.ZEN_UNREADABLE -> R.string.profile_run_reason_zen_unreadable
    InAppRunReason.ZEN_ALREADY_ON -> R.string.profile_run_reason_zen_already_on
    InAppRunReason.SNAPSHOT_UNSAFE -> R.string.profile_run_reason_snapshot_unsafe
    InAppRunReason.SNAPSHOT_UNREADABLE -> R.string.profile_run_reason_snapshot_unreadable
    InAppRunReason.ALREADY_OFF -> R.string.profile_run_reason_already_off
    InAppRunReason.LANE_UNAVAILABLE -> R.string.profile_run_reason_lane_unavailable
    InAppRunReason.UNKNOWN -> R.string.profile_run_reason_unknown
}

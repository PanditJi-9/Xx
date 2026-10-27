/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */
/*
 * الجزيرة الديناميكية — **نشاطان في وقت واحد**: الأساس والرفيق.
 *
 * ### ما كان
 *
 * الموسيقى كانت تُعرض بجانب المؤقّت والملاحة فقط، بقاعدة مكتوبة داخل الواجهة
 * (`mode is Live || mode is Timer`). فأيّ نشاط آخر (إشعار، مكالمة، تنزيل) كان يحجب الموسيقى
 * كلّيًا، ولو كان للشاشة متّسع. ولم يكن هناك طريق لتبديل أيّهما إلى الأساس.
 *
 * ### القاعدة
 *
 * الأساس هو الأعلى رتبةً، والرفيق هو الموسيقى إن لم تكن هي الأساس. ولمن ضغط الرفيق أن
 * يرقّيه إلى الأساس، فينزل الأساس السابق رفيقًا — تبديلٌ حقيقيّ لا زخرفة.
 *
 * والمكالمة لا تُنافَس ولا تُرقّى فوقها موسيقى: أزرار الردّ/الرفض لا يجوز أن تختفي.
 */
package nd.max.island.core

import nd.max.island.engine.IslandMode

enum class IslandActivity(val rank: Int) {
    Call(100),
    Notification(90),
    // الملاحة فوق التنزيل: تعليمة «انعطف الآن» تفوت إن انتظرت شريط تقدّم. والتنزيل فوق المؤقّت
    // لأنه ينتهي بنفسه ولا يحتاج انتباهًا.
    Navigation(80),
    Download(70),
    Tracking(65),
    Timer(60),
    Media(55),
    Charging(40),
    LowBattery(30),
    Device(20),
    Toggle(10),
    Idle(0),
}

fun activityOf(mode: IslandMode?): IslandActivity = when (mode) {
    null, IslandMode.Idle -> IslandActivity.Idle
    is IslandMode.Call -> IslandActivity.Call
    is IslandMode.Notification -> IslandActivity.Notification
    is IslandMode.Media -> IslandActivity.Media
    is IslandMode.Timer -> IslandActivity.Timer
    is IslandMode.Charging -> IslandActivity.Charging
    is IslandMode.LowBattery -> IslandActivity.LowBattery
    is IslandMode.Device -> IslandActivity.Device
    is IslandMode.Toggle -> IslandActivity.Toggle
    is IslandMode.Live -> when (mode.kind) {
        nd.max.island.engine.LiveKind.Download -> IslandActivity.Download
        nd.max.island.engine.LiveKind.Navigation -> IslandActivity.Navigation
        nd.max.island.engine.LiveKind.Tracking -> IslandActivity.Tracking
    }
}

data class IslandSelection(val primary: IslandMode?, val companion: IslandMode?)

object IslandActivityRules {
    /**
     * يختار الأساس والرفيق من الأنشطة الحيّة. [mediaPromoted] = المستخدم رقّى الموسيقى.
     *
     * والاختيار بالأولوية لا بترتيب الكتابة: `timer ?: live ?: download` القديم كان يجعل
     * مؤقّتًا قديمًا يحجب ملاحةً أهمّ. والترتيب هنا مقروء ومختبَر.
     */
    fun select(
        mediaPromoted: Boolean,
        timer: IslandMode.Timer?,
        live: IslandMode.Live?,
        download: IslandMode.Live?,
        media: IslandMode.Media?,
    ): IslandSelection {
        val main = listOfNotNull<IslandMode>(timer, live, download)
            .maxByOrNull { activityOf(it).rank }
        if (mediaPromoted && media != null) {
            return IslandSelection(primary = media, companion = main)
        }
        return IslandSelection(
            primary = main ?: media,
            companion = if (main != null) media else null,
        )
    }

    /**
     * رفيق النشاط المعروض الآن. لا رفيق لموسيقى (هي الأساس)، ولا لمكالمة (أزرارها لا تُزاحم)،
     * ولا لخمول. وهذا يُستدعى أيضًا حين يكون الأساس إشعارًا عابرًا: الموسيقى تبقى بجانبه.
     */
    fun companionFor(shown: IslandMode?, media: IslandMode.Media?): IslandMode? {
        if (media == null) return null
        return when (shown) {
            null, IslandMode.Idle, is IslandMode.Media, is IslandMode.Call -> null
            else -> media
        }
    }

    /** هل يمكن ترقية الرفيق؟ المكالمة استثناء صريح، ولا أساس = لا تبديل. */
    fun canPromote(primary: IslandMode?, companion: IslandMode?): Boolean =
        primary != null && companion != null && primary !is IslandMode.Call && primary !is IslandMode.Idle
}

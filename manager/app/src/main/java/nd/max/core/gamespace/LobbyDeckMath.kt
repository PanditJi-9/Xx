/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import kotlin.math.abs

/**
 * رياضيات «الحلقة» ثلاثية الأبعاد لبطاقات اللوبي — نقيّة تُقاس بلا جهاز. الرسم يمرّر [slot]
 * (مؤشّر البطاقة ناقص موضع التمرير الكسريّ: 0 للبطاقة المركزية، +1 التالية في اتجاه القراءة)
 * ويأخذ وضعيّةً جاهزة؛ الإشارة الفيزيائية (يمين/يسار في RTL) من شأن الرسم لا من شأن هذا الملف.
 */
object LobbyDeckMath {
    /** أبعد مسافة تُحسب بعمق؛ ما بعدها يبقى على آخر وضعيّة (وشفافيّتها قاربت الصفر). */
    const val REACH = 3f

    private const val MAX_TILT_DEGREES = 32f
    private const val BASE_SHRINK = 0.08f
    private const val DEPTH_SHRINK = 0.14f
    private const val FADE_PER_SLOT = 0.30f
    private const val TUCK_PER_SLOT_SQUARED = 0.13f

    /**
     * @property scale مقياس منتظم (1 للمركز).
     * @property alpha شفافيّة (1 للمركز، وتقارب 0 عند [REACH]).
     * @property rotationY درجات الميل؛ إشارتها إشارة [slot] (الخارجيّة تُقلب في الرسم).
     * @property tuck سحب البطاقة نحو المركز بنسبة من عرضها (≥ 0).
     */
    data class Pose(val scale: Float, val alpha: Float, val rotationY: Float, val tuck: Float)

    fun pose(slot: Float, depth: LobbyDepth): Pose {
        val a = abs(slot).coerceAtMost(REACH)
        val strength = depth.strength
        val sign = when {
            slot > 0f -> 1f
            slot < 0f -> -1f
            else -> 0f
        }
        val shrink = BASE_SHRINK + DEPTH_SHRINK * strength
        val t = a.coerceAtMost(1f)
        // smoothstep: المركز يبقى مسطّحًا تمامًا ويبلغ الميل كامله عند بُعد بطاقة واحدة.
        val eased = t * t * (3f - 2f * t)
        return Pose(
            scale = 1f / (1f + shrink * a),
            alpha = (1f - FADE_PER_SLOT * a).coerceIn(0f, 1f),
            rotationY = sign * MAX_TILT_DEGREES * strength * eased,
            tuck = strength * TUCK_PER_SLOT_SQUARED * a * a
        )
    }

    /** 1 عند المركز ويهبط خطّيًّا إلى 0 عند بُعد بطاقة فأكثر. */
    fun emphasis(slot: Float): Float = 1f - abs(slot).coerceIn(0f, 1f)

    /** ترتيب الرسم: الأقرب للمركز أعلى. */
    fun zOrder(slot: Float): Float = 100f - abs(slot).coerceAtMost(REACH) * 10f
}

/** ترتيب بطاقات الألعاب؛ يُجمّد ما كان منتشرًا في الشاشة ويقاس بلا جهاز. */
object LobbyOrder {
    fun sort(
        games: List<GameApp>,
        favorites: Set<String>,
        order: LobbySort,
        installedDays: Map<String, Int>
    ): List<GameApp> = when (order) {
        LobbySort.Favorites -> games.sortedWith(
            compareByDescending<GameApp> { it.packageName in favorites }
                .thenBy { it.label.lowercase() }
                .thenBy { it.packageName }
        )
        LobbySort.Name -> games.sortedWith(
            compareBy<GameApp> { it.label.lowercase() }.thenBy { it.packageName }
        )
        LobbySort.Newest -> games.sortedWith(
            compareBy<GameApp> { installedDays[it.packageName] ?: Int.MAX_VALUE }
                .thenBy { it.label.lowercase() }
                .thenBy { it.packageName }
        )
    }
}

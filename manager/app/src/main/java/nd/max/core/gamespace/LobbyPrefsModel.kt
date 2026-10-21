/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * عمق الـ3D في اللوبي. `strength` يضرب الميل والتقارب وانكماش الجارات؛ الصفر صفٌّ مسطّح بلا ميل.
 * الرياضيات نفسها في [LobbyDeckMath] والرسم لا يعرف هذه الأرقام.
 */
enum class LobbyDepth(val strength: Float) {
    Flat(0f),
    Soft(0.6f),
    Deep(1f)
}

/** ترتيب بطاقات الألعاب في اللوبي. */
enum class LobbySort { Favorites, Name, Newest }

/**
 * خيارات اللوبي المحفوظة (مظهر وسلوك). كلها تفضيلات عرض محلّية: لا واحد منها يكتب في العتاد
 * ولا يغيّر ملف لعبة. القيم الافتراضية هي ما يراه المستخدم أول مرة.
 */
data class LobbyPrefsState(
    val depth: LobbyDepth = LobbyDepth.Deep,
    /** `true`: ألوان اللوبي من أيقونة اللعبة المحدّدة؛ `false`: لون العلامة لكل الألعاب. */
    val gameColors: Boolean = true,
    /** زينة مستمرّة: طفو البطاقة وانجراف الضوء وتنفّس التوهّج. مُطفأة افتراضيًّا (ADR-12). */
    val ambient: Boolean = false,
    /** كبسولة الحالة (ساعة وبطارية وحرارة وذاكرة حرّة) أعلى المسرح. */
    val showStatus: Boolean = true,
    val sort: LobbySort = LobbySort.Favorites,
    /** وميض لحظيّ عند الإطلاق: زينة فلا تُفعَّل إلا بطلب صريح (ADR-12). */
    val launchFlash: Boolean = false,
    val haptics: Boolean = true,
    val keepScreenOn: Boolean = false,
    /** فتح اللوبي على آخر لعبة وُقف عليها بدل الأولى. */
    val resumeLast: Boolean = true,
    val lastPackage: String? = null
)

/** قراءة قيمة تعداد محفوظة نصًّا؛ المجهول أو التالف يعود للافتراضي لا يُسقط الشاشة. */
fun <T> lobbyEnumOr(raw: String?, values: List<T>, fallback: T): T where T : Enum<T> =
    values.firstOrNull { it.name == raw } ?: fallback

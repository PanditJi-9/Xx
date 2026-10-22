/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

/**
 * عقد اختصار «مساحة الألعاب» على الشاشة الرئيسية: أيقونة تفتح اللوبي مباشرة (فيبدأ بالدخول
 * السينمائيّ) من خارج التطبيق.
 *
 * ### كيف يصل الاختصار إلى اللوبي
 * نيّة صريحة إلى `MainActivity` تحمل `WidgetIntents.EXTRA_OPEN_ROUTE = ROUTE_GAME_LOBBY` — **المسار
 * نفسه الذي تستعمله الودجات وإشعار الجزيرة**، فلا قناة موازية: على نشاط قائم (`singleTask`) تصل عبر
 * `onNewIntent`، وعلى إطلاق جديد تُقرأ في `onCreate`، وفي الحالتين يترجمها `MainScreen` إلى
 * `MaxDestination.GameLobby` فتمرّ ببوابة `RemoteFlags.GAMESPACE` كأي دخول إلى اللوبي.
 *
 * ثوابت هذا الملف وحدها نقيّة (بلا Android) ليحرسها اختبار؛ وبناء الاختصار الفعليّ في
 * `GameSpaceShortcutPinner`.
 */
object GameSpaceShortcut {
    /**
     * معرّف الاختصار المثبَّت. **ثابت عبر الإصدارات عمدًا**: به يُعرف أنه مثبَّت فيُحدَّث بدل أن يتكرّر،
     * وتغييره يترك على شاشة المستخدم أيقونة يتيمة لا يُحدّثها أحد.
     */
    const val SHORTCUT_ID = "game_space_lobby"

    /** إجراء النيّة: `ShortcutInfo` يرفض نيّة بلا إجراء، وهذا اسمه الخاصّ بنا. */
    const val ACTION_OPEN_LOBBY = "nd.max.action.OPEN_GAME_LOBBY"
}

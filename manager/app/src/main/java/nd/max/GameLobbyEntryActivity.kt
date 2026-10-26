/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max

import android.app.Activity
import android.os.Bundle
import nd.max.ui.util.GameSpaceShortcutPinner

/**
 * مدخل أيقونة «لوبي الألعاب» **المستقلّة** في قائمة التطبيقات.
 *
 * ### لماذا وُجدت
 * تثبيت الاختصار اليدوي (`requestPinShortcut`) يرجع «نجح» حتى حين يرفض المشغّل وضع الأيقونة
 * (على HyperOS/MIUI إذن «اختصارات الشاشة الرئيسية» مغلق افتراضيًّا)، فيرى المستخدم رسالة ولا يرى
 * أيقونة. أيقونة تطبيق عاديّة (`LAUNCHER`) لا تحتاج هذا الإذن: تظهر في القائمة ويسحبها المستخدم إلى
 * الشاشة الرئيسية بنفسه كأي تطبيق.
 *
 * ### ما تفعله
 * لا شيء مرسوم: تُطلق [MainActivity] بنيّة اللوبي نفسها التي يستعملها الاختصار المثبَّت
 * ([GameSpaceShortcutPinner.lobbyIntent]) — فلا قناة موازية — ثم تنتهي في `onCreate` (شرط
 * `Theme.NoDisplay`: النهاية قبل `onResume`). والمكوّن **مُعطَّل افتراضيًّا** في المانيفست ويُفعَّله
 * المستخدم من خيارات اللوبي (`GameLobbyIcon`).
 */
class GameLobbyEntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { startActivity(GameSpaceShortcutPinner.lobbyIntent(this)) }
        finish()
    }
}

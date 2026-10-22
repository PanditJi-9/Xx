/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import nd.max.MainActivity
import nd.max.core.gamespace.GameSpaceShortcut
import nd.max.ui.component.LobbyPalette
import nd.max.ui.component.drawLobbyEmblem
import nd.max.widget.core.WidgetIntents

/** نتيجة طلب تثبيت اختصار اللوبي على الشاشة الرئيسية. */
enum class PinOutcome {
    /** طُلب من المشغّل؛ هو من يعرض مربّع التأكيد ويضع الأيقونة، ولا نعرف قراره. */
    Requested,

    /** مثبَّت من قبل: لم يُطلب تثبيت ثانٍ (يتكرّر الاختصار على بعض المشغّلات)، وحُدِّثت أيقونته واسمه. */
    AlreadyPinned,

    /** المشغّل الحاليّ لا يدعم تثبيت اختصارات. */
    Unsupported,

    /** رفض النظام الطلب أو فشل بناؤه. */
    Failed
}

/**
 * يبني اختصار «مساحة الألعاب» ويطلب تثبيته على الشاشة الرئيسية. لمس الأيقونة يفتح اللوبي مباشرة
 * (بنيّة `ROUTE_GAME_LOBBY` — العقد في [GameSpaceShortcut]) فيبدأ بالدخول السينمائيّ.
 *
 * لا إذن مطلوب: `requestPinShortcut` يمرّ بمربّع تأكيد المشغّل (لا تثبيت صامت)، وهو ما يميّزه عن
 * بثّ `INSTALL_SHORTCUT` القديم المعطَّل منذ أندرويد ٨.
 */
object GameSpaceShortcutPinner {
    /** لوحة الأيقونة التكيّفية: ١٠٨dp × ٤ = ٤٣٢px؛ والمنطقة الآمنة الوسطى ٦٦dp تتّسع للشعار. */
    private const val ICON_PX = 432

    /**
     * كثافة الرسم **٢ لا ٤**: [drawLobbyEmblem] تكتب سماكة خطوطها بالـdp (حافة ٣ وحرف M ٧)، وشعار الأيقونة
     * نصف حجمه في شاشة الدخول (٥٤dp بدل ١١٢dp). فنصف الكثافة يُبقي نسبة السماكة إلى القطر كما هي هناك؛
     * بالكثافة ٤ يتضاعف سمك الحرف فيلتحم عند منتصفه ويفقد الشعار هيئته. (قِيس على معاينة مرسومة بنفس المعادلات.)
     */
    private const val ICON_DENSITY = 2f

    /** هامش الشعار من كل جهة (٢٥٪): قطره ٥٤dp من ١٠٨ فيبقى داخل المنطقة الآمنة مهما قصّه المشغّل. */
    private const val EMBLEM_INSET = 0.25f

    /** هل يستطيع المشغّل الحاليّ تثبيت اختصارات؟ (لا يُبنى شيء ولا يُرسم.) */
    fun isSupported(context: Context): Boolean =
        context.applicationContext.getSystemService(ShortcutManager::class.java)
            ?.isRequestPinShortcutSupported == true

    /** نيّة الاختصار: صريحة إلى [MainActivity] بالإجراء الخاصّ ومسار اللوبي. */
    internal fun lobbyIntent(context: Context): Intent =
        Intent(context.applicationContext, MainActivity::class.java)
            .setAction(GameSpaceShortcut.ACTION_OPEN_LOBBY)
            .putExtra(WidgetIntents.EXTRA_OPEN_ROUTE, WidgetIntents.ROUTE_GAME_LOBBY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * يطلب تثبيت الاختصار باسم [label] (اسم مساحة الألعاب بلغة التطبيق الحاليّة). يُنادى من النقر على
     * الخيط الرئيسيّ؛ رسم الأيقونة مرّة واحدة على لوحة ٤٣٢px لا يُحسّ.
     */
    fun requestPin(context: Context, label: String): PinOutcome {
        val app = context.applicationContext
        val manager = app.getSystemService(ShortcutManager::class.java) ?: return PinOutcome.Unsupported
        if (!manager.isRequestPinShortcutSupported) return PinOutcome.Unsupported
        return runCatching {
            val info = ShortcutInfo.Builder(app, GameSpaceShortcut.SHORTCUT_ID)
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(Icon.createWithAdaptiveBitmap(renderIcon()))
                .setIntent(lobbyIntent(app))
                .build()
            if (manager.pinnedShortcuts.any { it.id == GameSpaceShortcut.SHORTCUT_ID }) {
                manager.updateShortcuts(listOf(info))
                PinOutcome.AlreadyPinned
            } else if (manager.requestPinShortcut(info, null)) {
                PinOutcome.Requested
            } else {
                PinOutcome.Failed
            }
        }.getOrDefault(PinOutcome.Failed)
    }

    /**
     * أيقونة الاختصار: خلفية كحليّة متدرّجة بتوهّج أزرق خلف **شعار الدخول نفسه** ([drawLobbyEmblem])،
     * مرسومة على لوحة خارج الشاشة. تكيّفية (`createWithAdaptiveBitmap`) فيقصّها المشغّل بشكله
     * (دائرة/مربّع مدوّر/قطرة) دون أن يُقطع الشعار.
     */
    private fun renderIcon() = ImageBitmap(ICON_PX, ICON_PX).also { image ->
        CanvasDrawScope().draw(
            Density(ICON_DENSITY),
            LayoutDirection.Ltr,
            Canvas(image),
            Size(ICON_PX.toFloat(), ICON_PX.toFloat())
        ) {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(LobbyPalette.Navy, LobbyPalette.NavyGlow),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            drawRect(
                Brush.radialGradient(
                    0f to LobbyPalette.Blue.copy(alpha = 0.40f),
                    1f to Color.Transparent,
                    center = center,
                    radius = size.minDimension * 0.55f
                )
            )
            inset(size.width * EMBLEM_INSET) { drawLobbyEmblem(LobbyPalette.Blue) }
        }
    }.asAndroidBitmap()
}

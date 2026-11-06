/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import nd.max.core.net.WifiChannel

/**
 * قراءة قنوات Wi-Fi كما تسمح بها المنصّة — والحدّ يُقال لا يُخفى.
 *
 * **ولماذا المنع حالةٌ صريحة لا استثناء:** أندرويد لا يعطي نتائج المسح إلا لتطبيق يحمل
 * `ACCESS_FINE_LOCATION` والموقع مفعَّل، مع أننا لا نطلب موقعًا ولا نحفظه ولا نرسله.
 * فغياب الإذن ليس خطأً يُبتلع: يُعرض سببه، ويُطلب الإذن عند ضغط المستخدم وحده.
 *
 * **ولا قناة تُخترع:** إن أعادت المنصّة تردّدًا لا يُطابق قناة معروفة، فهو لا يدخل العدّ
 * (`WifiChannel.summarise` يُسقطه) — الخريطة تقول ما رآه الجهاز، لا ما ينبغي أن يراه.
 */
object WifiChannelReader {

    enum class Blocker { NoPermission, LocationOff, WifiOff, Unreadable }

    data class State(val blocker: Blocker?, val summary: WifiChannel.Summary?)

    fun read(context: Context): State {
        val app = context.applicationContext
        if (app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return State(Blocker.NoPermission, null)
        }
        val wifi = app.getSystemService(WifiManager::class.java)
            ?: return State(Blocker.Unreadable, null)
        if (!wifi.isWifiEnabled) return State(Blocker.WifiOff, null)
        val locationOn = runCatching {
            app.getSystemService(LocationManager::class.java)?.isLocationEnabled ?: true
        }.getOrDefault(true)
        if (!locationOn) return State(Blocker.LocationOff, null)
        return runCatching {
            val connected = wifi.connectionInfo?.frequency?.takeIf { it > 0 }
            val scanned = wifi.scanResults.orEmpty().map { it.frequency }
            // بلا مسحٍ بعد، الشبكة المتّصل بها هي القراءة الوحيدة الصادقة — لا جدول فارغ.
            val frequencies = scanned.ifEmpty { listOfNotNull(connected) }
            State(null, WifiChannel.summarise(frequencies, connected))
        }.getOrElse { State(Blocker.NoPermission, null) }
    }
}

/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import nd.max.core.battery.BatteryHistoryStore

/**
 * Keeps the battery history alive across reboots and package updates.
 *
 * The receiver is deliberately non-exported: only the system's boot/unlock
 * broadcasts and the app's own explicit PendingIntent reach it. It does no
 * heavy work — one sticky-broadcast read and one asynchronous append.
 */
class BatterySampleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        when (intent?.action) {
            BatteryHistoryStore.ACTION_SAMPLE -> BatteryHistoryStore.recordNow(app)

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (BatteryHistoryStore.isEnabled(app)) {
                    BatteryHistoryStore.ensureScheduled(app)
                    BatteryHistoryStore.recordNow(app)
                }
            }
        }
    }
}

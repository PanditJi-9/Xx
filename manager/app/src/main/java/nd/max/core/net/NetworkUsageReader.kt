/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.net

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.NetworkCapabilities

/**
 * Per-app data usage, read from Android's own accounting.
 *
 * **What this is:** the bytes `NetworkStatsManager` reports for the requested
 * window, split by transport (Wi-Fi / mobile). Nothing is modelled, sampled or
 * extrapolated — the numbers are the platform's counters.
 *
 * **What this is not:** a live rate meter. The platform aggregates in buckets,
 * so the window must be closed (start..now) and the values move in steps.
 *
 * **Permission:** the same *Usage access* switch that [android.app.usage.UsageStatsManager]
 * needs. No new runtime permission is declared.
 *
 * **Honesty rules, and why each exists:**
 *  - a transport whose query throws is reported as *unavailable*, never as
 *    zero bytes — "no permission" and "no traffic" are different answers;
 *  - when the device reports a single `UID_ALL` bucket instead of per-app
 *    buckets, the total is still shown but per-app rows are suppressed and
 *    [NetworkUsageSnapshot.perAppAvailable] says so;
 *  - uids with no installed package (removed apps, system daemons) are counted
 *    in the transport total but cannot appear as rows.
 */
data class NetworkAppUsage(
    val uid: Int,
    val packageName: String,
    val label: String,
    val wifiBytes: Long,
    val mobileBytes: Long
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

/**
 * One reading of the platform's network counters.
 *
 * @param wifiBytes total bytes on the Wi-Fi transport, including uids that
 *   cannot be named.
 * @param mobileBytes total bytes on the mobile transport.
 * @param apps named packages only, ordered by total bytes descending.
 * @param wifiAvailable `false` when the query threw (no access or no support);
 *   the caller must show that state instead of `0 B`.
 * @param perAppAvailable `false` when the device collapsed every uid into a
 *   single bucket; the totals remain valid and the rows are intentionally empty.
 */
data class NetworkUsageSnapshot(
    val wifiBytes: Long,
    val mobileBytes: Long,
    val apps: List<NetworkAppUsage>,
    val wifiAvailable: Boolean,
    val mobileAvailable: Boolean,
    val perAppAvailable: Boolean
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

object NetworkUsageReader {

    /**
     * Queries both transports for `[startMs, endMs]`.
     *
     * Returns a failed [Result] only when the manager itself is missing or both
     * transports refused the query; a single unavailable transport is a
     * supported state of the snapshot, not an error.
     */
    fun read(context: Context, startMs: Long, endMs: Long): Result<NetworkUsageSnapshot> =
        runCatching {
            val manager = context.getSystemService(NetworkStatsManager::class.java)
                ?: error("NetworkStatsManager unavailable")

            val wifi = queryTransport(manager, NetworkCapabilities.TRANSPORT_WIFI, startMs, endMs)
            val mobile = queryTransport(manager, NetworkCapabilities.TRANSPORT_CELLULAR, startMs, endMs)

            if (wifi == null && mobile == null) {
                error("Network usage is not readable on this device")
            }

            val wifiUids = wifi?.bytesByUid.orEmpty()
            val mobileUids = mobile?.bytesByUid.orEmpty()
            val perAppAvailable = (wifi?.perUid ?: true) && (mobile?.perUid ?: true)

            val managerPackages = context.packageManager
            val apps = if (perAppAvailable) {
                buildList {
                    val uids = (wifiUids.keys + mobileUids.keys).toSet()
                    uids.forEach { uid ->
                        val packageName = managerPackages.packagesForUid(uid) ?: return@forEach
                        val label = managerPackages.labelOf(packageName)
                        val wifiBytes = wifiUids[uid] ?: 0L
                        val mobileBytes = mobileUids[uid] ?: 0L
                        if (wifiBytes + mobileBytes <= 0L) return@forEach
                        add(
                            NetworkAppUsage(
                                uid = uid,
                                packageName = packageName,
                                label = label,
                                wifiBytes = wifiBytes,
                                mobileBytes = mobileBytes
                            )
                        )
                    }
                }.sortedByDescending { it.totalBytes }
            } else {
                emptyList()
            }

            NetworkUsageSnapshot(
                wifiBytes = wifi?.totalBytes ?: 0L,
                mobileBytes = mobile?.totalBytes ?: 0L,
                apps = apps,
                wifiAvailable = wifi != null,
                mobileAvailable = mobile != null,
                perAppAvailable = perAppAvailable
            )
        }

    private data class TransportReading(
        val totalBytes: Long,
        val bytesByUid: Map<Int, Long>,
        val perUid: Boolean
    )

    /** `null` means the platform refused or does not support this transport. */
    private fun queryTransport(
        manager: NetworkStatsManager,
        transport: Int,
        startMs: Long,
        endMs: Long
    ): TransportReading? = runCatching {
        val stats = manager.querySummary(transport, null, startMs, endMs) ?: return null
        stats.use { networkStats ->
            val bucket = NetworkStats.Bucket()
            val byUid = HashMap<Int, Long>()
            var total = 0L
            var perUid = true
            while (networkStats.hasNextBucket()) {
                if (!networkStats.getNextBucket(bucket)) break
                val bytes = bucket.rxBytes + bucket.txBytes
                total += bytes
                when (bucket.uid) {
                    NetworkStats.Bucket.UID_ALL -> perUid = false
                    NetworkStats.Bucket.UID_REMOVED,
                    NetworkStats.Bucket.UID_TETHERING -> Unit
                    else -> if (bucket.uid >= 0) {
                        byUid[bucket.uid] = (byUid[bucket.uid] ?: 0L) + bytes
                    }
                }
            }
            TransportReading(totalBytes = total, bytesByUid = byUid, perUid = perUid)
        }
    }.getOrNull()

    private fun PackageManager.packagesForUid(uid: Int): String? {
        val packages = runCatching { getPackagesForUid(uid) }.getOrNull()
            ?: return null
        // An app may hold several packages under one uid; the first one that is
        // installed is the stable name we can show.
        return packages.firstOrNull { packageName ->
            runCatching { getApplicationInfo(packageName, 0) }.isSuccess
        }
    }

    private fun PackageManager.labelOf(packageName: String): String = runCatching {
        val info = getApplicationInfo(packageName, 0)
        getApplicationLabel(info).toString()
    }.getOrDefault(packageName)
}

/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Link quality, measured on demand and reported with its own limits.
 *
 * **What this is:** one name lookup and a few TCP connects to a single named
 * host, timed with `System.nanoTime`. The numbers answer "can this link reach
 * the internet right now, and how long does a connection take".
 *
 * **What this is not:**
 *  - an ICMP ping — Android cannot send one without root, so the times here are
 *    TCP handshakes and are systematically longer than a ping would be;
 *  - a bandwidth test — nothing is downloaded on purpose, because that would
 *    spend the user's data. Only the platform's own *estimate* from
 *    [NetworkCapabilities] is shown, and it is labelled as an estimate;
 *  - a background probe — [measure] runs only when the screen asks for it, and
 *    nothing is stored or sent to any other endpoint.
 *
 * **Why failures are values, not exceptions:** a timeout is the most useful
 * answer the screen can show ("this link cannot open a connection"), so failed
 * attempts are counted and the last exception's *class name* is reported. No
 * stack, no message, no address tree leaks into the UI.
 */
data class LinkReport(
    /** Host the probe connected to, as shown to the user before measuring. */
    val host: String,
    val port: Int,
    /** Name lookup time, or `null` when it timed out or failed. */
    val dnsMs: Long?,
    /** Successful TCP connect times in order; may be empty. */
    val connectsMs: List<Long>,
    /** Attempts that did not connect within the timeout. */
    val failures: Int,
    /** Total attempts made, successful or not. */
    val attempts: Int,
    /** Simple class name of the last failure, or `null`. */
    val lastFailure: String?,
    /** `NET_CAPABILITY_VALIDATED`, or `null` when the platform could not be asked. */
    val validated: Boolean?,
    /** `NET_CAPABILITY_CAPTIVE_PORTAL`, or `null` when the platform could not be asked. */
    val captivePortal: Boolean?,
    /** `NET_CAPABILITY_NOT_METERED` inverted; `null` when unknown. */
    val metered: Boolean?,
    /** Transport names the active network reports (Wi-Fi, cellular…). */
    val transports: List<String>,
    /** Platform estimate of downstream bandwidth in kbps, or `null`. */
    val downstreamKbps: Int?,
    /** Platform estimate of upstream bandwidth in kbps, or `null`. */
    val upstreamKbps: Int?
) {
    val bestMs: Long? get() = connectsMs.minOrNull()

    val averageMs: Long? get() = if (connectsMs.isEmpty()) null else connectsMs.sum() / connectsMs.size

    /**
     * Mean absolute difference between consecutive successful samples.
     *
     * Needs two successes by definition: one sample has no difference, and
     * reporting `0 ms` for it would claim a stability that was never measured.
     */
    val jitterMs: Long? get() {
        if (connectsMs.size < 2) return null
        val deltas = connectsMs.zipWithNext { first, second -> kotlin.math.abs(second - first) }
        return deltas.sum() / deltas.size
    }

    /** True when every attempt reached the host. */
    val allReached: Boolean get() = failures == 0 && connectsMs.isNotEmpty()
}

object LinkProbe {

    /** A reserved documentation domain served by IANA — no vendor product. */
    const val DEFAULT_HOST: String = "example.com"
    const val DEFAULT_PORT: Int = 443

    private const val CONNECT_TIMEOUT_MS = 2_000
    private const val DNS_TIMEOUT_MS = 3_000L

    /**
     * Measures the link now.
     *
     * @param samples how many TCP connects to attempt (clamped to 1..5).
     */
    suspend fun measure(
        context: Context,
        samples: Int = 4,
        host: String = DEFAULT_HOST,
        port: Int = DEFAULT_PORT
    ): LinkReport = withContext(Dispatchers.IO) {
        val attempts = samples.coerceIn(1, 5)
        val capabilities = activeCapabilities(context)

        var dnsMs: Long? = null
        val address = withTimeoutOrNull(DNS_TIMEOUT_MS) {
            val started = System.nanoTime()
            val resolved = runCatching { InetAddress.getByName(host) }.getOrNull()
            if (resolved != null) dnsMs = (System.nanoTime() - started) / 1_000_000L
            resolved
        }

        val times = ArrayList<Long>(attempts)
        var failures = 0
        var lastFailure: String? = null

        if (address != null) {
            repeat(attempts) {
                val started = System.nanoTime()
                val reached = runCatching {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(address, port), CONNECT_TIMEOUT_MS)
                    }
                }
                reached.exceptionOrNull()?.let { failure ->
                    failures++
                    lastFailure = failure.javaClass.simpleName
                }
                if (reached.isSuccess) {
                    times.add((System.nanoTime() - started) / 1_000_000L)
                }
            }
        } else {
            failures = attempts
            lastFailure = "UnknownHostException"
        }

        LinkReport(
            host = host,
            port = port,
            dnsMs = dnsMs,
            connectsMs = times,
            failures = failures,
            attempts = attempts,
            lastFailure = lastFailure,
            validated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            captivePortal = capabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL
            ),
            metered = capabilities?.let {
                !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            },
            transports = transportsOf(capabilities),
            downstreamKbps = capabilities?.linkDownstreamBandwidthKbps?.takeIf { it > 0 },
            upstreamKbps = capabilities?.linkUpstreamBandwidthKbps?.takeIf { it > 0 }
        )
    }

    private fun activeCapabilities(context: Context): NetworkCapabilities? {
        val manager = runCatching {
            context.getSystemService(ConnectivityManager::class.java)
        }.getOrNull() ?: return null
        val network = runCatching { manager.activeNetwork }.getOrNull() ?: return null
        return runCatching { manager.getNetworkCapabilities(network) }.getOrNull()
    }

    private fun transportsOf(capabilities: NetworkCapabilities?): List<String> {
        if (capabilities == null) return emptyList()
        val known = listOf(
            NetworkCapabilities.TRANSPORT_WIFI to "Wi-Fi",
            NetworkCapabilities.TRANSPORT_CELLULAR to "Cellular",
            NetworkCapabilities.TRANSPORT_ETHERNET to "Ethernet",
            NetworkCapabilities.TRANSPORT_BLUETOOTH to "Bluetooth",
            NetworkCapabilities.TRANSPORT_VPN to "VPN"
        )
        return known.filter { (transport, _) -> capabilities.hasTransport(transport) }
            .map { (_, name) -> name }
    }
}

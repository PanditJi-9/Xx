/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import nd.max.R
import nd.max.ui.design.MaxGroup
import nd.max.ui.design.MaxGroupDivider
import nd.max.ui.design.MaxRow
import nd.max.ui.design.MaxScreen
import nd.max.ui.design.MaxSection
import nd.max.ui.design.MaxSpace
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * فاحص الشبكة المحليّة — «ماذا تُظهر هذه الشبكة فعلًا»، لا «ماذا يمكن أن تُظهر».
 *
 * ثلاثة مصادر فقط، وكلّها مقروءة أو يبدأها المستخدم:
 * ١. `ConnectivityManager` + `LinkProperties`: عنوان هذا الجهاز وبادئته وبوّابته وخوادم
 *    الأسماء وواجهته وعنوانه الفيزيائي. لا شيء يُستنتج هنا؛ كل حقل يُقرأ أو يُعرض «—».
 * ٢. `/proc/net/arp` قراءةً فقط: الجيران الذين **تكلّم معهم هذا الجهاز** مؤخرًا. جدول
 *    النواة لا يُظهر الشبكة، بل يُظهر ما عرفه الجهاز — وهذا الفرق يُقال في الشاشة نفسها.
 * ٣. فحص اختياريّ يبدأه المستخدم: وصلات TCP على منافذ قليلة في /24. من ردّ يُذكر، ومن
 *    سكت **لا يُعدّ غائبًا**؛ فالمنافذ المفحوصة خمسة، والجهاز قد يكون حاضرًا على غيرها.
 *
 * ومحظور هنا ما لا نملكه: لا خام sockets، ولا التقاط حزم، ولا جذر، ولا منافذ مفتوحة
 * تُفتح بالنيابة عن المستخدم. الشاشة تقرأ وتعرض، وحدّها مكتوب في الشاشة لا في التعليق.
 */

private const val NsDash = "—"
private val NsPaneWidth = 304.dp

/**
 * أقلّ عرض يُترك للوحة النتائج حين يضيق الجهاز.
 *
 * الشاشة مبنيّة عمودين للعرض الأفقي، وفي الوضع الرأسي (٣٧٥dp) كان العمود الثابت
 * ٣٠٤dp يترك للنتائج نحو ١١dp، فتُرسم ملاحظة الفحص **حرفًا في كل سطر** (قيست على
 * جهاز المستخدم: ١٢٢٠×٢٧١٢ بكثافة ٥٢٠). فلا يُسمح للّوحة أن تأكل عرض النتائج.
 */
private val NsDetailMinWidth = 190.dp

/** وحدّ أدنى للوحة نفسها كي لا تُسحق بدورها على شاشة ضيّقة جدًّا. */
private val NsPaneMinWidth = 140.dp
private val NsProbePorts = listOf(22, 80, 443, 5000, 5555)
private const val NsProbeParallel = 24
private const val NsProbeTimeoutMs = 350
private const val NsResolveTimeoutMs = 400L

/** نوع الوصلة كما تعرفه المنصّة، لا كما نخمّنه من العنوان. */
internal enum class LanTransport { Wifi, Ethernet, Cellular, Other }

internal data class LanLink(
    val interfaceName: String,
    val address: String,
    val prefix: Int,
    val gateway: String?,
    val dns: List<String>,
    val mac: String?,
    val transport: LanTransport,
)

internal data class LanNeighbour(
    val ip: String,
    val mac: String,
    val interfaceName: String,
    val host: String? = null,
    val ports: List<Int> = emptyList(),
)

@Suppress("DEPRECATION")
private fun readLink(context: Context): LanLink? {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
    val network = manager.activeNetwork ?: return null
    val props = manager.getLinkProperties(network) ?: return null
    val caps = manager.getNetworkCapabilities(network)
    val v4 = props.linkAddresses.firstOrNull { it.address is Inet4Address } ?: return null
    val iface = props.interfaceName ?: return null
    val mac = runCatching {
        NetworkInterface.getByName(iface)?.hardwareAddress
            ?.joinToString(":") { "%02X".format(it) }
    }.getOrNull()
    val transport = when {
        caps == null -> LanTransport.Other
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> LanTransport.Wifi
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> LanTransport.Ethernet
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> LanTransport.Cellular
        else -> LanTransport.Other
    }
    return LanLink(
        interfaceName = iface,
        address = v4.address.hostAddress ?: return null,
        prefix = v4.prefixLength,
        gateway = props.routes.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress,
        dns = props.dnsServers.mapNotNull { it.hostAddress },
        mac = mac,
        transport = transport,
    )
}

private fun readArp(): List<LanNeighbour> {
    val file = File("/proc/net/arp")
    if (!file.canRead()) return emptyList()
    return runCatching {
        file.readLines().drop(1).mapNotNull { line ->
            val cells = line.trim().split(Regex("\\s+"))
            if (cells.size < 6) return@mapNotNull null
            val ip = cells[0]
            val flags = cells[2]
            val mac = cells[3]
            val iface = cells[5]
            if (flags == "0x0") return@mapNotNull null
            if (mac.length < 11 || mac == "00:00:00:00:00:00") return@mapNotNull null
            LanNeighbour(ip = ip, mac = mac.uppercase(), interfaceName = iface)
        }.distinctBy { it.ip }
    }.getOrDefault(emptyList())
}

private fun hostsInSubnet(link: LanLink): List<String> {
    if (link.prefix < 24) return emptyList()
    val parts = link.address.split(".")
    if (parts.size != 4) return emptyList()
    val base = parts.take(3).joinToString(".")
    return (1..254).map { "$base.$it" }.filter { it != link.address }
}

private fun probeHost(ip: String): List<Int> = NsProbePorts.filter { port ->
    runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, port), NsProbeTimeoutMs)
        }
        true
    }.getOrDefault(false)
}

private suspend fun resolveHost(ip: String): String? = withTimeoutOrNull(NsResolveTimeoutMs) {
    withContext(Dispatchers.IO) {
        runCatching {
            val name = InetAddress.getByName(ip).canonicalHostName
            if (name.isNullOrBlank() || name == ip) null else name
        }.getOrNull()
    }
}

private fun mergePorts(base: List<LanNeighbour>, found: Map<String, List<Int>>): List<LanNeighbour> {
    val byIp = base.associateBy { it.ip }.toMutableMap()
    found.forEach { (ip, ports) ->
        val existing = byIp[ip]
        byIp[ip] = if (existing == null) {
            LanNeighbour(ip = ip, mac = NsDash, interfaceName = NsDash, ports = ports)
        } else {
            existing.copy(ports = ports)
        }
    }
    return byIp.values.sortedWith(compareBy({ it.ip.substringAfterLast('.').toIntOrNull() ?: 0 }))
}

@Composable
fun NetworkScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf<LanLink?>(null) }
    var neighbours by remember { mutableStateOf<List<LanNeighbour>>(emptyList()) }
    var names by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var scanning by remember { mutableStateOf(false) }
    var scanned by remember { mutableIntStateOf(0) }
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(refreshKey) {
        names = emptyMap()
        val snapshot = withContext(Dispatchers.IO) { readLink(context) to readArp() }
        link = snapshot.first
        neighbours = snapshot.second
        val resolved = snapshot.second.map { it.ip }
            .map { ip -> async { ip to resolveHost(ip) } }
            .awaitAll()
            .mapNotNull { (ip, name) -> name?.let { ip to it } }
            .toMap()
        names = resolved
    }

    val current = link
    val total = remember(current) { current?.let { hostsInSubnet(it).size } ?: 0 }

    MaxScreen(
        title = stringResource(R.string.network_scanner_title),
        subtitle = stringResource(R.string.network_scanner_subtitle),
        onBack = { navController.popBackStack() },
        accentIcon = Icons.Rounded.Wifi,
        accent = GameSpacePalette.VioletSoft,
        scrollEnabled = false,
    ) {
        // العمود الأيسر عرضه ثابت في التصميم الأفقي، وفي الرأسي يحسب من عرض الشاشة
        // كي تبقى لوحة النتائج قابلة للقراءة (انظر [NsDetailMinWidth]).
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val paneWidth = NsPaneWidth
            .coerceAtMost(screenWidth - MaxSpace.lg * 3 - NsDetailMinWidth)
            .coerceAtLeast(NsPaneMinWidth)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaxSpace.lg),
            horizontalArrangement = Arrangement.spacedBy(MaxSpace.lg),
        ) {
            Column(
                modifier = Modifier
                    .width(paneWidth)
                    .fillMaxHeight()
                    .padding(vertical = MaxSpace.sm),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.md),
            ) {
                if (current == null) {
                    MaxGroup {
                        MaxRow(title = stringResource(R.string.ns_no_network))
                    }
                } else {
                    MaxGroup {
                        MaxRow(
                            title = stringResource(R.string.ns_interface),
                            subtitle = current.interfaceName,
                            trailing = {
                                Text(
                                    text = stringResource(
                                        when (current.transport) {
                                            LanTransport.Wifi -> R.string.ns_transport_wifi
                                            LanTransport.Ethernet -> R.string.ns_transport_ethernet
                                            LanTransport.Cellular -> R.string.ns_transport_cellular
                                            LanTransport.Other -> R.string.ns_transport_other
                                        }
                                    ),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                        MaxGroupDivider()
                        MaxRow(
                            title = stringResource(R.string.ns_address),
                            subtitle = "${current.address}/${current.prefix}",
                        )
                        MaxGroupDivider()
                        MaxRow(
                            title = stringResource(R.string.ns_gateway),
                            subtitle = current.gateway ?: NsDash,
                        )
                        MaxGroupDivider()
                        MaxRow(
                            title = stringResource(R.string.ns_dns),
                            subtitle = if (current.dns.isEmpty()) NsDash else current.dns.joinToString(", "),
                        )
                        MaxGroupDivider()
                        MaxRow(
                            title = stringResource(R.string.ns_mac),
                            subtitle = current.mac ?: NsDash,
                        )
                    }
                }

                MaxSection(title = stringResource(R.string.ns_sources_title)) {
                    Text(
                        text = stringResource(R.string.ns_sources_body),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = GameSpacePalette.Muted,
                        modifier = Modifier.padding(bottom = MaxSpace.sm),
                    )
                    TextButton(onClick = { refreshKey++ }) {
                        Text(stringResource(R.string.ns_refresh))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(vertical = MaxSpace.sm),
                verticalArrangement = Arrangement.spacedBy(MaxSpace.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaxSpace.sm),
                ) {
                    TextButton(
                        enabled = current != null && !scanning && total > 0,
                        onClick = {
                            val host = current ?: return@TextButton
                            if (scanning) return@TextButton
                            scanning = true
                            scanned = 0
                            neighbours = mergePorts(neighbours, emptyMap())
                            job = scope.launch {
                                val hosts = hostsInSubnet(host)
                                val found = LinkedHashMap<String, List<Int>>()
                                var index = 0
                                while (index < hosts.size) {
                                    val slice = hosts.subList(index, minOf(index + NsProbeParallel, hosts.size))
                                    val answers = coroutineScope {
                                        slice.map { ip -> async(Dispatchers.IO) { ip to probeHost(ip) } }.awaitAll()
                                    }
                                    answers.forEach { (ip, ports) ->
                                        if (ports.isNotEmpty()) found[ip] = ports
                                    }
                                    scanned = minOf(index + slice.size, hosts.size)
                                    neighbours = mergePorts(neighbours, found.toMap())
                                    index += slice.size
                                }
                                scanning = false
                                val resolved = found.keys
                                    .map { ip -> async { ip to resolveHost(ip) } }
                                    .awaitAll()
                                    .mapNotNull { (ip, name) -> name?.let { ip to it } }
                                    .toMap()
                                names = names + resolved
                            }
                        },
                    ) {
                        Text(stringResource(R.string.ns_probe))
                    }
                    if (scanning) {
                        TextButton(onClick = { job?.cancel(); scanning = false }) {
                            Text(stringResource(R.string.ns_probe_stop))
                        }
                    }
                }
                if (scanning) {
                    Column(verticalArrangement = Arrangement.spacedBy(MaxSpace.xs)) {
                        Text(
                            text = stringResource(R.string.ns_probe_progress, scanned, total),
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        )
                        LinearProgressIndicator(
                            progress = { if (total == 0) 0f else scanned.toFloat() / total.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.ns_probe_note),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = GameSpacePalette.Muted,
                )
                Spacer(Modifier.height(MaxSpace.xs))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.ns_neighbours),
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    )
                }
                Text(
                    text = stringResource(R.string.ns_neighbours_note),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = GameSpacePalette.Muted,
                )
                if (neighbours.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ns_neighbours_empty),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = GameSpacePalette.Muted,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(MaxSpace.xs),
                    ) {
                        items(neighbours, key = { it.ip }) { item ->
                            val name = names[item.ip]
                            MaxRow(
                                title = item.ip,
                                subtitle = buildString {
                                    append(item.mac)
                                    if (name != null) append(" · ").append(name)
                                },
                                trailing = if (item.ports.isEmpty()) {
                                    null
                                } else {
                                    {
                                        Text(
                                            text = stringResource(R.string.ns_ports_value, item.ports.joinToString(" ")),
                                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

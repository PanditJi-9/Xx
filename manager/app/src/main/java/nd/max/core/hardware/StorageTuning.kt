/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

/** Physical block devices only: stacked dm/loop/zram devices are not guessed to be storage targets. */
object StorageTuning {
    const val ROOT = "/sys/block"
    private val DEVICE = Regex("(?:mmcblk\\d+|sd[a-z]+|nvme\\d+n\\d+|vd[a-z]+)")

    data class Device(val name: String, val removable: Boolean?, val controls: List<NodeControl>)

    fun discover(io: NodeTuning.Io = NodeTuning.SystemIo): List<Device> =
        io.listNames(ROOT).filter { DEVICE.matches(it) }.distinct().sorted().take(32).mapNotNull { name ->
            val queue = "$ROOT/$name/queue"
            val controls = buildList {
                val schedulers = parseSchedulers(io.read("$queue/scheduler"))
                if (schedulers != null) {
                    add(NodeControl(
                        key = HardwareControlKey.storageQueue(name, "scheduler"),
                        path = "$queue/scheduler",
                        label = "scheduler",
                        value = schedulers.second,
                        format = NodeValueFormat.Choice(schedulers.first, bracketedRead = true),
                    ))
                }
                val fields = listOf(
                    "read_ahead_kb" to NodeValueFormat.Number(0, 4096, 128),
                    "nr_requests" to NodeValueFormat.Number(4, 1024, 16),
                    "rq_affinity" to NodeValueFormat.Number(0, 2),
                )
                for ((field, format) in fields) {
                    val value = format.normalise(io.read("$queue/$field")) ?: continue
                    add(NodeControl(HardwareControlKey.storageQueue(name, field), "$queue/$field", field, value, format))
                }
            }
            if (controls.isEmpty()) null else Device(
                name,
                removable = when (io.read("$ROOT/$name/removable")?.trim()) { "0" -> false; "1" -> true; else -> null },
                controls = controls,
            )
        }

    fun parseSchedulers(raw: String?): Pair<List<String>, String>? {
        val text = raw?.trim() ?: return null
        if (text.isEmpty() || text.length > 256) return null
        val tokens = text.split(Regex("\\s+"))
        if (tokens.size > 32) return null
        val active = tokens.filter { it.startsWith('[') && it.endsWith(']') }
        if (active.size != 1) return null
        val choices = tokens.map { it.removePrefix("[").removeSuffix("]") }
        if (choices.any { !it.matches(Regex("[A-Za-z0-9_-]{1,32}")) } || choices.distinct().size != choices.size) return null
        return choices to active.single().removeSurrounding("[", "]")
    }
}

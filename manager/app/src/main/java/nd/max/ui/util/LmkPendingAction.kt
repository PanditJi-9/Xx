/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

/** يحمل الطلب نفسه قبل فتح الإقرار؛ فالإقرار السابق قد ينفّذ فورًا بلا إعادة تركيب للواجهة. */
internal class LmkPendingAction {
    data class Request(val target: List<Int>, val expected: List<Int>)

    private var pending: Request? = null

    fun set(target: List<Int>, expected: List<Int>) {
        pending = Request(target.toList(), expected.toList())
    }

    fun take(): Request? = pending.also { pending = null }
}

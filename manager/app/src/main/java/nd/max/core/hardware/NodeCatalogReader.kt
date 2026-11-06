/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import java.io.File

/**
 * قراءة كتالوج العُقد — **قراءة فقط**، وبلا صلاحية جذر.
 *
 * **ولماذا `root` معامل:** الاختبار يحتاج أن يقيس الحالات الثلاث (موجودة · غائبة · غير مقروءة) على
 * شجرة مؤقّتة، فلا يعتمد على جهاز المصدر ولا على نواة معيّنة. والاستعمال الحقيقي يمرّر `/`.
 *
 * **والحدّ الصادق:** أكثر هذه العُقد **لا يُقرأ لتطبيق عاديّ** على أندرويد حديث (SELinux)، فترجع
 * `UNREADABLE` لا صفرًا. وهذه هي الفائدة المقصودة: أن تعرف أيّ عقدة **موجودة** (فتصلح للكتابة لاحقًا
 * عبر الـarbiter) وأيّها لا، قبل أن يُبنى محرّك وصفات على مسار غير موجود.
 */
object NodeCatalogReader {

    enum class State { PRESENT, ABSENT, UNREADABLE }

    /**
     * حالة عقدة واحدة. [value] يبقى `null` إلا في `PRESENT` — ولا يُختلق نصّ بديل.
     */
    data class Reading(val node: NodeCatalog.Node, val state: State, val value: String? = null)

    /** مجاميع القراءة. `present` هو ما يمكن أن يُكتب إليه لاحقًا، لا ما كُتب. */
    data class Summary(val total: Int, val present: Int, val absent: Int, val unreadable: Int) {
        val usable: Int get() = present
    }

    fun read(
        root: File = File("/"),
        nodes: List<NodeCatalog.Node> = NodeCatalog.nodes,
    ): List<Reading> = nodes.map { node -> readOne(root, node) }

    private fun readOne(root: File, node: NodeCatalog.Node): Reading {
        val file = File(root, node.path.trimStart('/'))
        if (!file.exists()) return Reading(node, State.ABSENT)
        // مجلّد أو مقبض خاصّ ليس عقدة قابلة للقراءة: لا نقرأ ولا نفترض.
        if (!file.isFile) return Reading(node, State.UNREADABLE)
        val text = runCatching { file.bufferedReader().use { it.readLine() } }.getOrNull()
            ?.trim()
            .orEmpty()
        return if (text.isEmpty()) Reading(node, State.UNREADABLE) else Reading(node, State.PRESENT, text)
    }

    fun summarise(readings: List<Reading>): Summary = Summary(
        total = readings.size,
        present = readings.count { it.state == State.PRESENT },
        absent = readings.count { it.state == State.ABSENT },
        unreadable = readings.count { it.state == State.UNREADABLE },
    )
}

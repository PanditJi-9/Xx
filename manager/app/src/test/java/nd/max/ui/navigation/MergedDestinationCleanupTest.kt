/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **دمج شاشتين لا ينتهي بحذف إحداهما.**
 *
 * `BatteryDetail` كان وجهة كاملة: مسار في الرسم، وسطر في جدول الأدوار، ودالة
 * `BatteryDetailScreen`، ونموذج بيانات خاصّ (`private data class BatteryDetail`)، وحلقة قراءة
 * خاصة به. ودمجُه في شاشة الشحن يعني أن **كل واحد من هذه** يجب أن يختفي، لا أن يبقى مسارًا
 * لا يفتحه أحد أو صنفًا لا يُبنى منه شيء.
 *
 * وهذا صنف عطب لا يكشفه شيء في المستودع: الشاشة المحذوفة لا تكسر البناء (المصرّف لا يشتكي من
 * صنف **غير** مستعمل)، ولا تكشفه البوابة (`code_health` تفحص الأصول المتقاطعة لا الموتى)، ولا
 * يُرى في التطبيق (ما لا مدخل له لا يُضغط).
 *
 * فالفحص هنا **بين-ملفّي**: يقابل السجل بما بقي مذكورًا في الكود.
 */
class MergedDestinationCleanupTest {

    /** الأثر الذي يجب ألّا يبقى بعد دمج «البطارية» في «الشحن». */
    private val mergedAway = listOf(
        "BatteryDetail",          // اسم الوجهة والصنف ودالة الشاشة
        "battery_detail",         // معرّف المسار
        "loadBatteryDetail",      // قارئ البطارية المنفصل
    )

    /**
     * علامةُ كل ملفّ تعريفٍ **بصيغته الصحيحة**: السجل يعرّف الوجهة (`data object StorageCare`)،
     * والرسم وجدول الأدوار يشيران إليها بالاسم المؤهَّل (`MaxDestination.StorageCare`).
     *
     * وفرضُ صيغةٍ واحدة على الثلاثة عطبٌ مقيس (CI · 2026-10-16): السجل كان **سليمًا** — التعريف
     * فيه إعلانٌ لا إشارة — فسقط الفحص على ملفّ سليم. عطبُ الفحص يُصلَح في الفحص.
     */
    private val registryMarkers = mapOf(
        "MaxDestinations.kt" to "data object StorageCare",
        "MaxNavGraph.kt" to "MaxDestination.StorageCare",
        "MaxDestinationCatalog.kt" to "MaxDestination.StorageCare",
    )

    /**
     * الملفات التي **تعريف** الوجهات فيها: السجل والرسم وجدول الأدوار.
     * ذِكر الاسم فيها مشروع؛ الممنوع أن يبقى في ملفّ **آخر**.
     * وتُشتقّ من [registryMarkers] فلا يفترق «ملفّ التعريف» عن علامته.
     */
    private val registryFiles = registryMarkers.keys

    private companion object {
        val BLOCK_COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
    }

    private fun sourceRoot(): File? {
        val candidates = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
            File("manager/app/src/main/java"),
        )
        candidates.firstOrNull { it.isDirectory }?.let { return it }

        var dir: File? = File("").absoluteFile
        repeat(6) {
            val current = dir ?: return null
            listOf("app/src/main/java", "manager/app/src/main/java")
                .map { File(current, it) }
                .firstOrNull { it.isDirectory }
                ?.let { return it }
            dir = current.parentFile
        }
        return null
    }

    @Test
    fun `الأصول موجودة فلا يمرّ اختبار فارغ`() {
        val root = sourceRoot()
        assertTrue("تعذّر العثور على جذر المصادر — اختبار لا يفحص شيئًا لا يثبت شيئًا", root != null)
        assertTrue(
            "لم يُعثر على ملفّات التعريف — الفحص نفسه معطوب",
            root!!.walkTopDown().any { it.name in registryFiles },
        )
    }

    @Test
    fun `لا يبقى أثر للوجهة المدموجة خارج ملفّات التعريف`() {
        val root = sourceRoot()!!
        val leftover = mutableListOf<String>()

        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name !in registryFiles }
            .forEach { file ->
                // التعليقات تُنزع أولًا: الشرح الذي يقول «هذه الشاشة دُمجت ولم تعد موجودة»
                // توثيق صحيح، وليس كودًا ميتًا. الفحص يبحث عن الكود لا عن الأثر المكتوب.
                val code = stripComments(file.readText())
                mergedAway.forEach { token ->
                    if (code.contains(token)) leftover += "${token} -> ${file.name}"
                }
            }

        assertTrue(
            "أثر لوجهة دُمجت وما زال في الكود (مسار أو نموذج أو مكوّن ميت): $leftover",
            leftover.isEmpty(),
        )
    }

    /** كتل `/* */` ثم أسطر `//` — بلا لمس `https://` في الروابط. */
    private fun stripComments(source: String): String =
        source
            .replace(BLOCK_COMMENT, "")
            .lineSequence()
            .joinToString("\n") { line ->
                val cut = Regex("(?<!:)//").find(line)
                if (cut == null) line else line.substring(0, cut.range.first)
            }

    @Test
    fun `السجل نفسه لا يعلن الوجهة المدموجة`() {
        val root = sourceRoot()!!
        val declared = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.isFile && it.name in registryFiles }
            .forEach { file ->
                val text = file.readText()
                if (text.contains("MaxDestination.BatteryDetail")) declared += file.name
            }

        assertTrue(
            "السجل ما زال يعلن وجهة دُمجت: $declared",
            declared.isEmpty(),
        )
    }

    /**
     * **وإعادة التسمية عطبٌ من النوع نفسه (`STORAGE-CARE-01`):** «التنظيف الفائق» صار «العناية
     * بالتخزين»، وبقاء الاسم القديم — صنفًا (`UltraClean`) أو مسارًا (`ultra_cleaner`) — يعني
     * إمّا كودًا ميتًا لا يراه المصرّف، وإمّا مسارًا لا يفتحه أحد.
     *
     * والفرق عن الدمج: هنا **لا موضع مشروع** للاسم القديم. لذلك يُفحص كل ملفّ Kotlin في
     * المصادر، **وملفّات التعريف معها** — فالسجل نفسه أُعيدت تسميته أيضًا. والتعليقات تُنزع
     * كسابقها: «كان `UltraCleanEngine`» توثيق صحيح لا أثر كود.
     */
    @Test
    fun `لا يبقى اسم التنظيف الفائق القديم في أي ملفّ Kotlin`() {
        val root = sourceRoot()!!
        val leftovers = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val code = stripComments(file.readText())
                if (code.contains("UltraClean")) leftovers += "UltraClean -> ${file.name}"
                if (code.contains("ultra_cleaner")) leftovers += "ultra_cleaner -> ${file.name}"
            }

        assertTrue(
            "اسم قديم ما زال في الكود (صنف أو مسار أو مفتاح): $leftovers",
            leftovers.isEmpty(),
        )
    }

    /** والاسم الجديد **موجود**، وإلا لكان الفحص أعلاه يمرّ على مستودع فارغ. */
    @Test
    fun `الاسم الجديد معلن في السجل وفي الرسم`() {
        val root = sourceRoot()!!
        val declared = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.isFile && it.name in registryFiles }
            .forEach { file ->
                // التعليقات تُنزع كسابقها: الإعلان المطلوب كودٌ لا شرح.
                val code = stripComments(file.readText())
                if (code.contains(registryMarkers.getValue(file.name))) declared += file.name
            }

        assertTrue(
            "الوجهة الجديدة غير معلنة في كل السجل والرسم: $declared",
            declared.containsAll(registryMarkers.keys),
        )
    }
}

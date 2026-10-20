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
package nd.max.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * المانيفست **المنشور فعلًا** (`docs/update/manifest.json`) — الملفّ الذي يستضيفه المالك،
 * ويقرؤه تطبيقٌ مثبَّت على أجهزة لا نراها.
 *
 * وحرّاس المحرّك (`UpdateManifestTest`) تقيس العقد على نصوصٍ داخل الاختبار؛ وهذه تقيس **الملفّ
 * الحقيقي**: ما يمرّ من هنا ملفٌّ لا يستطيع التطبيق أن يرفضه.
 *
 * والقاعدة الجوهرية: **ما أُعلن إصدارًا قابلًا للتنزيل لا بدّ أن يكون كاملًا** — رقمٌ ورابط
 * `https` وبصمةٌ من ٦٤ خانة سِت عشرية وحجمٌ أكبر من صفر. وتلك القاعدة هي التي تمنع زرّ تحديث
 * يقود إلى ملفّ نصفه فارغ: الملفّ الناقص **يجوز وجوده** (المالك يجهّز إصدارًا)، لكنه لا يُقرأ
 * إصدارًا — وهذا نصّ `UpdateRelease.usable` نفسه، لا قاعدة موازية له.
 *
 * وتُتخطّى الحرّاسة (لا تفشل) إن لم يكن الملفّ في هذه الشجرة، على قاعدة `HomeShapeContractTest`.
 */
class UpdatePublishedManifestTest {

    private lateinit var manifestFile: File

    @Before
    fun locateManifest() {
        val candidates = listOf(
            File("docs/update/manifest.json"),
            File("../docs/update/manifest.json"),
            File("../../docs/update/manifest.json"),
        )
        val found = candidates.firstOrNull { it.isFile }
        assumeTrue("لا docs/update/manifest.json في هذه الشجرة — الحرّاسة غير مُقيَّمة", found != null)
        manifestFile = found!!
    }

    private fun manifest(): UpdateManifest {
        val parsed = UpdateManifests.parse(manifestFile.readText())
        assertNotNull("المانيفست المنشور لا يُقرأ بالعقد نفسه (ولا نموذج نصفه أصفار)", parsed)
        return parsed!!
    }

    /** كل إصدار معلَن في الملفّ: `latest` وكل قناة. */
    private fun releases(m: UpdateManifest): List<Pair<String, UpdateRelease>> =
        buildList {
            m.latest?.let { add("latest" to it) }
            m.channels.forEach { (channel, release) -> add("channels.$channel" to release) }
        }

    @Test
    fun `the published file parses and announces the schema it actually is`() {
        val m = manifest()
        assertTrue("schema غير معروف: ${m.schema}", m.schema >= 1)
    }

    @Test
    fun `an announced release carries a real size and a real sha256`() {
        val m = manifest()
        assertTrue("لا إصدار معلن إطلاقًا", releases(m).isNotEmpty())
        releases(m).forEach { (where, release) ->
            if (!release.usable) return@forEach
            assertTrue(
                "$where قابل للتنزيل وبصمته ليست ٦٤ خانة سِت عشرية: «${release.sha256}»",
                release.sha256.matches(Regex("[0-9a-f]{64}")),
            )
            assertTrue("$where قابل للتنزيل وحجمه $release.size", release.size > 0L)
        }
    }

    @Test
    fun `the floor always has a way out`() {
        val m = manifest()
        val floor = m.minimum?.code ?: 0L
        if (floor <= 0L) return // ٠ أو الغياب = لا حجب، وهي الحالة الابتدائية المشروعة.
        val newest = releases(m).maxOfOrNull { it.second.code } ?: 0L
        assertTrue(
            "أرضيّة $floor تحجب نسخًا ولا إصدار يُخرجها منها (أحدث معلن $newest) — طريق مسدود",
            newest >= floor,
        )
    }

    @Test
    fun `maintenance cannot claim to block while it is inactive`() {
        val maintenance = manifest().maintenance
        if (maintenance == null || !maintenance.blocking) return
        assertTrue("blocking بلا active: حجبٌ لا يسقطه أحد", maintenance.active)
    }

    @Test
    fun `the text we publish is written by hand in both of our languages`() {
        val m = manifest()
        // ADR-14/§0.2: النصّان `en` و`ar` هما ما يُكتب بيد، وبقية اللغات تسقط إليهما.
        // والنصّ الفارغ مقصود في الملفّ الابتدائي (لا رسالة بعد) فلا يُطالَب بمحتوى.
        releases(m).forEach { (where, release) ->
            val notes = release.notes
            if (notes.isEmpty()) return@forEach
            listOf("en", "ar").forEach { key ->
                assertTrue("$where: مفتاح $key غائب من النصّ المنشور", notes.containsKey(key))
            }
        }
        m.minimum?.notes?.takeIf { it.values.any(String::isNotBlank) }?.let { notes ->
            listOf("en", "ar").forEach { key ->
                assertTrue("نصّ الحجب: مفتاح $key غائب", notes.containsKey(key))
            }
        }
    }

    @Test
    fun `every announced url is https and the other one is not used as a download`() {
        val m = manifest()
        releases(m).forEach { (where, release) ->
            if (release.url.isEmpty()) return@forEach
            assertTrue("$where: رابط تنزيل بلا https — «${release.url}»", release.url.startsWith("https://"))
            assertFalse("$where: رابط تنزيل يبدو صفحةً لا ملفًّا", release.url.endsWith("/"))
        }
    }
}

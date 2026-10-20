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

import nd.max.core.remote.RemoteLatest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حكم قناة اللوحة — مقيسًا على الدالّة الخالصة، بلا جهاز وبلا شاشة.
 *
 * والعطب الذي تمنعه هذه الأصناف قِيس فعلًا في الجولة السابقة: `latest.version` يُنشر من اللوحة
 * ولا يقرؤه أحد، و`download_page` كان يُهمَل لاختلاف الاسم — فبقيت «نافذة التحديث» غير موجودة
 * أصلًا. فالمقارنة والتصنيف هنا هما ما يجعل النشر من اللوحة يعني شيئًا في الجهاز.
 */
class ConfigUpdatesTest {

    @Test
    fun `tolerant version normalisation`() {
        assertEquals(listOf(2, 1), ConfigUpdates.normalize("V2.1"))
        assertEquals(listOf(2, 1), ConfigUpdates.normalize("v2.1"))
        assertEquals(listOf(2, 10, 0), ConfigUpdates.normalize("2.10.0"))
        // لاحقة وسم ليست رقمًا يُقارن، والرقم قبلها هو الحكم.
        assertEquals(listOf(2, 6), ConfigUpdates.normalize("2.6-beta"))
        assertNull(ConfigUpdates.normalize(""))
        assertNull(ConfigUpdates.normalize("UNKNOWN"))
        assertNull(ConfigUpdates.normalize("2..1"))
        assertNull(ConfigUpdates.normalize("2.1.0.4.9"))
    }

    @Test
    fun `numeric comparison, not lexicographic`() {
        assertTrue(ConfigUpdates.compare("2.10", "2.9")!! > 0)
        assertTrue(ConfigUpdates.compare("2.2", "V2.1")!! > 0)
        assertEquals(0, ConfigUpdates.compare("2.1.0", "2.1"))
        assertEquals(0, ConfigUpdates.compare("V2.1", "v2.1"))
        assertNull(ConfigUpdates.compare("2.2", "UNKNOWN"))
    }

    @Test
    fun `the four verdicts of the panel channel`() {
        val published = RemoteLatest(version = "2.2", released = "2026-10-15", page = "https://example.com/dl")

        // منشور أحدث من المثبَّت ⇒ نافذة.
        assertEquals(ConfigUpdates.Verdict.AVAILABLE, ConfigUpdates.decide(published, "V2.1"))
        // المنشور هو المثبَّت ⇒ لا نافذة، ولا «تحديث متاح» كاذب.
        assertEquals(ConfigUpdates.Verdict.UP_TO_DATE, ConfigUpdates.decide(published, "V2.2"))
        // لا إصدار منشور بعد.
        assertEquals(
            ConfigUpdates.Verdict.NOT_PUBLISHED,
            ConfigUpdates.decide(RemoteLatest(), "V2.1"),
        )
        assertEquals(
            ConfigUpdates.Verdict.NOT_PUBLISHED,
            ConfigUpdates.decide(null, "V2.1"),
        )
        // إصدار بلا رابط: عطب نشرٍ يُسمّى باسمه ولا يُخترع له رابط.
        assertEquals(
            ConfigUpdates.Verdict.LINK_MISSING,
            ConfigUpdates.decide(RemoteLatest(version = "2.3", page = ""), "V2.1"),
        )
        // نسخة التطبيق غير مقروءة ⇒ لا نُخفي تحديثًا منشورًا (الواجهة تقول «غير معروفة»).
        assertEquals(ConfigUpdates.Verdict.AVAILABLE, ConfigUpdates.decide(published, "UNKNOWN"))
    }
}

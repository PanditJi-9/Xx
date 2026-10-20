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

package nd.max.core.maxai

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * مصنّف «مطلوب/حيّ» لمقابض المدى — يفرّق قمع المنصّة عن الانحدار الحقيقي بلا جهاز.
 *
 * والقاعدة المُثبَّتة هنا هي النمط المقيس في حزم المالك: سقفٌ حيّ أدنى والأرضية نفسها
 * ⇒ قمع محتمل؛ وكل ما عداه (بما فيه الغامض) ⇒ انحدار يُعامَل كالسابق تمامًا.
 */
class ClampVerdictTest {

    private fun classify(wanted: String?, live: String?) =
        ClampVerdict.classify(wanted, live)

    @Test
    fun `an identical range matches`() {
        assertEquals(
            ClampVerdict.Verdict.MATCHED,
            classify("300000:1100000", "300000:1100000"),
        )
    }

    @Test
    fun `a live ceiling one step below with the same floor is a platform clamp`() {
        // النمط المقيس ٦/٧ في حزمة ٢٠٢٦-١٠-١٥: المقروء خطوة أدنى على السلّم المعلن.
        assertEquals(
            ClampVerdict.Verdict.CLAMPED_BELOW,
            classify("300000:1100000", "300000:1000000"),
        )
        assertEquals(
            ClampVerdict.Verdict.CLAMPED_BELOW,
            classify("400000:2900000", "400000:2800000"),
        )
        assertEquals(
            ClampVerdict.Verdict.CLAMPED_BELOW,
            classify("1000000:1300000", "1000000:1200000"),
        )
    }

    @Test
    fun `a moved floor is a real divergence not a clamp`() {
        assertEquals(
            ClampVerdict.Verdict.DIVERGED,
            classify("300000:1100000", "400000:1000000"),
        )
    }

    @Test
    fun `a live ceiling above wanted is a divergence not a clamp`() {
        assertEquals(
            ClampVerdict.Verdict.DIVERGED,
            classify("300000:1100000", "300000:1200000"),
        )
    }

    @Test
    fun `non range knobs never classify as clamped`() {
        // حاكم أو نصّ متطابق ⇒ مطابقة؛ ومختلف ⇒ انحدار كما كان — لا تليين لغير المدى.
        assertEquals(
            ClampVerdict.Verdict.MATCHED,
            classify("schedutil", "schedutil"),
        )
        assertEquals(
            ClampVerdict.Verdict.DIVERGED,
            classify("schedutil", "interactive"),
        )
        assertEquals(
            ClampVerdict.Verdict.DIVERGED,
            classify("300000:1100000", "interactive"),
        )
    }

    @Test
    fun `missing or malformed sides diverge instead of crashing`() {
        assertEquals(ClampVerdict.Verdict.DIVERGED, classify(null, "300000:1000000"))
        assertEquals(ClampVerdict.Verdict.DIVERGED, classify("300000:1100000", null))
        assertEquals(ClampVerdict.Verdict.DIVERGED, classify(null, null))
        assertEquals(ClampVerdict.Verdict.DIVERGED, classify("300000", "300000:1000000"))
        assertEquals(ClampVerdict.Verdict.DIVERGED, classify("300000:1100000", ""))
        assertEquals(ClampVerdict.Verdict.DIVERGED, classify("a:b", "a:c"))
    }
}

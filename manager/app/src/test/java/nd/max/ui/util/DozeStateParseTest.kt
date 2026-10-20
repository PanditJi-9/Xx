/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * قارئ حالة الـDoze يُقاس على النصوص التي تصل فعلًا من `dumpsys deviceidle` — لا على نصّ
 * واحد مفترض.
 *
 * والعطب المُبلَّغ عنه («المصدر deviceidle لا يقرأ») كان من مساواة حرفية على السطر كاملًا:
 * `State: IDLE` أو `mState=IDLE` تُعلَن `UNKNOWN` وهي قراءة ناجحة. وهذه العقود تقفل ذلك.
 */
class DozeStateParseTest {

    @Test
    fun `bare state names map`() {
        assertEquals(DozeState.ACTIVE, DozeModeUtil.mapState("ACTIVE"))
        assertEquals(DozeState.IDLE, DozeModeUtil.mapState("IDLE"))
        assertEquals(DozeState.IDLE_PENDING, DozeModeUtil.mapState("IDLE_PENDING"))
        assertEquals(DozeState.MAINTENANCE, DozeModeUtil.mapState("IDLE_MAINTENANCE"))
    }

    @Test
    fun `field prefixes from vendor dumps map`() {
        assertEquals(DozeState.IDLE, DozeModeUtil.mapState("mState=IDLE"))
        assertEquals(DozeState.SENSING, DozeModeUtil.mapState("mLightState=SENSING"))
        assertEquals(DozeState.LOCATING, DozeModeUtil.mapState("State: LOCATING"))
        assertEquals(DozeState.IDLE, DozeModeUtil.mapState("IDLE (deep)"))
    }

    @Test
    fun `response from a device without the controller is not a state`() {
        assertEquals(DozeState.UNKNOWN, DozeModeUtil.mapState("Can't find service: deviceidle"))
        assertEquals(DozeState.UNKNOWN, DozeModeUtil.mapState(""))
        assertEquals(DozeState.UNKNOWN, DozeModeUtil.mapState("unknown command: get"))
    }
}

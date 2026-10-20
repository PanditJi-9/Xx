package com.maxmanager.control

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {
    @Test fun versionShape() {
        assertTrue(Validators.isVersion("2.1"))
        assertTrue(Validators.isVersion("10"))
        assertTrue(Validators.isVersion("2.1.0.4"))
        assertFalse(Validators.isVersion(""))
        assertFalse(Validators.isVersion("v2.1"))
        assertFalse(Validators.isVersion("2..1"))
        assertFalse(Validators.isVersion("2.1.0.0.1"))
    }

    @Test fun versionOrderIsNumeric() {
        assertTrue(Validators.compareVersions("2.10", "2.9") > 0)
        assertTrue(Validators.compareVersions("2.0", "2.1") < 0)
        assertEquals(0, Validators.compareVersions("2.1", "2.1.0"))
    }

    @Test fun dates() {
        assertTrue(Validators.isDate("2026-10-14"))
        assertFalse(Validators.isDate("2026-02-30"))
        assertFalse(Validators.isDate("14-10-2026"))
        assertFalse(Validators.isDate(""))
    }

    @Test fun urls() {
        assertTrue(Validators.isHttpsUrl("https://example.com/subscribe"))
        assertFalse(Validators.isHttpsUrl("http://example.com"))
        assertFalse(Validators.isHttpsUrl("https://"))
        assertFalse(Validators.isHttpsUrl("https://a b.com"))
    }

    @Test fun noteLinesDropBlanks() {
        assertEquals(listOf("a", "b"), Validators.noteLines(" a \n\n  b\n"))
    }

    @Test fun keyDraftBoundsMatchTheServer() {
        // داخل الحدود ⇒ لا مشكلة (والطرفان مقبولان كما يقبلهما `intIn`).
        assertEquals(null, Validators.keyDraftProblem(days = 30, deviceLimit = 1, count = 1))
        assertEquals(null, Validators.keyDraftProblem(days = 3650, deviceLimit = 50, count = 200))
        // الخروج يُسمّى باسمه لا يُرسل ليُصحَّح صامتًا في الخادم.
        assertTrue(Validators.keyDraftProblem(days = 0, deviceLimit = 1, count = 1)!!.contains("1 و3650"))
        assertTrue(Validators.keyDraftProblem(days = 3651, deviceLimit = 1, count = 1) != null)
        assertTrue(Validators.keyDraftProblem(days = 30, deviceLimit = 51, count = 1)!!.contains("1 و50"))
        assertTrue(Validators.keyDraftProblem(days = 30, deviceLimit = 1, count = 0)!!.contains("1 و200"))
        assertTrue(Validators.keyDraftProblem(days = 30, deviceLimit = 1, count = 201) != null)
    }

    @Test fun momentsAcceptDateOrInstant() {
        assertTrue(Validators.isMoment("2026-10-14"))
        assertTrue(Validators.isMoment("2026-10-14T15:30:00Z"))
        assertTrue(Validators.isMoment("2026-10-14T15:30:00+02:00"))
        assertFalse(Validators.isMoment("14/10/2026"))
        assertFalse(Validators.isMoment("2026-10-14 15:30"))
        assertFalse(Validators.isMoment(""))
    }

    @Test fun windowIsHalfOpenAndLastDayIncluded() {
        // التاريخ ⇒ بداية اليوم، والنهاية ⇒ بداية اليوم التالي: اليوم الأخير **داخل** النافذة.
        assertEquals("2026-10-14T00:00:00Z", Validators.momentStart("2026-10-14"))
        assertEquals("2026-10-21T00:00:00Z", Validators.momentEndExclusive("2026-10-20"))
        // ولحظة كاملة تبقى كما كتبها المالك: لا إزاحة على ما كُتب صريحًا.
        assertEquals("2026-10-14T15:00:00Z", Validators.momentStart("2026-10-14T15:00:00Z"))
        assertEquals("2026-10-14T15:00:00Z", Validators.momentEndExclusive("2026-10-14T15:00:00Z"))
    }

    @Test fun windowDisplayRoundTrips() {
        // ما يكتبه `build` يقرؤه `load` بنفس القيم — وإلّا ظهرت اللوحة «معدَّلة» بعد التحميل
        // مباشرةً، فينشر المالك تغييرًا لم يكتبه.
        listOf("2026-10-14", "2026-01-01", "2026-12-31", "2028-02-29").forEach { day ->
            assertEquals(day, Validators.momentDisplay(Validators.momentStart(day)))
            assertEquals(day, Validators.momentEndDisplay(Validators.momentEndExclusive(day)))
        }
        // ولحظة ليست منتصف ليل تُعرض كما هي (لا تُكلَّس إلى يوم).
        assertEquals("2026-10-14T15:00:00Z", Validators.momentDisplay("2026-10-14T15:00:00Z"))
        assertEquals("2026-10-14T15:00:00Z", Validators.momentEndDisplay("2026-10-14T15:00:00Z"))
    }

    @Test fun windowOrderComparesInstantsNotText() {
        assertTrue(Validators.isWindowOrdered("2026-10-14", "2026-10-20"))
        assertTrue(Validators.isWindowOrdered("2026-10-14", "2026-10-14"))
        assertFalse(Validators.isWindowOrdered("2026-10-20", "2026-10-14"))
        // والمقارنة النصّية تكذب في الاتجاهين — وهذان شاهدان عليها:
        // ① بلحظاته: 10:00Z < 11:00Z ⇒ مرتَّبة، والنصّ يقول العكس.
        assertTrue(Validators.isWindowOrdered("2026-10-14T12:00:00+02:00", "2026-10-14T11:00:00Z"))
        // ② بلحظاته: 08:00Z > 07:00Z ⇒ غير مرتَّبة، والنصّ يقول العكس.
        assertFalse(Validators.isWindowOrdered("2026-10-14T08:00:00Z", "2026-10-14T09:00:00+02:00"))
    }
}

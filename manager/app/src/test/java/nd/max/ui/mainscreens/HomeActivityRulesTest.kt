/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import nd.max.core.privilege.PrivilegeLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * عقد «مفعّل / غير مفعّل» — أمر المالك ٢٠٢٦-١١-١٦:
 * «اعرض active إذا اختار الشخص بدون روت وShizuku، وليس active إذا اختار روت ولا يوجد،
 * أو module — يجب الشرطين لظهور active».
 *
 * والاختبار يحرس **السياسة** لا الشكل: مسار واحد عامل يكفي، والجذر وحده لا يكفي بلا وحدة.
 * ولو غُيّرت القاعدة غدًا سقط هذا الملفّ بالاسم، فلا تمرّ السياسة بتعديل صامت.
 */
class HomeActivityRulesTest {

    @Test
    fun `root without the module is not active`() {
        assertFalse(
            "الجذر وحده بلا وحدة ليس تفعيلًا — الشرطان معًا (أمر المالك)",
            HomeActivityRules.active(PrivilegeLevel.ROOT, moduleInstalled = false),
        )
    }

    @Test
    fun `root with the module is active`() {
        assertTrue(HomeActivityRules.active(PrivilegeLevel.ROOT, moduleInstalled = true))
    }

    @Test
    fun `shizuku ready is active with or without the module`() {
        assertTrue(HomeActivityRules.active(PrivilegeLevel.SHIZUKU, moduleInstalled = false))
        assertTrue(HomeActivityRules.active(PrivilegeLevel.SHIZUKU, moduleInstalled = true))
    }

    @Test
    fun `no working path is never active`() {
        assertFalse(HomeActivityRules.active(PrivilegeLevel.NONE, moduleInstalled = false))
        assertFalse(
            "وحدة مثبّتة بلا مسار صلاحية لا تُفعّل شيئًا",
            HomeActivityRules.active(PrivilegeLevel.NONE, moduleInstalled = true),
        )
    }
}

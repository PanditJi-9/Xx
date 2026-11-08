/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.navigation

import nd.max.core.privilege.AccessFilter
import nd.max.core.privilege.PrivilegeLevel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * سياسة «اعرض ما يدعمه الوضع» — الخريطة التي تحكم ما يراه المستخدم في وضع Shizuku.
 *
 * والخطأ الذي تمنعه ليس تجميليًّا: لو بقيت شاشة `wm`/`pm` مصنّفة ROOT لاختفت في وضع
 * Shizuku وهي تعمل فيه فعلًا، ولو بقي مسار sysfs مصنّفًا SHIZUKU لظهر ثم فشل بلا تفسير.
 */
class DestinationAccessTest {

    private fun shizuku() = DestinationAccess(PrivilegeLevel.SHIZUKU, AccessFilter.SUPPORTED_ONLY)
    private fun root() = DestinationAccess(PrivilegeLevel.ROOT, AccessFilter.SUPPORTED_ONLY)

    @Test
    fun `platform command screens are supported at shizuku level`() {
        val access = shizuku()
        assertTrue(access.supported(MaxDestination.Resolution))
        assertTrue(access.supported(MaxDestination.DebloatFreeze))
        assertTrue(access.supported(MaxDestination.DozeMode))
    }

    @Test
    fun `sysfs screens stay root only`() {
        val access = shizuku()
        assertFalse(access.supported(MaxDestination.CpuCoreControl))
        assertFalse(access.supported(MaxDestination.GovernorSettings))
        assertFalse(access.supported(MaxDestination.NetworkScheduler))
        assertFalse(access.supported(MaxDestination.FileManager))
        assertFalse(access.supported(MaxDestination.Dex2oat))
        assertFalse(access.supported(MaxDestination.BypassCharging))
    }

    @Test
    fun `root level supports both platform and sysfs screens`() {
        val access = root()
        assertTrue(access.supported(MaxDestination.Resolution))
        assertTrue(access.supported(MaxDestination.CpuCoreControl))
        assertTrue(access.supported(MaxDestination.NetworkScheduler))
        assertTrue(access.supported(MaxDestination.FileManager))
    }

    @Test
    fun `supported only hides what the level cannot run`() {
        val access = shizuku()
        assertTrue(access.visible(MaxDestination.Resolution))
        assertFalse(access.visible(MaxDestination.CpuCoreControl))
        assertFalse(access.visible(MaxDestination.NetworkScheduler))
    }

    @Test
    fun `everything keeps unsupported screens visible`() {
        val access = DestinationAccess(PrivilegeLevel.NONE, AccessFilter.EVERYTHING)
        assertTrue(access.visible(MaxDestination.Resolution))
        assertTrue(access.visible(MaxDestination.CpuCoreControl))
    }

    @Test
    fun `nothing privileged is supported without a transport`() {
        val access = DestinationAccess(PrivilegeLevel.NONE, AccessFilter.SUPPORTED_ONLY)
        assertFalse(access.supported(MaxDestination.Resolution))
        assertFalse(access.supported(MaxDestination.DozeMode))
        assertFalse(access.visible(MaxDestination.Resolution))
        // شاشة بلا صلاحية تبقى ظاهرة: لا تُحجب شاشة أساسية عن مستخدم بلا جذر.
        assertTrue(access.visible(MaxDestination.Now))
        assertTrue(access.visible(MaxDestination.Settings))
    }

    @Test
    fun `hidden destinations never return to a list`() {
        assertFalse(root().visible(MaxDestination.TouchBoost))
        val everything = DestinationAccess(PrivilegeLevel.ROOT, AccessFilter.EVERYTHING)
        assertFalse(everything.visible(MaxDestination.TouchBoost))
        assertFalse(everything.visible(MaxDestination.Plugins))
    }

    @Test
    fun `empty hubs are suppressed at shizuku level`() {
        val access = shizuku()
        // CPU: كل أبنائه كتابة sysfs → لا يبقى صفّ فارغ.
        assertFalse(access.visible(MaxDestination.CpuHub))
        // الذاكرة: zram وUFS وpreference كلها جذر.
        assertFalse(access.visible(MaxDestination.MemoryHub))
        // العرض: Resolution يعمل بـShizuku فيبقى الحوز ظاهرًا.
        assertTrue(access.visible(MaxDestination.DisplayHub))
    }

    @Test
    fun `root level keeps every populated hub`() {
        val access = root()
        assertTrue(access.visible(MaxDestination.CpuHub))
        assertTrue(access.visible(MaxDestination.MemoryHub))
        assertTrue(access.visible(MaxDestination.DisplayHub))
    }
}

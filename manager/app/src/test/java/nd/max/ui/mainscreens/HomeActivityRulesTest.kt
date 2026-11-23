/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.mainscreens

import nd.max.R
import nd.max.core.daemon.DaemonIpc
import nd.max.core.daemon.DaemonIpcProtocol
import nd.max.core.daemon.DaemonLiveness
import nd.max.core.privilege.PrivilegeLevel
import nd.max.ui.viewmodel.DaemonHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * عقد «مفعّل / غير مفعّل» — أمر المالك ٢٠٢٦-١١-١٦:
 * «اعرض active إذا اختار الشخص بدون روت وShizuku، وليس active إذا اختار روت ولا يوجد،
 * أو module — يجب الشرطين لظهور active».
 *
 * والاختبار يحرس **السياسة** لا الشكل: مسار واحد عامل يكفي، والجذر وحده لا يكفي بلا وحدة.
 * ولو غُيّرت القاعدة غدًا سقط هذا الملفّ بالاسم، فلا تمرّ السياسة بتعديل صامت.
 *
 * **وأُضيف في ٢٠٢٦-١١-٢٠ (أمر المالك، تقرير مستخدمين):** الشارة المفعّلة كانت تفتح نفس نافذة
 * «لماذا غير مفعّل؟»، فقرأ المستخدمون تطبيقًا يعمل كأنه معطّل. فصار للعنوان والجسم **معرّفان
 * مختلفان** تُثبتهما هذه الدروس، وصار للخادم الخلفيّ ثلاث حالات تُقاس لا واحدة.
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
    fun `a module written but not rebooted is not active yet`() {
        assertFalse(
            "الوحدة المكتوبة من التطبيق لا تعمل قبل الإقلاع — ومدير الجذر يراها فورًا",
            HomeActivityRules.active(
                PrivilegeLevel.ROOT,
                moduleInstalled = true,
                rebootPending = true,
            ),
        )
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

    /* ─── نافذة الحالة: «مفعّل» لا تُقرأ «لماذا غير مفعّل؟» (أمر المالك ٢٠٢٦-١١-٢٠) ─── */

    @Test
    fun `an active app is never told why it is not active`() {
        assertNotEquals(
            "العنوانان لا يتّحدان: الشارة المفعّلة لا تفتح «لماذا غير مفعّل؟»",
            HomeStatusCopy.titleRes(active = true),
            HomeStatusCopy.titleRes(active = false),
        )
        assertNotEquals(
            "والجسمان لا يتّحدان كذلك — النصّ نفسه كان نصف العطب",
            HomeStatusCopy.bodyRes(active = true),
            HomeStatusCopy.bodyRes(active = false),
        )
    }

    @Test
    fun `the inactive dialog keeps the exact strings the owner asked for`() {
        assertEquals(R.string.home_status_reason_title, HomeStatusCopy.titleRes(active = false))
        assertEquals(R.string.home_status_reason_body, HomeStatusCopy.bodyRes(active = false))
    }

    @Test
    fun `the active dialog does not reuse the inactive strings`() {
        assertNotEquals(R.string.home_status_reason_title, HomeStatusCopy.titleRes(active = true))
        assertNotEquals(R.string.home_status_reason_body, HomeStatusCopy.bodyRes(active = true))
    }

    /* ─── الخادم الخلفيّ: «حيّ يرفض الهويّة» ليس «فشل» ─── */

    @Test
    fun `a running daemon is running`() {
        assertEquals(
            DaemonHealth.RUNNING,
            DaemonHealth.of(DaemonIpcProtocol.parseReply("OK state=running ai=1\n")),
        )
    }

    @Test
    fun `a daemon that refuses this app's identity is denied not failed`() {
        assertEquals(
            "`ERR denied` يعني حيًّا يرفض الهويّة — لا خادمًا فشل",
            DaemonHealth.DENIED,
            DaemonHealth.of(DaemonIpcProtocol.parseReply("ERR denied\n")),
        )
        assertNotEquals(DaemonHealth.UNREACHABLE, DaemonHealth.of(DaemonIpc.Result.Refused("denied")))
    }

    @Test
    fun `no channel is unreachable and never a false success`() {
        assertEquals(DaemonHealth.UNREACHABLE, DaemonHealth.of(DaemonIpc.Result.Unreachable))
    }

    /**
     * مقيس على جهاز المالك (`192.168.1.3:36303`، ٢٠٢٦-١١-٢٣): الخادم حيّ وسوكيته مستمع،
     * لكن التطبيق (`untrusted_app`) لا يصل إلى سوكيت نطاق `su` في تثبيت KernelSU. فحكمٌ
     * بـ«لا يجيب» عن خادم يعمل هو الرسالة الكاذبة نفسها التي جاء الأمر لإزالتها.
     */
    @Test
    fun `a daemon alive but unreachable from the app is not reported as silent`() {
        assertEquals(
            "الجذر أثبت الحياة والقناة لم ترد — سطر رابع لا «لا يجيب»",
            DaemonHealth.CHANNEL_BLOCKED,
            DaemonHealth.of(DaemonIpc.Result.Unreachable, DaemonLiveness.Alive),
        )
        assertEquals(
            "وحين ينفي الجذر الحياة يبقى الحكم «لا يجيب»",
            DaemonHealth.UNREACHABLE,
            DaemonHealth.of(DaemonIpc.Result.Unreachable, DaemonLiveness.Dead),
        )
        assertEquals(
            "وقياس جذر غير حاسم لا يغيّر الحكم",
            DaemonHealth.UNREACHABLE,
            DaemonHealth.of(DaemonIpc.Result.Unreachable, DaemonLiveness.Unknown),
        )
        assertEquals(
            "وردّ القناة هو الحكم دائمًا، فلا يُستبدل بتأكيد جذر",
            DaemonHealth.RUNNING,
            DaemonHealth.of(
                DaemonIpcProtocol.parseReply("OK state=running ai=1\n"),
                DaemonLiveness.Dead,
            ),
        )
    }

    @Test
    fun `an unmeasured daemon stays unknown instead of claiming failure`() {
        assertEquals(
            "قبل أوّل قياس لا تُدّعى حالة — لا «لا يجيب» ولا «فشل»",
            DaemonHealth.UNKNOWN,
            DaemonHealth.of(null),
        )
        assertNull(
            "ولا سطر للخادم قبل القياس",
            HomeStatusCopy.daemonRowRes(DaemonHealth.UNKNOWN),
        )
    }

    @Test
    fun `a closed channel is not shown as a daemon failure`() {
        assertNull(
            "قناة مغلقة بسياسة النظام لا تُعرض عطبًا — التطبيق يعمل بمساره المباشر (أمر المالك)",
            HomeStatusCopy.daemonRowRes(DaemonHealth.UNREACHABLE),
        )
        assertEquals(
            "والخادم الذي ردّ «متوقف» يبقى بسطره",
            R.string.home_status_row_daemon_stopped,
            HomeStatusCopy.daemonRowRes(DaemonHealth.STOPPED),
        )
    }

    @Test
    fun `only a daemon that answers and runs gets the check`() {
        assertTrue(HomeStatusCopy.daemonSatisfied(DaemonHealth.RUNNING))
        assertFalse(HomeStatusCopy.daemonSatisfied(DaemonHealth.DENIED))
        assertFalse(HomeStatusCopy.daemonSatisfied(DaemonHealth.UNREACHABLE))
        assertFalse(HomeStatusCopy.daemonSatisfied(DaemonHealth.STOPPED))
        assertFalse(HomeStatusCopy.daemonSatisfied(DaemonHealth.UNKNOWN))
        assertFalse(
            "حيّ لكن التطبيق لا يصل إليه = مشكلة قناة، ليست «دون مشاكل»",
            HomeStatusCopy.daemonSatisfied(DaemonHealth.CHANNEL_BLOCKED),
        )
        assertNull(
            "UNKNOWN ليست نجاحًا ولا فشلًا — سطر محايد",
            HomeStatusCopy.daemonVerdict(DaemonHealth.UNKNOWN),
        )
    }

    @Test
    fun `the daemon row is asked only in root mode`() {
        assertNull(
            "Shizuku يعمل بمساره المباشر — لا سؤال خادم",
            HomeStatusCopy.daemonRowFor(PrivilegeLevel.SHIZUKU, DaemonHealth.RUNNING),
        )
        assertNull(
            "وبلا مسار لا خادم يُسأل",
            HomeStatusCopy.daemonRowFor(PrivilegeLevel.NONE, DaemonHealth.RUNNING),
        )
        assertEquals(
            "وفي وضع الروت يُعرض الخادم الذي لا يجيب",
            R.string.home_status_row_daemon_unreachable,
            HomeStatusCopy.daemonRowFor(PrivilegeLevel.ROOT, DaemonHealth.UNREACHABLE),
        )
        assertEquals(
            "وقبل القياس يُقال «لم تُقس» لا «فشل»",
            R.string.home_status_row_daemon_unknown,
            HomeStatusCopy.daemonRowFor(PrivilegeLevel.ROOT, DaemonHealth.UNKNOWN),
        )
        assertNull(
            "سطر «يعمل ولا يصل إليه التطبيق» لا يُعرض خارج وضع الروت",
            HomeStatusCopy.daemonRowFor(PrivilegeLevel.SHIZUKU, DaemonHealth.CHANNEL_BLOCKED),
        )
        assertEquals(
            "وفي وضع الروت يُعرض بسطر مستقلّ عن «لا يجيب»",
            R.string.home_status_row_daemon_channel_blocked,
            HomeStatusCopy.daemonRowFor(PrivilegeLevel.ROOT, DaemonHealth.CHANNEL_BLOCKED),
        )
    }

    @Test
    fun `each shown daemon state has its own line`() {
        val rows = listOf(
            DaemonHealth.RUNNING,
            DaemonHealth.DENIED,
            DaemonHealth.STOPPED,
            DaemonHealth.CHANNEL_BLOCKED,
            DaemonHealth.UNKNOWN,
        ).map { HomeStatusCopy.daemonRowRes(it, rootMode = true) }
        assertEquals("لكلّ حالة معروضة سطرها لا سطرٌ مشترك", rows.size, rows.toSet().size)
        assertTrue("الأسطر كلها موجودة", rows.all { it != null })
    }

    /* ─── الوصل: النافذة تقرأ القاعدة، فلا يعود النصّ يُكتب داخلها ─── */

    @Test
    fun `the status dialog reads the rule instead of hardcoded copy`() {
        val screen = locate("src/main/java/nd/max/ui/mainscreens/LegendaryHomeDashboard.kt")
        assumeTrue("مصدر الشاشة غير موجود في مسار العمل — تتولّاه CI", screen != null)
        val source = screen!!.readText()
        assertTrue(
            "العنوان من القاعدة",
            source.contains("stringResource(HomeStatusCopy.titleRes(active))"),
        )
        assertTrue(
            "والجسم من القاعدة",
            source.contains("stringResource(HomeStatusCopy.bodyRes(active))"),
        )
        assertTrue(
            "وسطر الخادم من الحالة المقيسة وفي وضع الروت وحده",
            source.contains("HomeStatusCopy.daemonRowFor(accessLevel, daemonHealth)"),
        )
        assertFalse(
            "ولا يبقى العنوان الثابت في النافذة",
            source.contains("stringResource(R.string.home_status_reason_title)"),
        )
    }

    private fun locate(relative: String): File? = listOf(
        File(relative),
        File("app/$relative"),
        File("../app/$relative"),
        File("manager/app/$relative"),
    ).firstOrNull { it.isFile }
}

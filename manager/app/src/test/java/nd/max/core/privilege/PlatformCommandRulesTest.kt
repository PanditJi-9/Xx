/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.privilege

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حدّ أوامر المنصّة: ما يُسمح به في طبقة ADB (Shizuku) وما يُرفض قبل التنفيذ.
 *
 * والخطر الذي تقيسه هذه القائمة ليس نظريًّا: `wm`/`pm`/`dumpsys` تعمل بمستوى ADB، أمّا
 * `echo 1 > /sys/...` فلا — ولو مرّت لأوهمت نجاحًا في وضع Shizuku وهي لم تكتب شيئًا، أو
 * نفّذت نصف الأمر ثم عاد الخطأ غامضًا.
 */
class PlatformCommandRulesTest {

    @Test
    fun `platform verbs used by real screens are accepted`() {
        assertTrue(PlatformCommandRules.isPlatformCommand("wm size"))
        assertTrue(PlatformCommandRules.isPlatformCommand("wm density 420"))
        assertTrue(PlatformCommandRules.isPlatformCommand("pm list packages -3"))
        assertTrue(PlatformCommandRules.isPlatformCommand("pm disable-user --user 0 com.example"))
        assertTrue(PlatformCommandRules.isPlatformCommand("cmd appops get com.example RUN_IN_BACKGROUND"))
        assertTrue(PlatformCommandRules.isPlatformCommand("cmd deviceidle whitelist +com.example"))
        assertTrue(PlatformCommandRules.isPlatformCommand("dumpsys deviceidle"))
        assertTrue(PlatformCommandRules.isPlatformCommand("dumpsys media.audio_flinger"))
        assertTrue(PlatformCommandRules.isPlatformCommand("settings get global low_power"))
        assertTrue(PlatformCommandRules.isPlatformCommand("service call SurfaceFlinger 1013"))
        assertTrue(PlatformCommandRules.isPlatformCommand("pidof com.example"))
        assertTrue(PlatformCommandRules.isPlatformCommand("getprop ro.build.version.sdk"))
        assertTrue(PlatformCommandRules.isPlatformCommand("am get-standby-bucket com.example"))
        assertTrue(PlatformCommandRules.isPlatformCommand("date +%s"))
    }

    @Test
    fun `kernel and privileged paths are rejected`() {
        assertFalse(PlatformCommandRules.isPlatformCommand("echo 1 > /sys/devices/system/cpu/cpu0/online"))
        assertFalse(PlatformCommandRules.isPlatformCommand("ls /proc/sys/vm"))
        assertFalse(PlatformCommandRules.isPlatformCommand("cat /data/adb/modules/x/module.prop"))
        assertFalse(PlatformCommandRules.isPlatformCommand("dd if=/dev/block/by-name/boot of=/sdcard/boot.img"))
    }

    @Test
    fun `privilege escalation verbs are rejected`() {
        assertFalse(PlatformCommandRules.isPlatformCommand("su -c id"))
        assertFalse(PlatformCommandRules.isPlatformCommand("sudo id"))
        assertFalse(PlatformCommandRules.isPlatformCommand("sh -c su"))
        assertFalse(PlatformCommandRules.isPlatformCommand("mount -o remount,rw /system"))
        assertFalse(PlatformCommandRules.isPlatformCommand("insmod /sdcard/x.ko"))
        assertFalse(PlatformCommandRules.isPlatformCommand("rmmod x"))
        assertFalse(PlatformCommandRules.isPlatformCommand("modprobe x"))
        assertFalse(PlatformCommandRules.isPlatformCommand("chmod 777 /data/local/tmp/x"))
        assertFalse(PlatformCommandRules.isPlatformCommand("chown root:root /data/local/tmp/x"))
    }

    @Test
    fun `error silencing is allowed but arbitrary redirection is not`() {
        assertTrue(PlatformCommandRules.isPlatformCommand("dumpsys media.audio_flinger 2>/dev/null"))
        assertTrue(PlatformCommandRules.isPlatformCommand("wm size 2>&1"))
        assertTrue(PlatformCommandRules.isPlatformCommand("pm list packages >/dev/null"))
        assertFalse(PlatformCommandRules.isPlatformCommand("wm size > /sdcard/size.txt"))
        assertFalse(PlatformCommandRules.isPlatformCommand("dumpsys window >/data/local/tmp/w.txt"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages >> /sdcard/p.txt"))
    }

    @Test
    fun `non-platform and empty commands are rejected`() {
        assertFalse(PlatformCommandRules.isPlatformCommand(""))
        assertFalse(PlatformCommandRules.isPlatformCommand("   "))
        assertFalse(PlatformCommandRules.isPlatformCommand("rm -rf /sdcard/x"))
        assertFalse(PlatformCommandRules.isPlatformCommand("cp /sdcard/a /sdcard/b"))
        assertFalse(PlatformCommandRules.isPlatformCommand("while true; do :; done"))
    }

    @Test
    fun `one platform command may be filtered through grep only`() {
        assertTrue(PlatformCommandRules.isPlatformCommand("dumpsys display | grep -m1 'fps='"))
        assertTrue(PlatformCommandRules.isPlatformCommand("dumpsys window 2>/dev/null | grep -e mCurrentFocus -e mFocusedApp"))
        assertTrue(PlatformCommandRules.isPlatformCommand("dumpsys SurfaceFlinger --timestats -dump | grep averageFPS"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages | sh"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages | reboot"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages | grep x | grep y"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages | grep x; reboot"))
    }

    @Test
    fun `shell chaining is rejected even behind an allowed verb`() {
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages; reboot"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages && reboot"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages || reboot"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages | sh"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages & reboot"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages\nreboot"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages `id`"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages \$(id)"))
        assertFalse(PlatformCommandRules.isPlatformCommand("pm list packages < /sdcard/p.txt"))
    }

    @Test
    fun `prefix matching does not accept lookalikes`() {
        assertFalse(PlatformCommandRules.isPlatformCommand("pmsomething"))
        assertFalse(PlatformCommandRules.isPlatformCommand("wmx size"))
        assertFalse(PlatformCommandRules.isPlatformCommand("amazing thing"))
    }
}

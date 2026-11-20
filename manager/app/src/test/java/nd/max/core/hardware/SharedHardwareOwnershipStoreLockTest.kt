/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.hardware

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * قفل المستودع المشترك لا يُسقط التطبيق.
 *
 * **القياس الذي وُجد هذا الاختبار لأجله:** `dropbox` على جهاز المالك فيه ثمانية سقوطات `nd.max`
 * باستثناء واحد متكرّر — `FileNotFoundException: /data/user_de/0/nd.max/files/hardware-control-plane/
 * owners.lock: open failed: EACCES` — لأن الرفيق الجذريّ ينشئ ملفّ القفل بمالك/تصفيفٍ لا يفتحه التطبيق.
 * والسقوط يقع في `RandomAccessFile` داخل `withExclusive`، أي **أثناء عملية تحكّم عادية** لا في مسار
 * نادر. فالمطلوب سلوكٌ واحد: يُعاد إنشاء القفل ويُمضى، أو يُعلَن العطب باسمه — ولا `EACCES` عارٍ.
 */
class SharedHardwareOwnershipStoreLockTest {

    private val root: File = File.createTempFile("maxmanager-ownership-", "").let { probe ->
        probe.delete()
        File(probe.absolutePath + "-dir").apply { mkdirs() }
    }

    @After
    fun cleanup() {
        root.deleteRecursively()
    }

    @Test
    fun `an unopenable lock file is recreated instead of crashing the caller`() {
        // مجلد باسم owners.lock: أي فتحٍ للملفّ يفشل، وهو نفس شكل الفشل الذي رآه المالك (EACCES)،
        // لكنه قابل للتكرار في اختبار JVM بلا جذر.
        val lock = File(root, "hardware-control-plane").apply { mkdirs() }.let { dir ->
            File(dir, "owners.lock").apply { mkdir() }
        }
        assertTrue("المُهيّأ يجب أن يكون مجلدًا ليكسر الفتح", lock.isDirectory)

        SharedHardwareOwnershipStore.configure(root, 0, 1)
        val snapshot = SharedHardwareOwnershipStore.withExclusive { it.intents.size }

        assertEquals(0, snapshot)
        assertTrue("القفل يجب أن يُعاد ملفًّا", File(lock.parentFile, "owners.lock").isFile)
    }

    @Test
    fun `a healthy lock file is opened in place and keeps the journal`() {
        SharedHardwareOwnershipStore.configure(root, 0, 1)
        val intent = SharedHardwareOwnershipStore.newIntent(
            key = "cpu.max_freq",
            owner = ControlOwnership.Owner.MAX_AI,
            token = "t",
            desired = "1000",
            requestId = "r",
        )
        SharedHardwareOwnershipStore.withExclusive { it.replaceToken(intent) }
        val stored = SharedHardwareOwnershipStore.withExclusive { journal ->
            journal.intents.count { it.key == "cpu.max_freq" }
        }
        assertEquals(1, stored)
    }
}

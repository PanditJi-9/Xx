/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * العناية بالتخزين — **القرارات وحدها**، مقيسة على JVM بلا أندرويد وبلا ملفّ.
 *
 * وأربعة عقود من `MAX-MANAGER-LEVEL-UP.md` §6 §7 §10.3 محفورة هنا، فلا يحرّكها أحد بضغطة
 * عابرة: مطفأ افتراضيًّا (APK والمجلدات الفارغة) · المقفل يُعلن ما ينقصه ولا يختفي · لا صفر
 * مكان فشل القياس · المجموع يتبع التأشير لا الوجود.
 *
 * **والخامسة أُضيفت في `STORAGE-CARE-01`:** بقايا التطبيقات المُزالة (`CorpseData`) **مُشّرّة
 * افتراضيًّا** — هي ليست ملك أحد (التطبيق أُزيل فعلًا وبقيت بياناته)، فاستثناؤها كان يحمي ما
 * لا يحتاج حماية. وهذا هو الفرق الذي يشرحه العقد الأول في [StorageCareModel].
 */
class StorageCareModelTest {

    @Test
    fun `installers and empty folders are off by default, by the owner's contract`() {
        val safe = StorageCareModel.safeSelection()
        assertFalse("APK قد تكون ملك المستخدم", safe.contains(StorageCareCategory.InstallerFiles))
        assertFalse("مجلد فارغ قد يكون مجلد المستخدم", safe.contains(StorageCareCategory.EmptyFolders))
        assertEquals(
            "وما عدا الاثنين مُشّر: كاش، تخزين مشترك، بقايا مُزالة، صور مصغّرة، سجلات",
            setOf(
                StorageCareCategory.AppCaches,
                StorageCareCategory.SharedCaches,
                StorageCareCategory.CorpseData,
                StorageCareCategory.Thumbnails,
                StorageCareCategory.SystemLogs,
            ),
            safe,
        )
    }

    @Test
    fun `corpse data is ticked by default, because nothing living owns it`() {
        assertTrue(
            "بقايا تطبيق أُزيل ليست ملك أحد — فلا تُطفأ افتراضيًّا كـAPK والمجلدات الفارغة",
            StorageCareModel.safeSelection().contains(StorageCareCategory.CorpseData),
        )
        assertTrue("وتُقاس بالجذر وحده (سرد Android/data خارج قراءة التطبيق)", StorageCareCategory.CorpseData.needsRoot)
        assertFalse(
            "ولا تُعرض بعدد بل بحجم — العدد هنا خبر ثانٍ لا الخبر الوحيد",
            StorageCareModel.showsCount(StorageCareCategory.CorpseData),
        )
    }

    @Test
    fun `a root-only category is hidden without root, and the rest is shown`() {
        // والمخفيّة **سبع**: مسارها خارج ما يقرؤه uid التطبيق أصلًا (لا قياس ولا حذف)،
        // فعرضها كصفّ معطّل كان صفًّا لا يمتلئ أبدًا. والباقي يُعرض دائمًا.
        val rootOnly = StorageCareCategory.entries.filter { it.needsRoot }
        assertEquals(
            "فئات الجذر السبع مثبّتة بالأسماء: إضافة ثامنة تحتاج قرارًا لا تعديلًا عابرًا",
            setOf(
                StorageCareCategory.AppCaches,
                StorageCareCategory.SharedCaches,
                StorageCareCategory.CorpseData,
                StorageCareCategory.SystemLogs,
                StorageCareCategory.BrokenSymlinks,
                StorageCareCategory.Databases,
                StorageCareCategory.RecycleBin,
            ),
            rootOnly.toSet(),
        )
        for (category in rootOnly) {
            assertFalse(
                "لا تُعرض بلا جذر: ${category.key}",
                StorageCareModel.visibleAt(rootAvailable = false, category),
            )
            assertTrue(
                "وتُعرض بالجذر: ${category.key}",
                StorageCareModel.visibleAt(rootAvailable = true, category),
            )
        }
        for (category in StorageCareCategory.entries.filter { !it.needsRoot }) {
            assertTrue(
                "ما يُقاس بلا جذر يُعرض بلا جذر: ${category.key}",
                StorageCareModel.visibleAt(rootAvailable = false, category),
            )
        }
        assertEquals(
            "بلا جذر تُعرض أربع، وبالجذر إحدى عشرة — وهذا هو الفرق المقيس الذي يشرحه سطر النطاق",
            4,
            StorageCareCategory.entries.count { StorageCareModel.visibleAt(rootAvailable = false, it) },
        )
        assertEquals(11, StorageCareCategory.entries.size)
    }

    @Test
    fun `the total follows the ticks, and a failed measurement is not a zero`() {
        val measurements = CleanMeasurementSet(
            listOf(
                CleanMeasurement(StorageCareCategory.AppCaches, 1_000L, 3),
                CleanMeasurement(StorageCareCategory.Thumbnails, 500L, 2),
                // فشل قياس: ليس صفرًا، ولا يُحسب.
                CleanMeasurement(StorageCareCategory.SharedCaches, null, 0),
                CleanMeasurement(StorageCareCategory.SystemLogs, 7_000L, 9),
            )
        )

        val ticked = setOf(StorageCareCategory.AppCaches, StorageCareCategory.Thumbnails)
        val small = StorageCareModel.total(ticked, measurements)
        assertEquals(1_500L, small.bytes)
        assertFalse("كل ما أُشّر قيس، فالمجموع تامّ", small.hasUnmeasured)

        val withFailure = StorageCareModel.total(
            ticked + StorageCareCategory.SharedCaches,
            measurements,
        )
        assertEquals(
            "الفئة التي فشل قياسها لا تدخل الرقم، ولا تُقرأ صفرًا",
            1_500L,
            withFailure.bytes,
        )
        assertTrue("لكن المجموع يُعلن أنه ناقص", withFailure.hasUnmeasured)

        val all = StorageCareModel.total(
            setOf(
                StorageCareCategory.AppCaches,
                StorageCareCategory.Thumbnails,
                StorageCareCategory.SystemLogs,
            ),
            measurements,
        )
        assertEquals(8_500L, all.bytes)

        assertFalse(
            "وما لم يُشّر لا يدخل المجموع ولو قيس",
            StorageCareModel.total(setOf(StorageCareCategory.SystemLogs), measurements).bytes == 0L,
        )
    }

    @Test
    fun `an untouched category contributes nothing, and an empty measurement set is not a failure`() {
        val empty = StorageCareModel.total(StorageCareModel.safeSelection(), CleanMeasurementSet())
        assertEquals(0L, empty.bytes)
        // ولم تُقس فئة واحدة بعد ⇒ لا «ناقص» بل «لا شيء بعد»: الشاشة تفرّق بينهما بـ
        // `measuredOnce`، والنموذج لا يَدّعي نقصًا لم يقع.
        assertFalse(empty.hasUnmeasured)
    }

    @Test
    fun `empty folders are reported by count, because their size is zero by definition`() {
        assertTrue(StorageCareModel.showsCount(StorageCareCategory.EmptyFolders))
        assertFalse(StorageCareModel.showsCount(StorageCareCategory.Thumbnails))
        assertFalse(StorageCareModel.showsCount(StorageCareCategory.InstallerFiles))
    }

    @Test
    fun `every key is distinct, so a stored key can never mean two categories`() {
        val keys = StorageCareCategory.entries.map { it.key }
        assertEquals(keys.distinct(), keys)
        assertTrue("والمفاتيح لاتينية صغيرة بلا فراغ", keys.all { it.matches(Regex("[a-z_]+")) })
    }

    @Test
    fun `a du row is kilobytes, and a malformed line is a silence not a zero`() {
        // الوحدة كيلوبايت كما يعرّفها `du -k` — والتحويل هنا مرة واحدة، لا في طبقة التنفيذ.
        assertEquals("/data/x" to 1_024L, StorageCareModel.parseDuRow("1\t/data/x"))
        assertEquals("/data/x" to 1_024L, StorageCareModel.parseDuRow("1 /data/x"))
        assertEquals("/a b/c" to 2_048L, StorageCareModel.parseDuRow("2\t/a b/c"))

        // سطر مشوّه ⇒ `null` ولا قيمة مُخترعة: لا رقم، لا مسار، أو سالب.
        assertNull(StorageCareModel.parseDuRow(""))
        assertNull(StorageCareModel.parseDuRow("   "))
        assertNull(StorageCareModel.parseDuRow("/only/path"))
        assertNull(StorageCareModel.parseDuRow("12"))
        assertNull(StorageCareModel.parseDuRow("-3\t/data/x"))
        assertNull(StorageCareModel.parseDuRow("abc\t/data/x"))
    }

    @Test
    fun `a cache path names its package only when the shape proves it`() {
        assertEquals("com.foo", StorageCareModel.packageOfCachePath("/data/data/com.foo/cache"))
        assertEquals(
            "com.bar",
            StorageCareModel.packageOfCachePath("/storage/emulated/0/Android/data/com.bar/cache"),
        )
        assertEquals("com.baz", StorageCareModel.packageOfCachePath("/data/data/com.baz/cache/"))

        // والشكل هو الحكم: ما لا ينتهي بـ`cache` لا يُنسب، و`data`/`cache` ليستا حزمتين.
        assertNull(StorageCareModel.packageOfCachePath("/data/data/com.foo/files"))
        assertNull(StorageCareModel.packageOfCachePath("/data/data/cache/cache"))
        assertNull(StorageCareModel.packageOfCachePath("/data/data/data/cache"))
        assertNull(StorageCareModel.packageOfCachePath("/cache"))
        assertNull(StorageCareModel.packageOfCachePath(""))
    }

    @Test
    fun `folding a target list ranks by size, keeps the tail summed, and counts every row`() {
        val rows = listOf(
            CareTarget(label = "small", path = "/c", bytes = 50L),
            CareTarget(label = "big", path = "/a", bytes = 900L),
            CareTarget(label = "unknown", path = "/d", bytes = null),
            CareTarget(label = "tie", path = "/b", bytes = 50L),
        )
        val fold = StorageCareModel.foldTargets(rows, limit = 2)

        // المجهول آخرًا (بلا حجم لا يُنافس)، والتعادل بالاسم — فالعرض ثابت لا يتبدّل برحلة.
        assertEquals(listOf("big", "small"), fold.rows.map { it.label })
        assertEquals(2, fold.restCount)
        assertEquals(50L, fold.restBytes)
        // والمقيس مجموع **الكل** لا المعروض: ما طُوي يبقى في المجموع، والطيّ لا يُخفي رقمًا.
        assertEquals(1_000L, fold.measuredBytes)
        assertTrue("في القائمة قياس مجهول ⇒ المجموع حدّ أدنى", fold.anyUnmeasured)
        assertFalse(fold.failed)
        assertEquals(4, fold.totalItems)
    }

    @Test
    fun `a failed list is not an empty list, and an empty list is not a failure`() {
        val failed = StorageCareModel.failedTargets()
        assertTrue(failed.failed)
        assertFalse("«تعذّرت القراءة» ليست «لا شيء هنا»", failed.isEmpty)

        val empty = StorageCareModel.foldTargets(emptyList())
        assertTrue(empty.isEmpty)
        assertFalse(empty.failed)
        assertEquals(0, empty.totalItems)
    }

    @Test
    fun `a category that deletes or edits user files is never ticked by default`() {
        // حرسٌ ضد ارتداد عطب مُقيس (تكملة هذه الجولة): أُضيفت بعد الجولة الأصلية فئات
        // (مكرّرات · قواعد بيانات · روابط مكسورة · سلة محذوفات) بمفتاح «مُشّر افتراضيًّا»،
        // و`cleanDuplicates` يمرّر `FileSystemEngine.delete` على ملفّات المستخدم نفسها. أي أن
        // نقرة «نظّف» واحدة كانت كافية لمحو ما لا يعرف المستخدم أنه أُشّر. القاعدة هنا ثابتة:
        // ما يحذف أو يعدّل ما قد يكون ملك المستخدم لا يُشّر إلّا بقرار صريح.
        val safe = StorageCareModel.safeSelection()
        setOf(
            StorageCareCategory.InstallerFiles,
            StorageCareCategory.EmptyFolders,
            StorageCareCategory.Duplicates,
            StorageCareCategory.Databases,
            StorageCareCategory.BrokenSymlinks,
            StorageCareCategory.RecycleBin,
        ).forEach { category ->
            assertFalse("لا يُشّر افتراضيًّا: ${category.key}", safe.contains(category))
        }
    }

    @Test
    fun `the sqlite vacuum path reads the command verdict, not the fact that something printed`() {
        // «نجاح كاذب» بعينه: كان التحقق السابق `result.any { it.isNotBlank() || true }` — وهو
        // صحيح لأي عنصر، أي أن مجرّد طباعة سطر تُقرأ نجاحًا. والقياس هنا نصّي على المصدر لأنه
        // ما يمكن قياسه في هذه البيئة؛ أمّا تنفيذ `sqlite3` نفسه فيبقى يحتاج جهازًا.
        val engine = sourceFile("ui/util/StorageCareEngine.kt").readText()
        assertFalse(
            "لا يعود نمط «طُبِع شيء ⇒ نجح»",
            engine.contains("isNotBlank() || true"),
        )
        assertTrue(
            "الحكم من رمز خروج الأمر",
            engine.contains("runDetailed(\"sqlite3 "),
        )
    }

    /** مسار ملفّ مصدر من جذر يعمل في كل المواضع: CI من `manager/`، ومحلّيًّا من الجذر. */
    private fun sourceFile(relative: String): java.io.File {
        val candidates = listOf(
            java.io.File("src/main/java/nd/max/$relative"),
            java.io.File("app/src/main/java/nd/max/$relative"),
            java.io.File("../app/src/main/java/nd/max/$relative"),
            java.io.File("manager/app/src/main/java/nd/max/$relative"),
        )
        return candidates.firstOrNull { it.isFile } ?: error("لم يُوجد المصدر: $relative")
    }

}

package nd.max.core.security

import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * مصيدة البيانات في `devices.db` — الصفّ الوهميّ الذي يُثبت **النقل** لا الفكرة.
 *
 * **لماذا اختبار وليس تعليقًا:** المصيدة بلا حارسٍ تُحذف في أول «تنظيف بيانات» فيصمت الدليل،
 * ولا يظهر ذلك في أي شاشة. فهذا الملفّ يمنع الحذف الصامت نفسه.
 *
 * **وحدّه المُعلن:** البحث في بايتات الأصل لا في محرّك SQLite — فاختبار JVM لا يملك
 * `android.database.sqlite`. والمقيس أن الثلاثية موجودة نصًّا في الملفّ المشحون، وهو ما يكفي
 * لإثبات النقل (`unzip -p other.apk assets/devices.db | strings | grep zz_mm_canary`).
 */
class DeviceCatalogCanaryTest {

    private val assetRoot = listOf(
        "src/main/assets-internal",
        "manager/app/src/main/assets-internal",
    ).map(::File).firstOrNull { it.isDirectory } ?: File("src/main/assets-internal")

    /** الثلاثية المحروسة — تغييرها يغيّر الدليل كلّه، فلا تُعدَّل بلا قرار مكتوب. */
    private val canary = listOf("zz_mm_canary", "MMC-0001", "MaxManager Sentinel")

    @Test
    fun `صفّ المصيدة موجود في كتالوج الأجهزة المشحون`() {
        val db = File(assetRoot, "devices.db")
        assumeTrue("devices.db غير موجود؛ لم يُقَس", db.isFile)

        // ISO-8859-1: قراءة بايتًا ببايت بلا فكّ ترميز ولا استثناء على بايت غير صالح.
        val raw = db.readBytes().toString(Charsets.ISO_8859_1)

        canary.forEach { token ->
            assertTrue(
                "المصيدة «$token» غائبة من devices.db — حُذف الدليل على النقل بصمت",
                raw.contains(token),
            )
        }
    }

    @Test
    fun `الكتالوج ليس فارغًا ولا مجرَّدَ مصيدة`() {
        val db = File(assetRoot, "devices.db")
        assumeTrue("devices.db غير موجود؛ لم يُقَس", db.isFile)

        // ٥٢٬٤٣٥ صفًّا وقت الإضافة ⇒ الملفّ بالملايين. والحدّ هنا يمنع «كتالوجًا» أُفرغ
        // وبقيت فيه المصيدة وحدها، وهي حالة تبدو ناجحة في الاختبار الأول.
        assertTrue("devices.db أصغر من أن يحمل كتالوجًا حقيقيًّا", db.length() > 500_000)
    }
}

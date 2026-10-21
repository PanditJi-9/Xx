/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */

package nd.max.core.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * الحالة المضادّة ٩ من §٥.٤ — وهي الوحيدة التي **لا تُثبت باختبار سلوك بل باختبار بناء**:
 * «إعداد يحاول تفعيل مسار كتابة/تعليم قدرة ⇒ **لا يوجد حقل يمكّنه أصلًا**».
 *
 * فالدعوى هنا على العقد نفسه: قائمة الحقول المعلنة في `RemoteConfig.kt`. وأي حقل يُضاف غدًا (اسمه
 * `write`, `exec`, `sysfs`, `override`, `unlock` …) **يُسقط هذا الاختبار** حتى يراجعه إنسان —
 * وهذا الفرق بين «لا نكتب» سياسةً في الكلام و«لا نستطيع أن نكتب» بناءً مُقفلًا.
 *
 * ## ولماذا تُقرأ الحقول من **نصّ المصدر** لا من `SerialDescriptor`
 *
 * جُرّب الأول: `descriptor.elementCount` **غير موجود** في `kotlinx-serialization 1.11.0` (قِيس
 * بمُصرّف حقيقي: `unresolved reference`)، و`elementNames` ليس عضوًا في الواجهة عند هذه النسخة.
 * فبدل تعليق حرّاس البناء بواجهة تتغيّر، يُقرأ **ما هو مكتوب فعلًا**: أسماء `val` داخل
 * `data class` بعينه. وهي الطريقة التي لا تتغيّر بتغيّر إصدار مكتبة، وتكشف الحقل حتى لو حُجب
 * بـ`@Transient`.
 *
 * والفحص الثاني على نصّ العميل نفسه: لا `doOutput` ولا `POST`/`PATCH`/`DELETE` في `ConfigClient.kt`.
 */
class RemoteConfigArchitectureTest {

    private fun source(name: String): String {
        val candidates = listOf(
            File("manager/app/src/main/java/nd/max/core/remote/$name"),
            File("../manager/app/src/main/java/nd/max/core/remote/$name"),
            File("../../manager/app/src/main/java/nd/max/core/remote/$name"),
            File("app/src/main/java/nd/max/core/remote/$name"),
        )
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("لم يُعثر على مصدر $name في هذه الشجرة — الفحص المعماري غير مُقيَّم")
    }

    /** أسماء الخصائص (`val`/`var`) المعلنة داخل `data class <name>( … )` في الملفّ نفسه. */
    private fun declaredFields(fileName: String, className: String): Set<String> {
        val text = source(fileName)
        val start = text.indexOf("data class $className(")
        assertTrue("`$className` لم يُعثر عليه في $fileName", start >= 0)
        val body = text.substring(start + "data class $className(".length)
        val end = body.indexOf("\n)")
        assertTrue("حصن `$className` غير مغلق كما هو متوقّع", end >= 0)
        return Regex("""\b(?:val|var)\s+(\w+)\s*:""")
            .findAll(body.substring(0, end))
            .map { it.groupValues[1] }
            .toSet()
    }

    @Test
    fun `the signed payload cannot carry a new capability`() {
        assertEquals(
            "حقلٌ جديد في العقد يجب أن يُراجَع يدويًّا قبل أن يُقرأ — لا يُضاف صامتًا",
            setOf(
                "schema", "revision", "issuedAt", "expiresAt", "features",
                "announcement", "latest", "stats", "subscription", "promos", "changelog",
                // `updates` — مُراجَع يدويًّا (٢٠٢٦-١٠-٢١) وهو **إسكات لا قدرة**: حقلٌ منطقيٌّ
                // واحد يُطفئ نوافذ التحديث التلقائية، ولا يستطيع تشغيل تنزيلٍ ولا تثبيتٍ ولا
                // تجاوز حجب النسخة. والفحص اليدويّ يبقى يرى ما وُجد، والحارس الأمنيّ لا يُلمس.
                "updates",
            ),
            declaredFields("RemoteConfig.kt", "RemotePayload"),
        )
    }

    @Test
    fun `a feature key can only turn a surface off`() {
        assertEquals(
            "مفتاح الميزة: حالةٌ بنصّ (on/subscribe/off) وحدٌّ أدنى للإصدار واسمٌ ونصوص — لا فعلًا",
            setOf("enabled", "minVersionCode", "state", "title", "messageAr", "messageEn"),
            declaredFields("RemoteConfig.kt", "RemoteFeature"),
        )
    }

    @Test
    fun `the announcement carries text and a deadline, not a command`() {
        assertEquals(
            "الإعلان نصّ ومدى، ولا حقل فعل",
            setOf("enabled", "textAr", "textEn", "until"),
            declaredFields("RemoteConfig.kt", "RemoteAnnouncement"),
        )
        assertEquals(
            "وسياسة الإحصاءات ثلاثة أرقام لا صلاحيات",
            setOf("collect", "sampleRate", "ttlHours"),
            declaredFields("RemoteConfig.kt", "RemoteStats"),
        )
    }

    @Test
    fun `the wire contract is exactly these classes`() {
        val text = source("RemoteConfig.kt")
        val wire = Regex("""@Serializable\s+data class (\w+)\(""")
            .findAll(text)
            .map { it.groupValues[1] }
            .toSet()
        assertEquals(
            "أي صنف يُوسَم `@Serializable` في العقد يعبر الشبكة — فلا يُضاف صامتًا",
            setOf(
                "RemoteFeature", "RemoteAnnouncement", "RemoteLatest", "RemoteStats",
                "RemotePayload", "RemoteSubscription", "RemotePromo", "RemoteRelease", "SignedEnvelope",
                // قيمةٌ واحدة (`disabled`) — لا نصّ أمر ولا مسار على الجهاز.
                "RemoteUpdates",
            ),
            wire,
        )
    }

    @Test
    fun `the resolved view carries no capability either`() {
        assertEquals(
            "العرض المحلول حقولُه معروفة: قيمٌ وتشخيص، لا فعل",
            setOf(
                "revision", "issuedAtMillis", "expiresAtMillis", "features",
                "announcement", "latest", "stats", "subscription", "promos", "changelog",
                // سياسة عرضٍ لا فعل: إسكات نوافذ التحديث التلقائية (نيابةً عن المالك المنشور).
                "updates",
                "unknownFeatureKeys",
            ),
            declaredFields("RemoteConfig.kt", "RemoteConfig"),
        )
    }

    @Test
    fun `no field name anywhere in the contract smells like a write path`() {
        val forbidden = listOf(
            "write", "exec", "shell", "sysfs", "node", "path", "file", "override",
            "allow", "grant", "unlock", "root", "privilege", "command", "intent", "hook",
        )
        val names = listOf(
            "RemotePayload" to "RemoteConfig.kt",
            "RemoteFeature" to "RemoteConfig.kt",
            "RemoteAnnouncement" to "RemoteConfig.kt",
            "RemoteStats" to "RemoteConfig.kt",
            "RemoteSubscription" to "RemoteConfig.kt",
            "RemotePromo" to "RemoteConfig.kt",
            "RemoteRelease" to "RemoteConfig.kt",
            "RemoteUpdates" to "RemoteConfig.kt",
        ).flatMap { (className, fileName) -> declaredFields(fileName, className) }
        names.forEach { name ->
            forbidden.forEach { smell ->
                assertFalse(
                    "الحقل «$name» يلمس «$smell» — العقد يجب ألّا يحمل فعلًا على الجهاز",
                    name.lowercase().contains(smell),
                )
            }
        }
    }

    @Test
    fun `the client can only read`() {
        val text = source("ConfigClient.kt")
        listOf("doOutput", "\"POST\"", "\"PATCH\"", "\"DELETE\"", "\"PUT\"").forEach { token ->
            assertFalse(
                "ConfigClient لا يملك مسار كتابة — وجدتُ «$token»",
                text.contains(token),
            )
        }
        assertTrue(
            "والقراءة موجودة: `GET` صريحًا",
            text.contains("requestMethod = \"GET\""),
        )
    }

    @Test
    fun `the private key is nowhere in the tree`() {
        // §٥.٢: التوقيع في سرّ دالّة الخادم. وأي ظهور لاسمه في العميل = سرّ في الحزمة.
        listOf("ConfigSignature.kt", "RemoteConfig.kt", "ConfigClient.kt").forEach { name ->
            val text = source(name)
            listOf("PRIVATE KEY", "begin EC PRIVATE", "privateKey =").forEach { token ->
                assertFalse("مادة مفتاح خاصّ في $name (وجدت «$token»)", text.contains(token))
            }
        }
    }
}

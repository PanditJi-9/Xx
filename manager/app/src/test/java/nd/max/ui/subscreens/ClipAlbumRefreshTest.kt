/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.ui.subscreens

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * **ألبوم لا يُخفي ما سُجّل للتوّ، ولا يحذف بلمسة لا رجعة فيها.**
 *
 * عيبان مقيسان في الشاشة الأولى: العودة من اللعبة (أو من إعدادات الإذن) كانت تُحدّث القائمة فقط
 * إذا تغيّر الإذن — فالتسجيل الجديد لا يظهر حتى تُعاد الشاشة؛ والحذف على ما قبل ٣٠ كان مباشرًا بلا
 * سؤال، لأن حوار الموافقة النظامي لا وجود له هناك. هذا الحارس يثبّت الإصلاحين نصًّا، ويثبت أن
 * المفاتيح الثلاثة الجديدة موجودة في اللغتين معًا (ADR-14).
 *
 * وحدّه المُعلَن: فحص نصّي على القصد، لا تشغيلًا على جهاز — ظهور المقطع فعلًا بعد المسح، وقيام
 * الحوار، والحذف، كلها تحتاج جهاز المالك.
 */
class ClipAlbumRefreshTest {
    private lateinit var sourceRoot: File

    @Before
    fun locate() {
        val found = listOf(
            File("src/main/java/nd/max"), File("app/src/main/java/nd/max"),
            File("../app/src/main/java/nd/max"), File("manager/app/src/main/java/nd/max")
        ).firstOrNull { it.isDirectory }
        assumeTrue("Cannot locate nd.max sources", found != null)
        sourceRoot = found!!
    }

    private fun text(relative: String) = File(sourceRoot, relative).readText()

    private fun screen() = text("ui/subscreens/ClipAlbumScreen.kt")

    @Test
    fun returningRereadsEvenWhenThePermissionDidNotChange() {
        val source = screen()
        val anchor = "LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {"
        val start = source.indexOf(anchor)
        assertTrue("العودة لا تعيد القراءة: مرساة ON_RESUME غائبة", start >= 0)
        val block = source.substring(start, source.indexOf("\n    }", start))
        assertTrue(
            "العودة تُحدّث القائمة فقط عند تغيّر الإذن، فالتسجيل الجديد يبقى مخفيًّا",
            block.contains("\n        refresh++")
        )
        assertFalse(
            "refresh++ ما زال داخل شرط تغيّر الإذن",
            block.contains("if (now != permitted) {")
        )
    }

    @Test
    fun everyDeleteAsksFirst() {
        val source = screen()
        assertTrue(
            "الحذف يعود مباشرًا بلا سؤال (وعلى ما قبل ٣٠ لا حوار نظام)",
            source.contains("onDelete = { pendingDelete = clip }")
        )
        assertFalse(
            "مسار الحذف المباشر ما زال موجودًا",
            source.contains("onDelete = { delete(clip) }")
        )
        assertTrue("لا حوار تأكيد في الشاشة", source.contains("AlertDialog("))
        assertTrue(
            "زرّ التأكيد لا يستعمل نصّه الخاص",
            source.contains("R.string.clip_album_delete_confirm")
        )
    }

    @Test
    fun theDeleteDialogSpeaksBothLanguages() {
        val english = text("../../../res/values/strings.xml")
        val arabic = text("../../../res/values-ar/strings.xml")
        listOf(
            "clip_album_delete_title", "clip_album_delete_message", "clip_album_delete_confirm"
        ).forEach { key ->
            assertTrue("مفتاح $key غائب عن الإنجليزية", english.contains("name=\"$key\""))
            assertTrue("مفتاح $key غائب عن العربية", arabic.contains("name=\"$key\""))
        }
    }
}

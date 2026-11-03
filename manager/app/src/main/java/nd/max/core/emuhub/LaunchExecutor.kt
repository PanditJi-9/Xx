/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.emuhub

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * تنفيذ وصفة تشغيل واحدة — **بالنيّة فقط، وبلا مسار جذر**.
 *
 * ### لماذا لا `am start` بجذر هنا
 * العقد الموثَّق (‏RetroArch) يعمل بنيّة صريحة عاديّة، فلا حاجة إلى shell. والمسار الجذريّ القائم
 * في `ActivityLauncher.launchAsRoot` **لا يمرّر إضافات**، فاستعماله لتشغيل لعبة يعني إرسال أمر ناقص
 * يبدو ناجحًا — وهذا أسوأ من الامتناع. فحين يمنع المحاكي الإطلاق الخارجيّ ([LaunchReport.NeedsRoot])
 * تقول الشاشة السبب، ولا يُفتح باب `su` ثانٍ في المستودع (‏ADR-11 · قاعدة «كاتب واحد»).
 *
 * ### وما لا يُدّعى
 * `startActivity` تعني **سُلّمت النيّة**، لا «بدأت اللعبة». الرسائل كلها في `strings.xml` تقول ذلك
 * بنصّها، والبلاغ `libretro/RetroArch#17433` يذكّر بأن العقد نفسه قد يعطب بإصدار.
 */
object LaunchExecutor {

    /**
     * @param recipes الوصفات المتاحة؛ الافتراضي هو المُدرجة، ويُمرَّر غيرها في الاختبار.
     */
    fun launch(
        context: Context,
        entry: RomEntry,
        recipes: List<LaunchRecipe> = LaunchRecipes.builtins,
    ): LaunchReport {
        val part = entry.parts.firstOrNull() ?: return LaunchReport.NoHandler
        val recipe = entry.system?.let { system -> recipes.firstOrNull { system in it.systems } }
            ?: return chooser(context, part)

        if (!installed(context, recipe.packageName)) return chooser(context, part)

        val values = mapOf(
            LaunchVariable.ROM_PATH to part.uri,
            LaunchVariable.ROM_DIR to part.parent,
            LaunchVariable.ROM_NAME to part.name,
            LaunchVariable.ROM_NAME_NO_EXT to RomSystems.baseName(part.name),
            LaunchVariable.PACKAGE to recipe.packageName,
        )
        val extras = when (val resolution = recipe.resolve(values)) {
            is Resolution.Ready -> resolution.extras
            is Resolution.Missing -> return LaunchReport.Refused(resolution.token)
        }

        val activity = recipe.activityName ?: return chooser(context, part)
        val uri = Uri.parse(part.uri)
        val intent = Intent(recipe.action ?: Intent.ACTION_VIEW)
            .setClassName(recipe.packageName, activity)
            .setDataAndType(uri, mimeOf(context, uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri(part.name, uri)
        extras.forEach { (key, value) -> intent.putExtra(key, value) }

        return try {
            context.startActivity(intent)
            LaunchReport.HandedTo(recipe.packageName)
        } catch (_: SecurityException) {
            // بيان المحاكي لا يُصدّر نشاطه ⇒ لا إطلاق خارجيّ. ولا جذر هنا (انظر رأس الملفّ).
            LaunchReport.NeedsRoot
        } catch (_: ActivityNotFoundException) {
            LaunchReport.NoHandler
        } catch (_: Exception) {
            LaunchReport.Refused(null)
        }
    }

    /** منتقي النظام: تطبيق آخر يشغّل اللعبة، والنصّ المعروض يقول ذلك. */
    private fun chooser(context: Context, part: RomFile): LaunchReport = try {
        val uri = Uri.parse(part.uri)
        // `NEW_TASK` لازمة هنا: السياق من `@ApplicationContext` لا من نشاط، وبدونها يرفض النظام
        // الإطلاق من غير نشاط. والنمط نفسه في المسار الصريح أعلاه.
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, mimeOf(context, uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.clipData = ClipData.newRawUri(part.name, uri)
        context.startActivity(Intent.createChooser(intent, null))
        LaunchReport.ChooserShown
    } catch (_: ActivityNotFoundException) {
        LaunchReport.NoHandler
    } catch (_: Exception) {
        LaunchReport.Refused(null)
    }

    /** نوع الملفّ من مزوّده؛ المجهول `application/octet-stream` لا نوع مخترع. */
    private fun mimeOf(context: Context, uri: Uri): String =
        runCatching { context.contentResolver.getType(uri) }.getOrNull() ?: "application/octet-stream"

    private fun installed(context: Context, packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)
}

/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nd.max.ui.util
import nd.max.core.platform.getChipsetName


import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nd.max.core.diagnostics.DeviceBlueprint
import nd.max.core.security.AssetVault
import nd.max.core.diagnostics.DiagnosticCenter
import nd.max.core.jni.PredictorBridge


/** أصل السكربت المختوم: صريح في التنقيح، مختوم في الإصدار — يقرأه [AssetVault]. */
private const val DIAGNOSTIC_SCRIPT_ASSET = "scripts/diagnostics.sh"

/** اقتباس shell آمن: بين اقتباسين مفردين، وكل اقتباس داخليّ يُهرَّب. */
private fun shellQuote(value: String): String =
    "'" + value.replace("'", "'\\''") + "'"

suspend fun dumpDiagnosticLogs(context: Context, saveToDownloads: Boolean): File? = withContext(Dispatchers.IO) {
    // كتلة مركز التشخيص: تُكتب أولًا لأن السكربت أدناه يستهلكها كملف.
    val diagFile = File(context.cacheDir, "diag_center_block.txt")
    runCatching {
        val nativePart = if (PredictorBridge.nativeAvailable) {
            // حالة وكيل RL ولقطة التوأم: خطوات التدريب الفعلية وتحليل
            // التوأم — لإعادة تشغيل جهاز المستخدم على محرك المطور.
            "rlState=" + (PredictorBridge.rlState?.let { s ->
                "steps=${s.steps} counts=${s.actionCounts} model=${s.modelPath}"
            } ?: "unavailable") + "\n" +
                "twinAnalysis=" + (PredictorBridge.getTwinAnalysis() ?: "null") + "\n"
        } else {
            ""
        }
        diagFile.writeText(
            DiagnosticCenter.formatBlock() +
                "\nexportedAt=" + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()) +
                "\nnative=" + (if (PredictorBridge.nativeAvailable) "loaded" else "MISSING") + "\n" +
                nativePart
        )
    }
    // بصمة الجهاز الكاملة: هوية + طوبولوجيا السياسات + قدرات كل ميزة.
    val blueprintFile = File(context.cacheDir, "device_blueprint.txt")
    runCatching { blueprintFile.writeText(DeviceBlueprint.generate(context)) }

    // Bersihkan file log lama di cache
    val cacheDir = context.cacheDir
    cacheDir.listFiles()?.forEach { file ->
        if (file.name.startsWith("MaxManager_Logs_") && file.name.endsWith(".tar.gz")) {
            file.delete()
        }
    }

    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "MaxManager_Logs_$timeStamp.tar.gz"
    val appUid = context.applicationInfo.uid

    // UBAH: Gunakan /storage/emulated/0 agar lewat FUSE Android (Otomatis ngatur permission)
    val targetPath = if (saveToDownloads) {
        "/storage/emulated/0/Download/$fileName"
    } else {
        "${context.cacheDir.absolutePath}/$fileName"
    }
    
    val realDeviceName = getRealDeviceName(context)
    val chipsetName = getChipsetName(context)
    val appVersion = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) {
        "Unknown"
    }
    
    val serviceBin = nd.max.MaxManagerPaths.serviceBin()

    // السكربت الحرفيّ يعيش في أصل مختوم (`assets-internal/scripts/diagnostics.sh`) لا في DEX:
    // كان **أكبر كتلة شرح مقروءة** في الحزمة (قِيس ٤٬١٧٥ حرفًا منها ٨١٥ حرفًا عربيًّا)، فيقرأ
    // من يفكّ الحزمة سبب كل سطر وقراره. والقيم تُمرَّر متغيّراتِ shell في مقدّمة صغيرة،
    // والسكربت نفسه لا يتغيّر حرفًا.
    val template = AssetVault.readText(context, DIAGNOSTIC_SCRIPT_ASSET) ?: return@withContext null
    val script = buildString {
        append("BLUEPRINT_FILE=").append(shellQuote(blueprintFile.absolutePath)).append('\n')
        append("DIAG_FILE=").append(shellQuote(diagFile.absolutePath)).append('\n')
        append("OUT_NAME=").append(shellQuote(fileName)).append('\n')
        append("TARGET_PATH=").append(shellQuote(targetPath)).append('\n')
        append("SAVE_TO_DOWNLOADS=").append(shellQuote(saveToDownloads.toString())).append('\n')
        append("APP_UID=").append(appUid).append('\n')
        append("SERVICE_BIN=").append(shellQuote(serviceBin)).append('\n')
        append("DEVICE_NAME=").append(shellQuote(realDeviceName)).append('\n')
        append("CHIPSET_NAME=").append(shellQuote(chipsetName)).append('\n')
        append("APP_VERSION=").append(shellQuote(appVersion)).append('\n')
        append(template)
    }


    val result = nd.max.core.privilege.RootCommands.cmd(script).exec()

    val finalFileForApp = File(targetPath)

    if (result.isSuccess && finalFileForApp.exists()) {
        if (saveToDownloads) {
            MediaScannerConnection.scanFile(context, arrayOf(finalFileForApp.absolutePath), null, null)
        }
        finalFileForApp
    } else {
        null
    }
}


fun getShareLogIntent(context: Context, file: File): Intent {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/gzip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(intent, "Send MaxManager Logs")
}


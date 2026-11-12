/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 *
 * MaxManager proprietary source. See LICENSE at the repository root: this file is
 * MaxManager-owned and carries no third-party licence obligations.
 */

package nd.max.core.gamespace

import android.content.Context
import java.io.File

/**
 * مخزن سجلّ الجلسات: **ملفّ واحد في `filesDir`**، بلا جذر. ولا يشارك `GameSessionStore` ملفّه:
 * ذلك أرشيف تقارير FPS، وهذا سجلّ وقت اللعب التلقائي.
 *
 * ### من يكتب
 * خدمة اللوحة الجانبية وحدها، عند انتقال اللعبة أو مغادرتها أو إيقاف الخدمة. الشاشة تقرأ فقط،
 * فلا يتنازع موضعان على الملفّ. والكتابة ذرّية (مؤقّت ثم إعادة تسمية) فالسجلّ لا يُقطع في منتصفه.
 *
 * ### والحدّ المعلن
 * إن حُذف الملفّ أو تلف، تُقرأ الجلسات الصالحة فقط؛ ولا تُخترع جلسة. والسجلّ يبقى محليًّا ولا
 * يُرسل إلى أي شبكة.
 */
object PlaySessionStore {
    private const val FILE_NAME = "game_play_sessions.tsv"

    private val lock = Any()

    fun read(context: Context): List<PlaySession> = synchronized(lock) {
        readUnlocked(context.applicationContext)
    }

    /** يسجّل جلسة صالحة؛ ويعود `false` بلا كتابة إن كانت غير صالحة أو مكرّرة. */
    fun record(context: Context, session: PlaySession): Boolean = synchronized(lock) {
        if (!PlaySessionLedger.valid(session)) return false
        val app = context.applicationContext
        val current = readUnlocked(app)
        val next = PlaySessionLedger.append(current, session)
        if (next == current) return false
        writeUnlocked(app, PlaySessionLedger.encode(next))
    }

    private fun file(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    private fun readUnlocked(context: Context): List<PlaySession> = runCatching {
        val target = file(context)
        if (!target.isFile) emptyList() else PlaySessionLedger.parse(target.readText())
    }.getOrDefault(emptyList())

    private fun writeUnlocked(context: Context, text: String): Boolean = runCatching {
        val target = file(context)
        // لا نمحو ملفًّا تالفًا قبل أن نحتفظ بنسخة منه: الدليل يبقى، والقراءة التالية لا تدّعي فراغًا.
        if (target.isFile && target.length() > 0L) {
            val previous = runCatching { target.readText() }.getOrNull()
            if (!previous.isNullOrBlank() && PlaySessionLedger.parse(previous).isEmpty()) {
                runCatching {
                    val corrupt = File(target.parentFile, target.name + ".corrupt")
                    corrupt.writeText(previous)
                }
            }
        }
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(text)
        if (!temp.renameTo(target)) {
            target.writeText(text)
            temp.delete()
        }
        true
    }.getOrDefault(false)
}

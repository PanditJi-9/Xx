/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package nd.max.core.display

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import nd.max.core.platform.SettingsAccess
import nd.max.receiver.NightLightScheduleReceiver
import java.util.Calendar

/**
 * جدولة الوضع الليلي: نافذة زمنية تكتب **المفتاح نفسه** الذي تكتبه الشاشة والبلاطة.
 *
 * **ولا مفاتيح خاصة بالضوء:** `secure/night_display_activated` هو ما يقرؤه النظام؛
 * فلو خزّنّا حالتنا في تفضيل خاصّ لصار لدينا ضوءان — واحد يظنّه التطبيق، وآخر
 * يعرضه النظام. هنا الحالة **مقروءة** من المنصّة عند كل تغيير، لا مُدَّعاة.
 *
 * **والمنبّه غير دقيق بقصد (`setAndAllowWhileIdle`):** المنبّهات الدقيقة تحتاج
 * `SCHEDULE_EXACT_ALARM`، وهو إذن لا يُمنح لهذا النوع من العمل ولم يُضَف. النظام
 * يجمع المنبّهات ليوفّر البطارية، فقد تتأخّر التبديلة دقائق — وهذا مقبول في ضوء
 * ليلي، ومكتوب في وصف الخيار لا مخفيًّا.
 */
object NightLightSchedule {

    const val ACTION_APPLY = "nd.max.action.APPLY_NIGHT_LIGHT_SCHEDULE"

    private const val PREFS = "maxmanager_night_schedule"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_START = "start_minute"
    private const val KEY_END = "end_minute"

    /** ٢٢:٠٠ → ٠٧:٠٠: نافذة مسائية عادية، والمالك يغيّرها من الشاشة. */
    const val DEFAULT_START_MINUTE = 22 * 60
    const val DEFAULT_END_MINUTE = 7 * 60

    private const val REQUEST_CODE = 4711
    private const val NAMESPACE = "secure"
    private const val KEY = "night_display_activated"
    private const val MINUTE_MS = 60_000L

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun startMinute(context: Context): Int = prefs(context).getInt(KEY_START, DEFAULT_START_MINUTE)

    fun endMinute(context: Context): Int = prefs(context).getInt(KEY_END, DEFAULT_END_MINUTE)

    /**
     * تشغيل الجدولة أو إيقافها.
     *
     * عند التشغيل تُطبَّق النافذة **فورًا** ثم تُجدول: من يفعّل الجدولة في منتصف
     * النافذة يتوقّع أن يرى أثرها الآن، لا «من الغد».
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            applyNow(context)
            ensureScheduled(context)
        } else {
            cancel(context)
        }
    }

    /** تغيير طرفَي النافذة؛ ويُطبَّق فورًا إن كانت الجدولة مفعّلة. */
    fun setWindow(context: Context, startMinute: Int, endMinute: Int) {
        prefs(context).edit()
            .putInt(KEY_START, NightLightWindow.normalise(startMinute))
            .putInt(KEY_END, NightLightWindow.normalise(endMinute))
            .apply()
        if (isEnabled(context)) {
            applyNow(context)
            ensureScheduled(context)
        }
    }

    /**
     * يكتب الحالة التي تقتضيها النافذة الآن. يعيد هل قُبلت الكتابة.
     *
     * `false` ليس خطأً صامتًا: معناه أن المنصّة رفضت المفتاح (لا
     * `WRITE_SECURE_SETTINGS` ولا جذر)، وتُبقى الحالة الظاهرة كما هي بدل ادّعاء
     * تبديل لم يقع.
     */
    fun applyNow(context: Context, nowMinute: Int = nowMinute()): Boolean {
        if (!isEnabled(context)) return false
        val active = NightLightWindow.isActive(nowMinute, startMinute(context), endMinute(context))
        return runCatching {
            SettingsAccess.put(NAMESPACE, KEY, if (active) "1" else "0")
        }.getOrDefault(false)
    }

    /** دقيقة اليوم محليًّا؛ تُمرَّر من الاختبار في صورة زمن مطلق. */
    fun nowMinute(atMs: Long = System.currentTimeMillis()): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = atMs
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    /**
     * يضمن وجود منبّه واحد عند التبديلة القادمة — لا منبّه لكلّ طرف.
     *
     * بعد كل تبديلة يُعاد الجدول من داخل المستقبل، فيبقى منبّه واحد دائمًا:
     * لا تراكم ولا نسخة قديمة تُوقظ الجهاز ليلًا بعد أن غيّر المالك النافذة.
     */
    fun ensureScheduled(context: Context) {
        val app = context.applicationContext
        if (!isEnabled(app)) {
            cancel(app)
            return
        }
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        val minutes = NightLightWindow.nextSwitch(nowMinute(), startMinute(app), endMinute(app))
        val at = System.currentTimeMillis() + minutes * MINUTE_MS
        runCatching {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(app))
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        val intent = pending(app)
        runCatching { manager.cancel(intent) }
        intent.cancel()
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, NightLightScheduleReceiver::class.java).setAction(ACTION_APPLY)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

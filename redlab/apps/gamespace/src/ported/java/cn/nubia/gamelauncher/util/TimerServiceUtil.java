package dev.redlab.gamespace.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.Date;
import java.util.Locale;

/* loaded from: classes2.dex */
public class TimerServiceUtil {
    private static final String DATE_KEY = "date";
    private static final String SHARED_PERFERENCES_FILES = "game_launcher_timer";
    public static final String SHARED_PREFERENCES_NAME = "data";
    private static final String TAG = "TimerServiceUtil";
    private static final String TIMER_KEY = "timer";
    public static final String TIME_OUT_4H = "time_out_4h";
    public static final String WEEKLY_TIME_OUT = "weekly_time_out";
    private static Date mDate;

    public static boolean isUpdateDate(Context context) {
        Date date = new Date();
        Date readDataTosharedPrefs = readDataTosharedPrefs(context);
        mDate = readDataTosharedPrefs;
        if (readDataTosharedPrefs == null) {
            return resetDatas();
        }
        if ((date.getDate() > mDate.getDate() || date.getMonth() > mDate.getMonth() || date.getYear() > mDate.getYear()) && date.getHours() >= 5) {
            return resetDatas();
        }
        return false;
    }

    public static boolean isGameTimeWeeklyRemind(Context context) {
        Date date = new Date();
        int hours = date.getHours();
        String format = String.format(Locale.US, "%ta", date);
        Log.i(TAG, "week:" + format + ",hours:" + hours);
        if (format != null) {
            if ((format.equals("Mon") && hours >= 9) || (format.equals("Tues") && hours < 9)) {
                return true;
            }
            if (!getWeeklyTimeoutValue(context)) {
                setWeeklyTimeoutValue(context, true);
            }
        }
        return false;
    }

    private static boolean resetDatas() {
        mDate = new Date();
        return true;
    }

    public static Date readDataTosharedPrefs(Context context) {
        long j = context.getSharedPreferences(SHARED_PERFERENCES_FILES, 0).getLong(DATE_KEY, -1L);
        if (j == -1) {
            return null;
        }
        return new Date(j);
    }

    public static int readTimerTosharedPrefs(Context context) {
        return context.getSharedPreferences(SHARED_PERFERENCES_FILES, 0).getInt("timer", -1);
    }

    public static void writeDataTosharedPrefs(Context context, long j) {
        context.getSharedPreferences(SHARED_PERFERENCES_FILES, 0).edit().putLong(DATE_KEY, j).apply();
    }

    public static void writeTimerTosharedPrefs(Context context, int i) {
        context.getSharedPreferences(SHARED_PERFERENCES_FILES, 0).edit().putInt("timer", i).apply();
    }

    public static void setTimeOut4HValue(Context context) {
        SharedPreferences.Editor edit = context.getSharedPreferences("data", 0).edit();
        edit.putBoolean(TIME_OUT_4H, false);
        edit.apply();
    }

    public static boolean getTimeOut4HValue(Context context) {
        return context.getSharedPreferences("data", 0).getBoolean(TIME_OUT_4H, true);
    }

    public static void setWeeklyTimeoutValue(Context context, boolean z) {
        SharedPreferences.Editor edit = context.getSharedPreferences("data", 0).edit();
        edit.putBoolean(WEEKLY_TIME_OUT, z);
        edit.apply();
        Log.i(TAG, "setWeeklyTimeoutValue " + z);
    }

    public static boolean getWeeklyTimeoutValue(Context context) {
        boolean z = context.getSharedPreferences("data", 0).getBoolean(WEEKLY_TIME_OUT, false);
        Log.i(TAG, "getWeeklyTimeoutValue " + z);
        return z;
    }
}

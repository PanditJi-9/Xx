package cn.nubia.gamecenter.settings.utils;

import android.os.Build;
import android.util.Log;

/* loaded from: classes.dex */
public class LogUtil {
    public static final int ASSERT = 7;
    public static final int DEBUG = 3;
    public static final int ERROR = 6;
    public static final int INFO = 4;
    private static final boolean ISDEBUG = !"user".equals(Build.TYPE);
    public static final String LOG_TAG = "gcs_Settings";
    public static final int VERBOSE = 2;
    public static final int WARN = 5;

    public static int v(String str, String str2) {
        if (ISDEBUG) {
            return Log.v(LOG_TAG, normalizeMsg(str, str2));
        }
        return 0;
    }

    public static int v(String str, String str2, Throwable th) {
        if (ISDEBUG) {
            return Log.v(LOG_TAG, normalizeMsg(str, str2), th);
        }
        return 0;
    }

    public static int d(String str, String str2) {
        if (ISDEBUG) {
            return Log.d(LOG_TAG, normalizeMsg(str, str2));
        }
        return 0;
    }

    public static int d(String str, String str2, Throwable th) {
        if (ISDEBUG) {
            return Log.d(LOG_TAG, normalizeMsg(str, str2), th);
        }
        return 0;
    }

    public static int i(String str, String str2) {
        return Log.i(LOG_TAG, normalizeMsg(str, str2));
    }

    public static int i(String str, String str2, Throwable th) {
        return Log.i(LOG_TAG, normalizeMsg(str, str2), th);
    }

    public static int w(String str, String str2) {
        return Log.w(LOG_TAG, normalizeMsg(str, str2));
    }

    public static int w(String str, String str2, Throwable th) {
        return Log.w(LOG_TAG, normalizeMsg(str, str2), th);
    }

    public static boolean isLoggable(String str, int i) {
        return Log.isLoggable(str, i);
    }

    public static int w(String str, Throwable th) {
        return Log.w(LOG_TAG, th);
    }

    public static int e(String str, String str2) {
        return Log.e(LOG_TAG, normalizeMsg(str, str2));
    }

    public static int e(String str, String str2, Throwable th) {
        return Log.e(LOG_TAG, normalizeMsg(str, str2), th);
    }

    public static int wtf(String str, String str2) {
        return Log.wtf(LOG_TAG, normalizeMsg(str, str2));
    }

    public static int wtf(String str, Throwable th) {
        return Log.wtf(LOG_TAG, th);
    }

    public static int wtf(String str, String str2, Throwable th) {
        return Log.wtf(LOG_TAG, normalizeMsg(str, str2), th);
    }

    public static String getStackTraceString(Throwable th) {
        return Log.getStackTraceString(th);
    }

    private static String normalizeMsg(String str, String str2) {
        StringBuilder sb = new StringBuilder(" [");
        sb.append(str).append("] ").append(str2);
        return sb.toString();
    }

    public static int e(Throwable th) {
        return Log.e(LOG_TAG, th.getMessage());
    }
}

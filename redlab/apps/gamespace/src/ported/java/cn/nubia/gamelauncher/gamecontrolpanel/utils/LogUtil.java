package dev.redlab.gamespace.gamecontrolpanel.utils;

import android.os.Build;
import android.util.Log;

/* loaded from: classes.dex */
public class LogUtil {
    public static final boolean DEBUG = !"user".equals(Build.TYPE);
    private static final String LOG_TAG = "GameControlPanel";

    public static void d(String str, String str2) {
        if (DEBUG) {
            Log.d("GameControlPanel[" + str + "]", str2);
        }
    }

    public static void i(String str, String str2) {
        Log.i("GameControlPanel[" + str + "]", str2);
    }

    public static void e(String str, String str2) {
        Log.e("GameControlPanel[" + str + "]", str2);
    }

    public static void e(String str, String str2, Throwable th) {
        Log.e("GameControlPanel[" + str + "]", str2, th);
    }

    public static void w(String str, String str2) {
        Log.w("GameControlPanel[" + str + "]", str2);
    }

    public static void w(String str, String str2, Throwable th) {
        Log.w("GameControlPanel[" + str + "]", str2, th);
    }
}

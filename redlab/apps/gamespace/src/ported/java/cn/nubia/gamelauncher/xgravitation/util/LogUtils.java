package dev.redlab.gamespace.xgravitation.util;

import android.util.Log;

/* loaded from: classes2.dex */
public class LogUtils {
    private static final String LOG_TAG = "X_Gravitation_Log";

    public static void d(String str, String str2) {
        Log.d("X_Gravitation_Log[" + str + "]", str2);
    }

    public static void d(String str, String str2, Exception exc) {
        Log.d("X_Gravitation_Log[" + str + "]", str2, exc);
    }

    public static void i(String str, String str2) {
        Log.i("X_Gravitation_Log[" + str + "]", str2);
    }

    public static void i(String str, String str2, Exception exc) {
        Log.i("X_Gravitation_Log[" + str + "]", str2, exc);
    }

    public static void e(String str, String str2) {
        Log.e("X_Gravitation_Log[" + str + "]", str2);
    }

    public static void e(String str, String str2, Exception exc) {
        Log.e("X_Gravitation_Log[" + str + "]", str2, exc);
    }
}

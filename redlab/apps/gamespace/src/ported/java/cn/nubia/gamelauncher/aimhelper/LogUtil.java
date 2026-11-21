package dev.redlab.gamespace.aimhelper;

import android.util.Log;

/* loaded from: classes.dex */
public class LogUtil {
    private static final boolean LOGD = false;
    private static final boolean LOGE = true;
    private static final boolean LOGI = true;
    private static final boolean LOGV = false;
    private static final boolean LOGW = true;
    private static final String TAG = "Crosshair";

    public static void d(Object obj, String str) {
    }

    public static void d(String str, String str2) {
    }

    public static void v(Object obj, String str) {
    }

    public static void v(String str, String str2) {
    }

    public static void i(String str, String str2) {
        Log.i(TAG, "[" + str + "] " + str2);
    }

    public static void w(String str, String str2) {
        Log.w(TAG, "[" + str + "] " + str2);
    }

    public static void e(String str, String str2) {
        Log.e(TAG, "[" + str + "] " + str2);
    }

    public static void i(Object obj, String str) {
        Log.i(TAG, "[" + obj.getClass().getSimpleName() + "] " + str);
    }

    public static void w(Object obj, String str) {
        Log.w(TAG, "[" + obj.getClass().getSimpleName() + "] " + str);
    }

    public static void e(Object obj, String str) {
        Log.e(TAG, "[" + obj.getClass().getSimpleName() + "] " + str);
    }
}

package dev.redlab.gamespace.redmagicplanet.util;

import android.util.Log;

/* loaded from: classes.dex */
public class LogUtil {
    private static final String LOG_TAG = "RedMagicVideoPlayer";

    public static void d(String str, String str2) {
        Log.d("RedMagicVideoPlayer[" + str + "]", str2);
    }

    public static void i(String str, String str2) {
        Log.i("RedMagicVideoPlayer[" + str + "]", str2);
    }

    public static void e(String str, String str2, Throwable th) {
        Log.e("RedMagicVideoPlayer[" + str + "]", str2, th);
    }
}

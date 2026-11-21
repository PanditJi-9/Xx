package com.zte.toolsdk.common;

import android.content.Context;
import android.net.Uri;

/* loaded from: classes2.dex */
public class SharedPrefs {
    public static final String ASR_COMPLETE = "asrComplete";
    public static final String KEY_NAME = "key";
    public static final String VALUE_NAME = "value";
    private static String sAuthority;
    private static Uri sContentUri;

    public static void init(Context context) {
        sAuthority = context.getPackageName() + ".sharedprefsprovider";
        sContentUri = Uri.parse("content://" + sAuthority + "/preferences");
    }

    public static String getAuthority(Context context) {
        if (sAuthority == null) {
            init(context);
        }
        return sAuthority;
    }

    public static Uri getContentUri(Context context) {
        if (sContentUri == null) {
            init(context);
        }
        return sContentUri;
    }
}

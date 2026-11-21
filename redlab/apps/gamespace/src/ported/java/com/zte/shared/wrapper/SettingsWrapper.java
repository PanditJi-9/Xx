package com.zte.shared.wrapper;

import android.app.ActivityManager;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;

/* loaded from: classes2.dex */
public class SettingsWrapper {
    public static final String TILES_SETTING = "cc_tiles";

    public static void putTilesSetting(Context context, List<String> list, List<String> list2) {
        String join = TextUtils.join(",", list2);
        Log.i("game_custom", "savespec:" + join);
        Settings.System.putStringForUser(context.getContentResolver(), TILES_SETTING, join, ActivityManager.getCurrentUser());
    }
}

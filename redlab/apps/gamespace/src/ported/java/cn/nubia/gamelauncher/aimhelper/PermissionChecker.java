package dev.redlab.gamespace.aimhelper;

import android.content.Context;
import android.provider.Settings;

/* loaded from: classes.dex */
public class PermissionChecker {
    public static boolean hasPermission(Context context) {
        LogUtil.i("PermissionChecker", "permission = " + Settings.canDrawOverlays(context));
        return Settings.canDrawOverlays(context);
    }
}

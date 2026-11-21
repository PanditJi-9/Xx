package com.zte.toolsdk.consumer;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

/* loaded from: classes2.dex */
public class AppStatusManager {
    private static final String TAG = "AppStatusManager";
    private final PackageManager mPackageManager;

    public AppStatusManager(Context context) {
        this.mPackageManager = context.getPackageManager();
    }

    public boolean isAppInstalled(String packageName) {
        try {
            this.mPackageManager.getPackageInfo(packageName, 1);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            Log.d(TAG, "应用未安装: " + packageName);
            return false;
        }
    }

    public boolean isAppEnabled(String packageName) {
        try {
            return this.mPackageManager.getApplicationInfo(packageName, 0).enabled;
        } catch (PackageManager.NameNotFoundException unused) {
            Log.d(TAG, "应用未安装: " + packageName);
            return true;
        }
    }
}

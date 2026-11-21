package cn.nubia.upgrade.api;

import android.content.Context;
import cn.nubia.upgrade.http.IDownLoadListener;
import cn.nubia.upgrade.http.IGetVersionListener;
import cn.nubia.upgrade.model.VersionData;

/* loaded from: classes2.dex */
public class NubiaUpgradeManager {
    private String mAppKey;
    private Context mContext;
    private String mSecretKey;

    public void addDownLoadListener(IDownLoadListener iDownLoadListener) {
    }

    public void debug(boolean z) {
    }

    public void getVersion(Context context, IGetVersionListener iGetVersionListener) {
    }

    public void install(Context context, VersionData versionData) {
    }

    public boolean isApkExist(VersionData versionData) {
        return false;
    }

    public void pauseDownload() {
    }

    public void setConfiguration(NubiaUpdateConfiguration nubiaUpdateConfiguration) {
    }

    public void startDownload(Context context, VersionData versionData) {
    }

    private NubiaUpgradeManager(Context context, String str, String str2) {
    }

    public static NubiaUpgradeManager getInstance(Context context, String str, String str2) {
        return new NubiaUpgradeManager(context, str, str2);
    }
}

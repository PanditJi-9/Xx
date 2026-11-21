package cn.nubia.upgrade.http;

import cn.nubia.upgrade.model.VersionData;

/* loaded from: classes2.dex */
public interface IGetVersionListener {
    void onError(int i);

    void onGetNewVersion(VersionData versionData);

    void onGetNoVersion();
}

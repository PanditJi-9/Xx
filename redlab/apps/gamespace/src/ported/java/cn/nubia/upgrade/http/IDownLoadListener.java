package cn.nubia.upgrade.http;

/* loaded from: classes2.dex */
public interface IDownLoadListener {
    void onDownloadComplete(String str);

    void onDownloadError(int i);

    void onDownloadPause();

    void onDownloadProgress(int i);

    void onResumeDownload();

    void onStartDownload();
}

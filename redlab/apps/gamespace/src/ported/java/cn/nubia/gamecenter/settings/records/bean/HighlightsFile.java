package cn.nubia.gamecenter.settings.records.bean;

import android.net.Uri;

/* loaded from: classes.dex */
public class HighlightsFile {
    private int duration;
    private long modified;
    private String path;
    private String title;
    private int type;
    private Uri uri;

    public HighlightsFile() {
    }

    public HighlightsFile(String str, String str2, long j, Uri uri, int i, int i2) {
        this.path = str;
        this.title = str2;
        this.modified = j;
        this.uri = uri;
        this.duration = i;
        this.type = i2;
    }

    public long getModified() {
        return this.modified;
    }

    public void setModified(long j) {
        this.modified = j;
    }

    public String getPath() {
        return this.path;
    }

    public void setPath(String str) {
        this.path = str;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String str) {
        this.title = str;
    }

    public Uri getUri() {
        return this.uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int i) {
        this.duration = i;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int i) {
        this.type = i;
    }
}

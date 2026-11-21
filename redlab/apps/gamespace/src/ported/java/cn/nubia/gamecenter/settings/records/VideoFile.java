package cn.nubia.gamecenter.settings.records;

import android.graphics.Bitmap;
import android.net.Uri;
import java.io.File;

/* loaded from: classes.dex */
public class VideoFile extends File {
    private int id;
    private Bitmap thumbImage;
    private int time;
    private String title;
    private Uri uri;
    private String videoPath;

    public VideoFile(String str, Uri uri, String str2, int i) {
        super(str);
        this.title = str2;
        this.uri = uri;
        this.time = i;
        this.videoPath = str;
    }

    public void setTitle(String str) {
        this.title = str;
    }

    public void setTime(int i) {
        this.time = i;
    }

    public String getTitle() {
        return this.title;
    }

    public Uri getUri() {
        return this.uri;
    }

    public String getVideoPath() {
        return this.videoPath;
    }

    public int getTime() {
        return this.time;
    }

    public int getId() {
        return this.id;
    }
}

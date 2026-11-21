package dev.redlab.gamespace.atmosphere;

import java.util.Date;

/* loaded from: classes.dex */
public class LiveAtmosphereBean {
    Date mDate;
    String mPackageName;
    String mPath;
    String mTitle;

    public LiveAtmosphereBean(String str, String str2, String str3, Date date) {
        this.mPath = str;
        this.mTitle = str2;
        this.mPackageName = str3;
        this.mDate = date;
    }

    public void setTitle(String str) {
        this.mTitle = str;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public void setDate(Date date) {
        this.mDate = date;
    }

    public Date getDate() {
        return this.mDate;
    }

    public void setPath(String str) {
        this.mPath = str;
    }

    public String getUrl() {
        return this.mPath;
    }

    public String toString() {
        return "LiveAtmosphereBean{mPath='" + this.mPath + "', mPackageName='" + this.mPackageName + "', mDate=" + this.mDate + '}';
    }
}

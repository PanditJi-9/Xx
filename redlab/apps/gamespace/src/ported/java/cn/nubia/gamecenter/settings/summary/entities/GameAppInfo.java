package cn.nubia.gamecenter.settings.summary.entities;

import android.graphics.drawable.Drawable;

/* loaded from: classes.dex */
public class GameAppInfo {
    public Drawable icon;
    public String label;
    public String pkgName;
    public long totalTimeInForeground;

    public String toString() {
        return "GameAppInfo{icon: " + this.icon + ", label: " + this.label + ", pkgName: " + this.pkgName + ", totalTimeInForeground: " + this.totalTimeInForeground + "}";
    }
}

package cn.nubia.gamecenter.settings.summary.entities;

import android.content.Intent;
import android.graphics.drawable.Drawable;

/* loaded from: classes.dex */
public class GameRecord {
    public Drawable icon;
    public String label;
    public Intent launchIntent;
    public String pkgName;
    public long totalTimeAllGames;
    public long totalTimeInForeground;

    public String toString() {
        return "GameRecord{icon: " + this.icon + ", label: " + this.label + ", pkgName: " + this.pkgName + ", totalTimeInForeground: " + this.totalTimeInForeground + ", totalTimeAllGames: " + this.totalTimeAllGames + ", launchIntent: " + this.launchIntent + "}";
    }
}

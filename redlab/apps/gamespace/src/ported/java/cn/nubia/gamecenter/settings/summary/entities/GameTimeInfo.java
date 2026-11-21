package cn.nubia.gamecenter.settings.summary.entities;

import android.graphics.drawable.Drawable;

/* loaded from: classes.dex */
public class GameTimeInfo {
    public static final int MAX_INDEX = 2;
    public static final int SUGGEST_EXCESS = 2;
    public static final int SUGGEST_FATIGUE = 1;
    public static final int SUGGEST_HEALTH = 0;
    public String label;
    public long mGameMaxTime;
    public String mGameTimeSpan;
    public long mGameTotalTime;
    public Drawable mMaxTimeAppIcon;
    public String mMaxTimePkgName;
    public int mSuggestType = 0;

    public String toString() {
        return "GameTimeInfo{mGameTimeSpan: " + this.mGameTimeSpan + ", mMaxTimePkgName: " + this.mMaxTimePkgName + ", label: " + this.label + ", mMaxTimeAppIcon: " + this.mMaxTimeAppIcon + ", mGameTotalTime: " + this.mGameTotalTime + ", mGameMaxTime: " + this.mGameMaxTime + ", mSuggestType: " + this.mSuggestType + "}";
    }
}

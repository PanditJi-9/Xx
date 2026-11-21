package cn.nubia.gamecenter.settings.summary.entities;

import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class OneGameTimeAndLaunchTimesInfo {
    public Drawable icon;
    public String label;
    public int launchTimes;
    public int launchTimes7Days;
    public List<Long> mDayTimesIn7Days = new ArrayList();
    public String pkgName;
    public long totalTimeInForeground;
    public long totalTimeInForeground7Days;

    public String toString() {
        return "OneGameTimeAndLaunchTimesInfo{icon: " + this.icon + ", label: " + this.label + ", pkgName: " + this.pkgName + ", totalTimeInForeground: " + this.totalTimeInForeground + ", launchTimes: " + this.launchTimes + ", totalTimeInForeground7Days: " + this.totalTimeInForeground7Days + ", launchTimes7Days: " + this.launchTimes7Days + "}";
    }
}

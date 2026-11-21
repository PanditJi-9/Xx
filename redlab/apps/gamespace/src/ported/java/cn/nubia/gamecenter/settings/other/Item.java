package cn.nubia.gamecenter.settings.other;

import android.content.Context;
import java.util.List;

/* loaded from: classes.dex */
public abstract class Item {
    public abstract boolean enable(List<String> list);

    public abstract String getKey();

    public abstract boolean getSettings(Context context);

    public int getSummary() {
        return 0;
    }

    public abstract int getTitle();

    public abstract void setSettings(Context context, boolean z);

    public void track(boolean z) {
    }
}

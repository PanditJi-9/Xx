package dev.redlab.gamespace.gamecontrolpanel;

import android.content.Context;
import android.content.Intent;
import dev.redlab.gamespace.gamecontrolpanel.utils.LogUtil;
import java.io.Serializable;

/* loaded from: classes.dex */
public class GamePlugData implements Serializable {
    private static final String TAG = "GamePlugData";
    public boolean isEnabled = true;
    private int mContentId;
    private Context mContext;
    private int mDrawables;
    private int mEnable;
    private Intent mIntent;
    private String mKey;

    public GamePlugData(int i, int i2, String str, Intent intent) {
        this.mDrawables = i;
        this.mContentId = i2;
        this.mKey = str;
        this.mIntent = intent;
    }

    public void deactivatePluginFunction(Context context) {
        if (this.mIntent != null) {
            LogUtil.i(TAG, "action =  " + this.mIntent.getAction() + "  ;; packageName = " + this.mIntent.getPackage());
            context.startService(this.mIntent);
        }
    }

    public String getKey() {
        return this.mKey;
    }

    public int getDrawables() {
        return this.mDrawables;
    }

    public int getContentId() {
        return this.mContentId;
    }

    public void setEnable(int i) {
        this.mEnable = i;
    }

    public int getEnable() {
        return this.mEnable;
    }
}

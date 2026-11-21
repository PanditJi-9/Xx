package dev.redlab.gamespace.gamecontrolpanel;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;
import dev.redlab.gamespace.gamecontrolpanel.utils.LogUtil;

/* loaded from: classes.dex */
public class Vibration4dGamePlugData extends GamePlugData {
    private static final String PLUG_TAG = "vibrate_plugin_enable";
    private static final String SETTING_GAME_4D_SWITCH = "nubia_4d_shocks";
    private static final String TAG = "Vibration4dGamePlugData";

    public Vibration4dGamePlugData(int i, int i2, String str, Intent intent) {
        super(i, i2, str, intent);
    }

    @Override // dev.redlab.gamespace.gamecontrolpanel.GamePlugData
    public void deactivatePluginFunction(Context context) {
        removePlug(context);
    }

    private void removePlug(Context context) {
        String key = getKey();
        try {
            if (TextUtils.isEmpty(key) || !key.contains(PLUG_TAG)) {
                return;
            }
            String replace = key.replace("_vibrate_plugin_enable", "");
            LogUtil.d(TAG, "removePlug, pkgname = " + replace);
            String string = Settings.Global.getString(context.getContentResolver(), SETTING_GAME_4D_SWITCH);
            if (TextUtils.isEmpty(string) || !string.contains(replace)) {
                return;
            }
            Settings.Global.putString(context.getContentResolver(), SETTING_GAME_4D_SWITCH, string.replace(replace + ",", ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

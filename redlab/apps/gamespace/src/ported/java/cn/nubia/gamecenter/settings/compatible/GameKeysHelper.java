package cn.nubia.gamecenter.settings.compatible;

import android.content.Context;
import android.provider.Settings;
import cn.nubia.gamecenter.settings.utils.ReflectUtilities;

/* loaded from: classes.dex */
public class GameKeysHelper {
    public static final String CHANGED_BY_SCREEN_POWER = "CHANGED_BY_SCREEN_POWER";
    public static final int DEFAULT_GAME_KEYS = 0;
    public static final int GAME_KEYS_OFF_ON = 1;
    public static final int GAME_KEYS_OFF_ON_CHAT = 8;
    public static final int GAME_KEYS_OFF_ON_DEFAULT = 0;
    public static final int GAME_KEYS_OFF_ON_LIGHT = 16;
    public static final int GAME_KEYS_OFF_ON_NOTIFICATION = 4;
    public static final int GAME_KEYS_OFF_ON_PHONE = 2;
    public static final int GAME_KEYS_OFF_ON_PHONE_DEFAULT = 0;
    public static final int GAME_KEYS_OFF_ON_SUPER_PERFORMANCE = 32;
    public static final String STR_GAME_KEYS_OFF_ON = "nubia_db_game_keys";

    GameKeysHelper() {
    }

    public static GameKeysHelper getDefault() {
        return new GameKeysHelper();
    }

    public int getGameKeysDBValue(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "nubia_db_game_keys", 0);
    }

    public void openSub(Context context, int i) {
        setGameKeysDBValue(context, i | getGameKeysDBValue(context));
    }

    public void closeSub(Context context, int i) {
        setGameKeysDBValue(context, (~i) & getGameKeysDBValue(context));
    }

    public void setGameKeysDBValue(Context context, int i) {
        Settings.Global.putInt(context.getContentResolver(), "nubia_db_game_keys", i);
    }

    public String readNodeValue(Context context) {
        return ReflectUtilities.readGameKeyNodeValue(context);
    }
}

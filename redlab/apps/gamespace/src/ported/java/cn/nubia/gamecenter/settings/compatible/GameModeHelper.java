package cn.nubia.gamecenter.settings.compatible;

import cn.nubia.gamecenter.settings.utils.LogUtil;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class GameModeHelper {
    public static final int CHANGE_NETWORK_GAME_MODE_OFF_ON = 128;
    public static final int FIX_LIGHT_GAME_MODE_OFF_ON = 256;
    private static final String GAME_MODE_CLASS_NAME = "cn.nubia.game.GameModeHelper";
    public static final int GAME_MODE_OFF_ON = 1;
    public static final String NETWORK_ACCELERATION_APP_LABEL_WHITE_LIST = "network_acceleration_app_label_white_list";
    public static final int NETWORK_ACCELERATION_OFF_ON = 64;
    public static final int NO_DISTURBE_GAME_MODE_OFF_ON = 4;
    public static final int NO_FUN_GAME_MODE_OFF_ON = 16;
    public static final int NO_KEYS_GAME_MODE_OFF_ON = 8;
    public static final int NO_NETWORK_GAME_MODE_OFF_ON = 32;
    public static final int SPEEDUP_GAME_MODE_OFF_ON = 2;
    public static final String STR_GAME_MODE_OFF_ON = "nubia_game_mode";
    public static final int VOICE_OPTIMIZATION_OFF_ON = 512;

    public static int getDefaultValue() {
        try {
            Class<?> cls = Class.forName(GAME_MODE_CLASS_NAME);
            Method declaredMethod = cls.getDeclaredMethod("getDefaultValue", new Class[0]);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(cls.newInstance(), new Object[0])).intValue();
        } catch (Exception e) {
            LogUtil.e(e);
            return 319;
        }
    }
}

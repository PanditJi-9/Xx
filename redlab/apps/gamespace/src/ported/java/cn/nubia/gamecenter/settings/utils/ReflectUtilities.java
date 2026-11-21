package cn.nubia.gamecenter.settings.utils;

import android.content.Context;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class ReflectUtilities {
    private static final String GAME_KEYS_CLASS_NAME = "cn.nubia.game.GameKeysHelper";
    private static final String GAME_KEYS_CTRL_APP_BACK_HOME = "cn.nubia.server.appmgmt.game.GameAppCtrl";
    private static final String TAG = "ReflectUtilities";

    public static String readGameKeyNodeValue(Context context) {
        try {
            Class<?> cls = Class.forName(GAME_KEYS_CLASS_NAME);
            Method declaredMethod = cls.getDeclaredMethod("readNodeValue", Context.class);
            declaredMethod.setAccessible(true);
            return (String) declaredMethod.invoke(cls.newInstance(), context);
        } catch (Exception e) {
            LogUtil.e(TAG, "readGameKeyNodeValue, " + e);
            return "";
        }
    }
}

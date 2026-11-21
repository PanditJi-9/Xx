package cn.nubia.config.android;

import cn.nubia.gamecenter.settings.utils.LogUtil;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class NubiaFeatureConfig {
    private static final String METHOD_GETSUBVALUE = "getSubValue";
    private static final String METHOD_GETVALUE = "getValue";
    private static final String NUBIA_CONFIG_CLASS_NAME = "android.util.NubiaConfig";
    private static final String TAG = "NubiaFeatureConfig";

    public static String getSubValue(String str, String str2, String str3) {
        try {
            Class<?> cls = Class.forName(NUBIA_CONFIG_CLASS_NAME);
            Method declaredMethod = cls.getDeclaredMethod(METHOD_GETSUBVALUE, String.class, String.class);
            declaredMethod.setAccessible(true);
            return (String) declaredMethod.invoke(cls.newInstance(), str, str2);
        } catch (Exception e) {
            LogUtil.e(TAG, "getSubValue " + e);
            return str3;
        }
    }

    public static String getSubValue(String str, String str2) {
        try {
            Class<?> cls = Class.forName(NUBIA_CONFIG_CLASS_NAME);
            Method declaredMethod = cls.getDeclaredMethod(METHOD_GETSUBVALUE, String.class, String.class);
            declaredMethod.setAccessible(true);
            return (String) declaredMethod.invoke(cls.newInstance(), str, str2);
        } catch (Exception e) {
            LogUtil.e(TAG, "getSubValue " + e);
            return "false";
        }
    }

    public static String getValue(String str) {
        try {
            Class<?> cls = Class.forName(NUBIA_CONFIG_CLASS_NAME);
            Method declaredMethod = cls.getDeclaredMethod(METHOD_GETVALUE, String.class);
            declaredMethod.setAccessible(true);
            return (String) declaredMethod.invoke(cls.newInstance(), str);
        } catch (Exception e) {
            LogUtil.e(TAG, "getValue " + e);
            return "false";
        }
    }
}

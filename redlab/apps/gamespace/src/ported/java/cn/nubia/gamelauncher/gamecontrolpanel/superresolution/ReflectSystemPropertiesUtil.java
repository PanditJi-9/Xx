package dev.redlab.gamespace.gamecontrolpanel.superresolution;

import android.content.Context;
import android.text.TextUtils;
import dev.redlab.gamespace.gamecontrolpanel.utils.LogUtil;
import java.lang.reflect.Method;

/* loaded from: classes.dex */
public class ReflectSystemPropertiesUtil {
    private static final String MASO_MAGIC_CONFIG_TEST = "maso.magic_config_test";
    private static final String METHOD_GET_STRING = "get";
    private static final String METHOD_GET_SYSTEMPROPERTIES = "getSystemProperties";
    private static final String METHOD_SET = "set";
    private static final String METHOD_SET_SYSTEMPROPERTIES = "setSystemProperties";
    private static final String PERSIST_MAGIC_SUPER_RESOLUTION = "persist.magic.super.resolution";
    private static final String REDMAGICAPPMANAGER_CLASS_NAME = "com.redmagic.os.RedMagicAppManager";
    private static final String REDMAGICAPPMANAGER_TRIGGER_CLASS_NAME = "com.redmagic.os.RedMagicAppManager$Trigger";
    private static final String SYSTEM_PROPERTIES_CLASS_NAME = "android.os.SystemProperties";
    private static final String TAG = "ReflectSystemPropertiesUtil";

    public static String get(Context context, String str, String str2) {
        String str3;
        try {
            Class<?> loadClass = context.getClassLoader().loadClass(SYSTEM_PROPERTIES_CLASS_NAME);
            Method method = loadClass.getMethod(METHOD_GET_STRING, String.class, String.class);
            if (TextUtils.isEmpty(str)) {
                str = PERSIST_MAGIC_SUPER_RESOLUTION;
            }
            str3 = (String) method.invoke(loadClass, str, str2);
        } catch (Exception e) {
            e.printStackTrace();
            str3 = null;
        }
        LogUtil.i(TAG, " get  value = " + str3);
        return str3;
    }

    public static Integer set(Context context, String str, String str2) {
        try {
            Class<?> loadClass = context.getClassLoader().loadClass(SYSTEM_PROPERTIES_CLASS_NAME);
            Method method = loadClass.getMethod(METHOD_SET, String.class, String.class);
            if (TextUtils.isEmpty(str)) {
                str = PERSIST_MAGIC_SUPER_RESOLUTION;
            }
            return (Integer) method.invoke(loadClass, str, str2);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Integer setDebugSwitch(Context context, String str) {
        try {
            Class<?> loadClass = context.getClassLoader().loadClass(SYSTEM_PROPERTIES_CLASS_NAME);
            return (Integer) loadClass.getMethod(METHOD_SET, String.class, String.class).invoke(loadClass, MASO_MAGIC_CONFIG_TEST, str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getSystemProperties(Context context, String str, int i) {
        try {
            Class<?> cls = context.getClassLoader().loadClass(REDMAGICAPPMANAGER_CLASS_NAME).getDeclaredClasses()[0];
            return Integer.valueOf((String) cls.getMethod(METHOD_GET_SYSTEMPROPERTIES, String.class, String.class).invoke(cls, PERSIST_MAGIC_SUPER_RESOLUTION, String.valueOf(i))).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void setSystemProperties(Context context, String str, Integer num) {
        try {
            Class<?> cls = context.getClassLoader().loadClass(REDMAGICAPPMANAGER_CLASS_NAME).getDeclaredClasses()[0];
            cls.getMethod(METHOD_SET_SYSTEMPROPERTIES, String.class, String.class).invoke(cls, PERSIST_MAGIC_SUPER_RESOLUTION, num.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

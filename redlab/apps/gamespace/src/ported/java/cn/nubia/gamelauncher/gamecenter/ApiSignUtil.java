package dev.redlab.gamespace.gamecenter;

import android.util.Log;
import cn.nubia.gamecenter.settings.applearning.AppDbSchema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class ApiSignUtil {
    public static final int API_SIGN = 1;
    public static final String CONNECTOR_TAG = "=";
    public static final int EXT_SIGN = 2;
    public static final String SIGN_CHECK = "tg6RNngGG5bY7RXp3uq4P8sLOB2DmdDS";
    public static final String SIGN_CHECK_EXT = "";

    private static String genSing(String str, String str2) {
        return str + CONNECTOR_TAG + str2;
    }

    private static String genUnit(String str, String[] strArr) {
        String str2 = "";
        if (strArr == null) {
            return "";
        }
        Arrays.sort(strArr);
        for (String str3 : strArr) {
            str2 = str2 + genSing(str, str3);
        }
        return str2;
    }

    private static String genSign(String[] strArr, Map<String, List<String>> map, StringBuffer stringBuffer, int i) {
        String[] strArr2;
        Arrays.sort(strArr);
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
        }
        for (String str : strArr) {
            if (map != null) {
                List<String> list = map.get(str);
                strArr2 = (String[]) list.toArray(new String[list.size()]);
            } else {
                Log.e("ApiSign", "error keyValues is null!");
                strArr2 = null;
            }
            stringBuffer.append(genUnit(str, strArr2));
        }
        if (i == 1) {
            stringBuffer.append(SIGN_CHECK);
        } else if (i == 2) {
            stringBuffer.append("");
        }
        try {
            return MD5Util.md5(stringBuffer.toString());
        } catch (Throwable th) {
            th.printStackTrace();
            throw new RuntimeException(th);
        }
    }

    public static void genSignForArkbase(Map<String, String> map) {
        map.put(AppDbSchema.OneKeyLockedAppsTable.Cols.LIMIT_TIME_STAMP, Integer.valueOf((int) (new Date().getTime() / 1000)).toString());
        HashMap hashMap = new HashMap();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (!hashMap.containsKey(key)) {
                hashMap.put(entry.getKey(), new ArrayList());
            }
            ((List) hashMap.get(key)).add(entry.getValue());
        }
        map.put("sign", genSign((String[]) hashMap.keySet().toArray(new String[hashMap.keySet().size()]), hashMap, null, 1));
    }
}

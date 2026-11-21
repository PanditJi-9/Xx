package com.zte.toolsdk.utils;

import android.content.Context;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;

/* loaded from: classes2.dex */
public class ResourceUtil {
    private static final ConcurrentHashMap<String, Integer> RESOURCE_CACHE = new ConcurrentHashMap<>();

    public static int getDrawableRes(String aarPackageName, String drawableName) {
        if (drawableName == null) {
            return 0;
        }
        String str = aarPackageName + ":drawable/" + drawableName;
        ConcurrentHashMap<String, Integer> concurrentHashMap = RESOURCE_CACHE;
        if (concurrentHashMap.containsKey(str)) {
            return concurrentHashMap.get(str).intValue();
        }
        Log.e("ResourceUtil", "Error get drawable resource: " + str);
        return 0;
    }

    public static void putDrawableRes(Context context, String aarPackageName, String drawableName) {
        if (context == null || aarPackageName == null || drawableName == null) {
            return;
        }
        String str = aarPackageName + ":drawable/" + drawableName;
        try {
            int identifier = context.getResources().getIdentifier(drawableName, "drawable", aarPackageName);
            if (identifier != 0) {
                RESOURCE_CACHE.put(str, Integer.valueOf(identifier));
            }
        } catch (Exception e) {
            Log.e("ResourceUtil", "Error put drawable resource: " + str, e);
        }
    }
}

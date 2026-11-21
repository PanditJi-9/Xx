package dev.redlab.gamespace.gamecenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes.dex */
public class BannerRequest {
    public static final String GAME_COVER_REQUEST_URL = "/gamecover/get_by_packagename";
    public static final String URL = "https://arkbase-api.nubia.com";

    public static String getSoftListByPackageNames() {
        return "https://arkbase-api.nubia.com/gamecover/get_by_packagename";
    }

    public static Map<String, String> createParams(ArrayList<String> arrayList) {
        HashMap hashMap = new HashMap();
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = arrayList.iterator();
        while (it.hasNext()) {
            String next = it.next();
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(next);
        }
        hashMap.put("package_names", sb.toString());
        ApiSignUtil.genSignForArkbase(hashMap);
        return hashMap;
    }
}

package cn.nubia.common.wallpaper;

import java.util.ArrayList;

/* loaded from: classes.dex */
public class WallpaperList {
    public static ArrayList<String> LOCAL_WALLPAPER_LIST = null;
    public static final String RES_URL_PREFIX = "android.resource://dev.redlab.gamespace/mipmap/";

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        LOCAL_WALLPAPER_LIST = arrayList;
        arrayList.add("android.resource://dev.redlab.gamespace/mipmap/wallpaper_0");
        LOCAL_WALLPAPER_LIST.add("android.resource://dev.redlab.gamespace/mipmap/wallpaper_1");
        LOCAL_WALLPAPER_LIST.add("android.resource://dev.redlab.gamespace/mipmap/wallpaper_2");
        LOCAL_WALLPAPER_LIST.add("android.resource://dev.redlab.gamespace/mipmap/wallpaper_3");
    }
}

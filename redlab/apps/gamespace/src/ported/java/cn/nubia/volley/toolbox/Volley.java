package cn.nubia.volley.toolbox;

import android.content.Context;
import cn.nubia.volley.Network;
import cn.nubia.volley.RequestQueue;
import java.io.File;

/* loaded from: classes2.dex */
public class Volley {
    private static final String DEFAULT_CACHE_DIR = "volley";

    public static RequestQueue newRequestQueue(Context context, BaseHttpStack baseHttpStack) {
        BasicNetwork basicNetwork;
        if (baseHttpStack == null) {
            basicNetwork = new BasicNetwork((BaseHttpStack) new HurlStack());
        } else {
            basicNetwork = new BasicNetwork(baseHttpStack);
        }
        return newRequestQueue(context, basicNetwork);
    }

    @Deprecated
    public static RequestQueue newRequestQueue(Context context, HttpStack httpStack) {
        if (httpStack == null) {
            return newRequestQueue(context, (BaseHttpStack) null);
        }
        return newRequestQueue(context, new BasicNetwork(httpStack));
    }

    private static RequestQueue newRequestQueue(Context context, Network network) {
        RequestQueue requestQueue = new RequestQueue(new DiskBasedCache(new File(context.getCacheDir(), DEFAULT_CACHE_DIR)), network);
        requestQueue.start();
        return requestQueue;
    }

    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, (BaseHttpStack) null);
    }
}

package cn.nubia.resourcelibrary.util;

import android.content.Context;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public class SmallRedPointerHelper {
    private static final String TAG = "SmallRedPointerHelper";
    private static Context mContext;
    private static SmallRedPointerHelper mInstance;

    public static SmallRedPointerHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SmallRedPointerHelper();
        }
        if (mContext == null) {
            mContext = context.getApplicationContext();
        }
        return mInstance;
    }

    public synchronized ArrayList<String> executeRequest() {
        return null;
    }
}

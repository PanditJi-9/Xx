package cn.nubia.gamecenter.settings.utils;

import android.text.TextUtils;
import android.view.View;
import com.zte.gameassist.ai.AIFlickerTips;

/* loaded from: classes.dex */
public class FlickerUtils {
    private static String FLICKER_NAME = null;
    private static final String TAG = "FlickerUtils";
    public static final String VIEW_ID = "view_id";

    public static void showFlicker(String str) {
        LogUtil.i(TAG, "showFlicker " + str);
        FLICKER_NAME = str;
        if (TextUtils.isEmpty(str)) {
            return;
        }
        AIFlickerTips.showFlicker(str);
    }

    public static void setFlickerName(View view, String str) {
        if (!TextUtils.isEmpty(FLICKER_NAME) && FLICKER_NAME.equals(str)) {
            LogUtil.i(TAG, "setFlickerName " + str);
            AIFlickerTips.setFlickerName(view, str);
        }
    }

    public static void release() {
        LogUtil.i(TAG, "release");
        AIFlickerTips.hideAllFlicker();
    }
}

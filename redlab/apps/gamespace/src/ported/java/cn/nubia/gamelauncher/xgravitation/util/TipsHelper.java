package dev.redlab.gamespace.xgravitation.util;

import android.content.Context;
import android.content.res.Resources;
import dev.redlab.gamespace.R;
import java.util.HashMap;

/* loaded from: classes2.dex */
public class TipsHelper {
    private static final int MAX_INDEX = 19;
    private static final String TAG = "TipsHelper";
    private static TipsHelper mInstance;
    public static HashMap<Integer, Integer> mVoiceMap;

    public void releaseVoiceMap() {
    }

    static {
        HashMap<Integer, Integer> hashMap = new HashMap<>();
        mVoiceMap = hashMap;
        hashMap.put(0, Integer.valueOf(R.raw.x_gravitation_sound_0));
        mVoiceMap.put(1, Integer.valueOf(R.raw.x_gravitation_sound_1));
        mVoiceMap.put(2, Integer.valueOf(R.raw.x_gravitation_sound_2));
        mVoiceMap.put(3, Integer.valueOf(R.raw.x_gravitation_sound_3));
        mVoiceMap.put(4, Integer.valueOf(R.raw.x_gravitation_sound_4));
        mVoiceMap.put(5, Integer.valueOf(R.raw.x_gravitation_sound_5));
        mVoiceMap.put(6, Integer.valueOf(R.raw.x_gravitation_sound_6));
        mVoiceMap.put(7, Integer.valueOf(R.raw.x_gravitation_sound_7));
        mVoiceMap.put(8, Integer.valueOf(R.raw.x_gravitation_sound_8));
        mVoiceMap.put(9, Integer.valueOf(R.raw.x_gravitation_sound_9));
        mVoiceMap.put(10, Integer.valueOf(R.raw.x_gravitation_sound_10));
        mVoiceMap.put(11, Integer.valueOf(R.raw.x_gravitation_sound_11));
        mVoiceMap.put(12, Integer.valueOf(R.raw.x_gravitation_sound_12));
        mVoiceMap.put(13, Integer.valueOf(R.raw.x_gravitation_sound_13));
        mVoiceMap.put(14, Integer.valueOf(R.raw.x_gravitation_sound_14));
        mVoiceMap.put(15, Integer.valueOf(R.raw.x_gravitation_sound_15));
        mVoiceMap.put(16, Integer.valueOf(R.raw.x_gravitation_sound_16));
        mVoiceMap.put(17, Integer.valueOf(R.raw.x_gravitation_sound_17));
        mVoiceMap.put(18, Integer.valueOf(R.raw.x_gravitation_sound_18));
        mVoiceMap.put(19, Integer.valueOf(R.raw.x_gravitation_sound_19));
    }

    public static TipsHelper getInstance() {
        LogUtils.d(TAG, " getInstance mInstance == " + mInstance);
        if (mInstance == null) {
            mInstance = new TipsHelper();
        }
        return mInstance;
    }

    public String getTipsTitle(Context context, int i) {
        Resources resources = context.getResources();
        if (i < 0 || i > 19) {
            LogUtils.d(TAG, "getTipsTitle: index error : " + i);
            return null;
        }
        switch (i) {
            case 0:
                return resources.getString(R.string.x_gravitation_tips_title_x_gravitation);
            case 1:
            case 2:
            case 3:
            case 4:
                return resources.getString(R.string.x_gravitation_tips_title_gravitation_instruction);
            case 5:
            case 6:
            case 7:
            case 8:
                return resources.getString(R.string.x_gravitation_tips_title_cloud_game);
            case 9:
                return resources.getString(R.string.x_gravitation_tips_title_host_game);
            case 10:
            case 11:
                return resources.getString(R.string.x_gravitation_tips_title_mouse_play);
            case 12:
            case 13:
            case 14:
            case 15:
                return resources.getString(R.string.x_gravitation_tips_title_handle_play);
            case 16:
            case 17:
            case 18:
            case 19:
                return resources.getString(R.string.x_gravitation_tips_title_touping_play);
            default:
                return null;
        }
    }

    public String getTipsContent(Context context, int i) {
        if (i < 0 || i > 19) {
            LogUtils.d(TAG, "getTipsContent: index error : " + i);
            return null;
        }
        String[] stringArray = context.getResources().getStringArray(R.array.x_gravitation_tips_content_array);
        if (stringArray == null || i >= stringArray.length) {
            return null;
        }
        return stringArray[i];
    }

    public int getTipsVoiceRedId(Context context, int i) {
        int i2 = -1;
        if (i < 0 || i > 19) {
            LogUtils.d(TAG, "getTipsVoiceRedId: index error : " + i);
            return -1;
        }
        LogUtils.d(TAG, "getTipsVoiceRedId ---- mVoiceMap.size = " + mVoiceMap.size());
        HashMap<Integer, Integer> hashMap = mVoiceMap;
        if (hashMap != null && i < hashMap.size()) {
            i2 = mVoiceMap.get(Integer.valueOf(i)).intValue();
        }
        LogUtils.d(TAG, " 1111 getTipsVoiceRedId: " + i2);
        LogUtils.d(TAG, " 2222 getTipsVoiceRedId: " + i2);
        return i2;
    }
}

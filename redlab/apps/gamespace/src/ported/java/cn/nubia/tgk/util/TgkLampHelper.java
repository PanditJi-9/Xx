package cn.nubia.tgk.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.util.ColorfulLightShoulderParser;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes2.dex */
public class TgkLampHelper {
    private static final String TAG = "TgkLampHelper";
    public static int mLeftDownId;
    public static int mLeftUpId;
    public static int mRightDownId;
    public static int mRightUpId;
    public static String mSelection;
    public static volatile ColorfulLightShoulderParser.ShoulderSettings mShoulderSettings;
    public static ArrayList<String> mLampCaseNames = new ArrayList<>();
    public static ArrayList<String> mLampCaseCodes = new ArrayList<>();

    public static void getShoulderSettings(Context context) {
        if (mShoulderSettings == null) {
            mShoulderSettings = ColorfulLightShoulderParser.getShoulderSettings(context);
            Log.d(TAG, "mShoulderSettings= " + mShoulderSettings);
            if (mShoulderSettings != null) {
                String[] stringArray = context.getResources().getStringArray(R.array.lamp_case_names_789);
                if (mShoulderSettings.shoulder_configs != null && !mShoulderSettings.shoulder_configs.isEmpty()) {
                    for (String str : mShoulderSettings.shoulder_configs.keySet()) {
                        if (!TextUtils.isEmpty(getCaseName(stringArray, str))) {
                            mLampCaseCodes.add(mShoulderSettings.shoulder_configs.get(str));
                        }
                    }
                }
                if (mShoulderSettings.l_down != null) {
                    mLeftDownId = mShoulderSettings.l_down.id;
                }
                if (mShoulderSettings.l_up != null) {
                    mLeftUpId = mShoulderSettings.l_up.id;
                }
                if (mShoulderSettings.r_down != null) {
                    mRightDownId = mShoulderSettings.r_down.id;
                }
                if (mShoulderSettings.r_up != null) {
                    mRightUpId = mShoulderSettings.r_up.id;
                }
                mSelection = mShoulderSettings.selection;
            }
            Log.d(TAG, "mLeftDownId= " + mLeftDownId + ";mLeftUpId=" + mLeftUpId + ";mRightDownId= " + mRightDownId + ";mRightUpId=" + mRightUpId + ";mSelection= " + mSelection + ";mRightUpId=" + mRightUpId + ";mLampCaseNames size = " + mLampCaseNames.size() + ";mLampCaseCodes size =" + mLampCaseCodes.size());
        }
    }

    public static void getLampCaseNames(Context context) {
        if (mShoulderSettings != null) {
            mLampCaseNames.clear();
            String[] stringArray = context.getResources().getStringArray(R.array.lamp_case_names_789);
            if (mShoulderSettings.shoulder_configs == null || mShoulderSettings.shoulder_configs.isEmpty()) {
                return;
            }
            Iterator<String> it = mShoulderSettings.shoulder_configs.keySet().iterator();
            while (it.hasNext()) {
                String caseName = getCaseName(stringArray, it.next());
                if (!TextUtils.isEmpty(caseName)) {
                    mLampCaseNames.add(caseName);
                }
            }
        }
    }

    public static String getSelection() {
        return mSelection;
    }

    public static int getLeftDownId() {
        return mLeftDownId;
    }

    public static int getLeftUpId() {
        return mLeftUpId;
    }

    public static int getRightDownId() {
        return mRightDownId;
    }

    public static int getRightUpId() {
        return mRightUpId;
    }

    public static ArrayList<String> getLampCaseNames() {
        return mLampCaseNames;
    }

    public static ArrayList<String> getLampCaseCodes() {
        return mLampCaseCodes;
    }

    public static String getLampCaseNameForIndex(int i) {
        try {
            return mLampCaseNames.get(i);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getLampCaseCodesForIndex(int i) {
        try {
            return mLampCaseCodes.get(i);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static int getLampCaseNamesSize() {
        try {
            return mLampCaseNames.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int getLampCaseCodesSize() {
        try {
            return mLampCaseCodes.size();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String getCaseName(String[] strArr, String str) {
        try {
            if ("close".equals(str)) {
                str = strArr[0];
            } else if ("steady".equals(str)) {
                str = strArr[1];
            } else if ("pulse".equals(str)) {
                str = strArr[2];
            } else if ("rhythm".equals(str)) {
                str = strArr[3];
            } else if ("flashing".equals(str)) {
                str = strArr[4];
            }
            Log.d(TAG, "name= " + str);
            return str;
        } catch (Exception e) {
            Log.d(TAG, "getCaseName e= " + e.toString());
            e.printStackTrace();
            return "";
        }
    }
}

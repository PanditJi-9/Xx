package dev.redlab.gamespace.xgravitation.util;

import java.util.Random;

/* loaded from: classes2.dex */
public class RandomUntil {
    private static final int MAX_NUM = 19;
    private static final String TAG = "RandomUntil";
    private static int mLastRandom = -1;

    public static int getNum() {
        return new Random().nextInt(19);
    }

    public static int getDifferentNum() {
        int num = getNum();
        if (mLastRandom == -1) {
            mLastRandom = num;
        }
        LogUtils.d(TAG, "getDifferentNum: randomNum = " + num + " ;; mLastRandom = " + mLastRandom);
        while (mLastRandom == num) {
            num = getNum();
        }
        mLastRandom = num;
        return num;
    }
}

package dev.redlab.gamespace.gamecontrolpanel;

import dev.redlab.gamespace.gamecontrolpanel.utils.TouchOperationHelper;

/* loaded from: classes.dex */
public class TouchOperationBean {
    private static final String DB_GAME_PERFORMANCE_GYRO_SEN_LIST = "NubiaperformanceGyroSen";
    private static final String DB_GAME_PERFORMANCE_IS_TOUCH_PROTECT_OPEN = "PerformanceISTouchProtectOpen";
    private static final String DB_GAME_PERFORMANCE_TOUCH_FOLLOW = "NubiaperformanceTouchFollow";
    private static final String DB_GAME_PERFORMANCE_TOUCH_MICRO_SENSITIVE = "NubiaperformanceTouchMicroSensitive";
    private static final String DB_GAME_PERFORMANCE_TOUCH_PROTECTION_LEVEL = "PerformanceTouchProtectLev";
    private static final String DB_GAME_PERFORMANCE_TOUCH_SAMPLE_RATE = "NubiaperformanceTouchSampleRate";
    private static final String DB_GAME_PERFORMANCE_TOUCH_SEN = "NubiaperformanceTouchSen";
    private static final String TAG = "TouchOperationBean";
    private String mCurrentPkgName;
    private boolean mIsSupportGyro;
    private boolean mIsSupportTouchProtection;
    private boolean mIsSupportTouchSample = true;
    private OperationTypeParams mOperationData;

    public TouchOperationBean(boolean z, boolean z2) {
        this.mIsSupportGyro = z;
        this.mIsSupportTouchProtection = z2;
    }

    public boolean isSupportGyro() {
        return this.mIsSupportGyro;
    }

    public boolean isSupportTouchProtection() {
        return this.mIsSupportTouchProtection;
    }

    public boolean isSupportTouchSample() {
        return this.mIsSupportTouchSample;
    }

    public enum OperationTypeParams {
        TOUCH_SAMPLE(TouchOperationBean.DB_GAME_PERFORMANCE_TOUCH_SAMPLE_RATE, TouchOperationHelper.getTouchSampleRateDefaultValue()),
        TOUCH_SEN(TouchOperationBean.DB_GAME_PERFORMANCE_TOUCH_SEN, TouchOperationHelper.getTouchSenDefaultValue()),
        TOUCH_FOLLOW(TouchOperationBean.DB_GAME_PERFORMANCE_TOUCH_FOLLOW, TouchOperationHelper.getTouchFollowDefaultValue()),
        TOUCH_MICRO_SENSITIVE(TouchOperationBean.DB_GAME_PERFORMANCE_TOUCH_MICRO_SENSITIVE, TouchOperationHelper.getTouchMicroSensitiveDefaultValue()),
        GYROSEN(TouchOperationBean.DB_GAME_PERFORMANCE_GYRO_SEN_LIST, TouchOperationHelper.getGyroSenDefaultValue()),
        TOUCH_PROTECTION(TouchOperationBean.DB_GAME_PERFORMANCE_TOUCH_PROTECTION_LEVEL, TouchOperationHelper.getTouchProtectOpenDefaultValue());

        private String mCurrentPkgName;
        private String mDBFieldName;
        private int[] mDefaultValue;
        private int[] mValue;

        OperationTypeParams(String str, int[] iArr) {
            this.mDefaultValue = iArr;
            this.mValue = iArr;
            this.mDBFieldName = str;
        }

        public void setValue(int[] iArr) {
            this.mValue = iArr;
        }

        public int[] getValue() {
            return this.mValue;
        }

        public String getDBFieldName() {
            return this.mDBFieldName;
        }

        public void reset() {
            this.mValue = this.mDefaultValue;
        }
    }
}

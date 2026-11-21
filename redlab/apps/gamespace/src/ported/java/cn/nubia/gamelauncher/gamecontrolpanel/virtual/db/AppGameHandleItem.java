package dev.redlab.gamespace.gamecontrolpanel.virtual.db;

import android.graphics.Rect;
import android.text.TextUtils;

/* loaded from: classes.dex */
public class AppGameHandleItem {
    public static final int COUNT = 15;
    public static final int TARGET_DOWN_ARROW_KEY = 7;
    private static final String[] TARGET_INFO_KEYS = {DBConstant.LEFT_JOYSTICK, DBConstant.RIGHT_JOYSTICK, DBConstant.LEFT_ENTITY_KEY, DBConstant.RIGHT_ENTITY_KEY, DBConstant.LEFT_ARROW_KEY, DBConstant.RIGHT_ARROW_KEY, DBConstant.UP_ARROW_KEY, DBConstant.DOWN_ARROW_KEY, DBConstant.LETTER_A_KEY, DBConstant.LETTER_A1_KEY, DBConstant.LETTER_A2_KEY, DBConstant.LETTER_B_KEY, DBConstant.LETTER_X_KEY, DBConstant.LETTER_Y_KEY, DBConstant.LETTER_Z_KEY};
    public static final int TARGET_LEFT_ARROW_KEY = 4;
    public static final int TARGET_LEFT_ENTITY_KEY = 2;
    public static final int TARGET_LEFT_JOYSTICK = 0;
    public static final int TARGET_LETTER_A1_KEY = 9;
    public static final int TARGET_LETTER_A2_KEY = 10;
    public static final int TARGET_LETTER_A_KEY = 8;
    public static final int TARGET_LETTER_B_KEY = 11;
    public static final int TARGET_LETTER_X_KEY = 12;
    public static final int TARGET_LETTER_Y_KEY = 13;
    public static final int TARGET_LETTER_Z_KEY = 14;
    public static final int TARGET_RIGHT_ARROW_KEY = 5;
    public static final int TARGET_RIGHT_ENTITY_KEY = 3;
    public static final int TARGET_RIGHT_JOYSTICK = 1;
    public static final int TARGET_UP_ARROW_KEY = 6;
    private int mCurrentConfig;
    private int mCutSize;
    private int mDefaultConfig;
    private String mId;
    private String mImageUrl;
    private String mPackageName;
    private String mRightGameHandleStyle;
    private TargetInfo[] mTargetInfos;
    private String mTitle;
    private String mType;

    public static class TargetInfo {
        private boolean mIsEnable;
        private Rect mTargetRect;

        public TargetInfo() {
            this.mTargetRect = new Rect();
        }

        public TargetInfo(boolean z, Rect rect) {
            this.mTargetRect = new Rect();
            this.mIsEnable = z;
            this.mTargetRect = new Rect(rect);
        }

        public TargetInfo(TargetInfo targetInfo) {
            this.mTargetRect = new Rect();
            if (targetInfo == null) {
                this.mIsEnable = false;
            } else {
                this.mIsEnable = targetInfo.isEnable();
                this.mTargetRect = new Rect(targetInfo.getTargetRect());
            }
        }

        public boolean isEnable() {
            return this.mIsEnable;
        }

        public Rect getTargetRect() {
            return this.mTargetRect;
        }

        public void setEnable(boolean z) {
            this.mIsEnable = z;
        }

        public void setTargetRect(Rect rect) {
            this.mTargetRect.set(rect);
        }

        public boolean equals(Object obj) {
            if (obj != null && (obj instanceof TargetInfo)) {
                TargetInfo targetInfo = (TargetInfo) obj;
                return this.mIsEnable == targetInfo.isEnable() && this.mTargetRect == targetInfo.getTargetRect();
            }
            return super.equals(obj);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.mIsEnable ? "true" : "false");
            sb.append("|");
            sb.append(this.mTargetRect.left);
            sb.append("|");
            sb.append(this.mTargetRect.top);
            sb.append("|");
            sb.append(this.mTargetRect.right);
            sb.append("|");
            sb.append(this.mTargetRect.bottom);
            return sb.toString();
        }
    }

    public AppGameHandleItem() {
        this.mTargetInfos = new TargetInfo[15];
    }

    public AppGameHandleItem(AppGameHandleItem appGameHandleItem) {
        this.mTargetInfos = new TargetInfo[15];
        this.mId = appGameHandleItem.getId();
        this.mTitle = appGameHandleItem.getTitle();
        this.mPackageName = appGameHandleItem.getPackageName();
        this.mType = appGameHandleItem.getType();
        this.mCutSize = appGameHandleItem.getCutSize();
        this.mRightGameHandleStyle = appGameHandleItem.getRightGameHandleStyle();
        this.mImageUrl = appGameHandleItem.getImageUrl();
        this.mDefaultConfig = appGameHandleItem.getDefaultConfig();
        this.mCurrentConfig = appGameHandleItem.getCurrentConfig();
        for (int i = 0; i < 15; i++) {
            TargetInfo targetInfo = appGameHandleItem.getTargetInfo(i);
            setTargetInfo(i, targetInfo.isEnable(), targetInfo.getTargetRect());
        }
    }

    public void setId(String str) {
        this.mId = str;
    }

    public void setTitle(String str) {
        this.mTitle = str;
    }

    public void setPackageName(String str) {
        this.mPackageName = str;
    }

    public void setType(String str) {
        this.mType = str;
    }

    public void setCutSize(int i) {
        this.mCutSize = i;
    }

    public void setRightGameHandleStyle(String str) {
        this.mRightGameHandleStyle = str;
    }

    public void setImageUrl(String str) {
        this.mImageUrl = str;
    }

    public void setDefaultConfig(int i) {
        this.mDefaultConfig = i;
    }

    public void setCurrentConfig(int i) {
        this.mCurrentConfig = i;
    }

    public boolean setTargetInfo(int i, boolean z, Rect rect) {
        if (i < 0 || i >= 15) {
            return false;
        }
        TargetInfo[] targetInfoArr = this.mTargetInfos;
        TargetInfo targetInfo = targetInfoArr[i];
        if (targetInfo != null) {
            targetInfo.mIsEnable = z;
            this.mTargetInfos[i].mTargetRect.set(rect);
            return true;
        }
        targetInfoArr[i] = new TargetInfo(z, rect);
        return true;
    }

    public void setTargetInfo(int i, TargetInfo targetInfo) {
        if (i < 0 || i >= 15) {
            return;
        }
        this.mTargetInfos[i] = targetInfo;
    }

    public String getId() {
        return this.mId;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getType() {
        return this.mType;
    }

    public int getCutSize() {
        return this.mCutSize;
    }

    public String getRightGameHandleStyle() {
        return this.mRightGameHandleStyle;
    }

    public String getImageUrl() {
        return this.mImageUrl;
    }

    public int getDefaultConfig() {
        return this.mDefaultConfig;
    }

    public int getCurrentConfig() {
        return this.mCurrentConfig;
    }

    public TargetInfo getTargetInfo(int i) {
        if (i < 0 || i >= 15) {
            return new TargetInfo();
        }
        TargetInfo targetInfo = this.mTargetInfos[i];
        return targetInfo != null ? targetInfo : new TargetInfo();
    }

    public boolean equals(Object obj) {
        if (obj != null && (obj instanceof AppGameHandleItem)) {
            AppGameHandleItem appGameHandleItem = (AppGameHandleItem) obj;
            return TextUtils.equals(getTitle(), appGameHandleItem.getTitle()) && TextUtils.equals(getPackageName(), appGameHandleItem.getPackageName()) && TextUtils.equals(getType(), appGameHandleItem.getType()) && isSameData(appGameHandleItem) && TextUtils.equals(getImageUrl(), appGameHandleItem.getImageUrl()) && getDefaultConfig() == appGameHandleItem.getDefaultConfig();
        }
        return super.equals(obj);
    }

    public boolean isSameData(AppGameHandleItem appGameHandleItem) {
        if (appGameHandleItem == null || getCutSize() != appGameHandleItem.getCutSize() || !TextUtils.equals(getRightGameHandleStyle(), appGameHandleItem.getRightGameHandleStyle())) {
            return false;
        }
        for (int i = 0; i < 15; i++) {
            if (getTargetInfo(i) != appGameHandleItem.getTargetInfo(i)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return getDescription(true);
    }

    public String toShortString() {
        return getDescription(false);
    }

    private String getDescription(boolean z) {
        StringBuilder sb = new StringBuilder("AppGameHandleItem{_id:");
        sb.append(getId());
        sb.append(",title:");
        sb.append(getTitle());
        sb.append(",type:");
        sb.append(getType());
        sb.append(",package_name:");
        sb.append(getPackageName());
        sb.append(",cut_size:");
        sb.append(getCutSize());
        sb.append(",right_game_handle_style:");
        sb.append(getRightGameHandleStyle());
        for (int i = 0; i < 15; i++) {
            TargetInfo targetInfo = getTargetInfo(i);
            if (z || targetInfo.isEnable()) {
                sb.append(",");
                sb.append(TARGET_INFO_KEYS[i]);
                sb.append(":");
                sb.append(targetInfo);
            }
        }
        sb.append("}");
        return sb.toString();
    }
}

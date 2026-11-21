package dev.redlab.gamespace.gamecontrolpanel.performancetuning;

import java.util.List;

/* loaded from: classes.dex */
public class CustomPerfProfile {
    private String mDisplayName;
    private int mSerial;
    private List<SettingGroup> mSettingGroupList;

    public int getSerial() {
        return this.mSerial;
    }

    public void setSerial(int i) {
        this.mSerial = i;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public void setDisplayName(String str) {
        this.mDisplayName = str;
    }

    public List<SettingGroup> getSettingGroupList() {
        return this.mSettingGroupList;
    }

    public void setSettingGroupList(List<SettingGroup> list) {
        this.mSettingGroupList = list;
    }

    public String toString() {
        return "CustomPerfProfile{mSerial=" + this.mSerial + ", mDisplayName='" + this.mDisplayName + "', mSettingGroupList=" + this.mSettingGroupList + '}';
    }

    public static class SettingGroup {
        private String mKey;
        private String mName;
        private List<SettingItem> mSettingList;

        public String getKey() {
            return this.mKey;
        }

        public void setKey(String str) {
            this.mKey = str;
        }

        public String getName() {
            return this.mName;
        }

        public void setName(String str) {
            this.mName = str;
        }

        public List<SettingItem> getSettingList() {
            return this.mSettingList;
        }

        public void setSettingList(List<SettingItem> list) {
            this.mSettingList = list;
        }

        public String toString() {
            return "SettingGroup{mKey='" + this.mKey + "', mName='" + this.mName + "', mSettingList=" + this.mSettingList + '}';
        }
    }

    public static class SettingItem {
        private boolean mIsScoped;
        private String mItemPath;
        private String mKey;
        private String mName;
        private String mValue;
        private String mValue2;
        private List<String> mValueList;

        public String getKey() {
            return this.mKey;
        }

        public void setKey(String str) {
            this.mKey = str;
        }

        public String getName() {
            return this.mName;
        }

        public void setName(String str) {
            this.mName = str;
        }

        public String getValue() {
            return this.mValue;
        }

        public void setValue(String str) {
            this.mValue = str;
        }

        public String getValue2() {
            return this.mValue2;
        }

        public void setValue2(String str) {
            this.mValue2 = str;
        }

        public boolean isScoped() {
            return this.mIsScoped;
        }

        public void setScoped(boolean z) {
            this.mIsScoped = z;
        }

        public List<String> getValueList() {
            return this.mValueList;
        }

        public void setValueList(List<String> list) {
            this.mValueList = list;
        }

        public String getItemPath() {
            return this.mItemPath;
        }

        public void setItemPath(String str) {
            this.mItemPath = str;
        }

        public String toString() {
            return "SettingItem{mKey='" + this.mKey + "', mName='" + this.mName + "', mValue='" + this.mValue + "', mValue2='" + this.mValue2 + "', mItemPath='" + this.mItemPath + "', mIsScoped=" + this.mIsScoped + ", mValueList=" + this.mValueList + '}';
        }
    }
}

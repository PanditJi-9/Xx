package dev.redlab.gamespace.gamecontrolpanel.performancetuning.adapter;

import java.util.List;

/* loaded from: classes.dex */
public class AdapterItem {
    public String mGroupKey;
    public boolean mGroupLastItem;
    public boolean mIsScoped;
    public String mKey;
    public String mName;
    public String mNormalValue;
    public String mNormalValue2;
    public String mPath;
    public int mSettingType;
    public String mValue;
    public String mValue2;
    public List<String> mValueList;

    public String toString() {
        return "AdapterItem{mGroupKey='" + this.mGroupKey + "', mKey='" + this.mKey + "', mPath='" + this.mPath + "', mName='" + this.mName + "', mNormalValue='" + this.mNormalValue + "', mNormalValue2='" + this.mNormalValue2 + "', mValue='" + this.mValue + "', mValue2='" + this.mValue2 + "', mIsScoped=" + this.mIsScoped + ", mValueList=" + this.mValueList + ", mSettingType=" + this.mSettingType + ", mGroupLastItem=" + this.mGroupLastItem + '}';
    }
}

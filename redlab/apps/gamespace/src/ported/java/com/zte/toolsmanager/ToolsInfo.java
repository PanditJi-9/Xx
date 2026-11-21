package com.zte.toolsmanager;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes2.dex */
public class ToolsInfo implements Parcelable {
    public static final Parcelable.Creator<ToolsInfo> CREATOR = new Parcelable.Creator<ToolsInfo>() { // from class: com.zte.toolsmanager.ToolsInfo.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ToolsInfo createFromParcel(Parcel in) {
            return new ToolsInfo(in);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public ToolsInfo[] newArray(int size) {
            return new ToolsInfo[size];
        }
    };
    public String appName;
    public String packageName;
    public String toolsName;

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public ToolsInfo(String toolsName, String packageName, String appName) {
        this.toolsName = toolsName;
        this.packageName = packageName;
        this.appName = appName;
    }

    protected ToolsInfo(Parcel in) {
        this.toolsName = in.readString();
        this.packageName = in.readString();
        this.appName = in.readString();
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.toolsName);
        dest.writeString(this.packageName);
        dest.writeString(this.appName);
    }
}

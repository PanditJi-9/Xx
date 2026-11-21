package cn.nubia.upgrade.model;

import android.os.Parcel;
import android.os.Parcelable;

/* loaded from: classes2.dex */
public class VersionData implements Parcelable {
    public static final Parcelable.Creator<VersionData> CREATOR = new Parcelable.Creator<VersionData>() { // from class: cn.nubia.upgrade.model.VersionData.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VersionData createFromParcel(Parcel parcel) {
            return new VersionData(parcel);
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public VersionData[] newArray(int i) {
            return new VersionData[i];
        }
    };

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public long getFileSize() {
        return 0L;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
    }

    public String getUpgradeContent() {
        return "  ";
    }

    public String getToVersionCode() {
        return "  ";
    }

    public String getApkUrl() {
        return "  ";
    }

    public VersionData(Parcel parcel) {
    }
}

package cn.nubia.gamecenter.settings.applearning;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import cn.nubia.gamecenter.settings.applearning.AppDbSchema;

/* loaded from: classes.dex */
public class PackageLockRecord implements Parcelable {
    public static final Parcelable.Creator<PackageLockRecord> CREATOR = new Parcelable.Creator<PackageLockRecord>() { // from class: cn.nubia.gamecenter.settings.applearning.PackageLockRecord.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PackageLockRecord createFromParcel(Parcel parcel) {
            PackageLockRecord packageLockRecord = new PackageLockRecord();
            packageLockRecord.packagename = parcel.readString();
            packageLockRecord.time = parcel.readLong();
            packageLockRecord.delayTime = parcel.readLong();
            packageLockRecord.enable = parcel.readInt();
            return packageLockRecord;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public PackageLockRecord[] newArray(int i) {
            return new PackageLockRecord[i];
        }
    };
    public long delayTime;
    public int enable;
    public String packagename;
    public long time;

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public PackageLockRecord() {
    }

    public String toString() {
        return "PackageLockRecord [packagename=" + this.packagename + ", time=" + this.time + ", delayTime=" + this.delayTime + ", enable=" + this.enable + "]";
    }

    public PackageLockRecord(Cursor cursor) {
        this.packagename = cursor.getString(0);
        this.time = cursor.getLong(1);
        this.delayTime = cursor.getLong(2);
        this.enable = cursor.getInt(3);
    }

    public PackageLockRecord(PackageLockRecord packageLockRecord) {
        this.packagename = packageLockRecord.packagename;
        this.time = packageLockRecord.time;
        this.delayTime = packageLockRecord.delayTime;
        this.enable = packageLockRecord.enable;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.packagename);
        parcel.writeLong(this.time);
        parcel.writeLong(this.delayTime);
        parcel.writeInt(this.enable);
    }

    public static ContentValues getContentValues(PackageLockRecord packageLockRecord) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("packagename", packageLockRecord.packagename);
        contentValues.put(AppDbSchema.OneKeyLockedAppsTable.Cols.LIMIT_TIME_STAMP, Long.valueOf(packageLockRecord.time));
        contentValues.put(AppDbSchema.OneKeyLockedAppsTable.Cols.DELAY_TIME, Long.valueOf(packageLockRecord.delayTime));
        contentValues.put("enable", Integer.valueOf(packageLockRecord.enable));
        return contentValues;
    }
}

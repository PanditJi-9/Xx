package cn.nubia.permissioncapsule;

import android.app.usage.StorageStats;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.UserHandle;
import java.util.List;
import java.util.Map;

/* loaded from: classes2.dex */
public interface INbUsageStatsManager extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.permissioncapsule.INbUsageStatsManager";

    public static class Default implements INbUsageStatsManager {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
        public UsageEvents queryEvents(long j, long j2) throws RemoteException {
            return null;
        }

        @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
        public Map queryNbStatsCount(List<String> list, long j, long j2, long j3) throws RemoteException {
            return null;
        }

        @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
        public List queryNbStatsDistribution(List<String> list, long j, long j2, long j3) throws RemoteException {
            return null;
        }

        @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
        public Map queryNbStatsTime(List<String> list, long j, long j2) throws RemoteException {
            return null;
        }

        @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
        public Map queryNbStatsTotalTime(List<String> list) throws RemoteException {
            return null;
        }

        @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
        public StorageStats queryStatsForPackage(String str, String str2, UserHandle userHandle) throws RemoteException {
            return null;
        }

        @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
        public List<UsageStats> queryUsageStats(int i, long j, long j2) throws RemoteException {
            return null;
        }
    }

    UsageEvents queryEvents(long j, long j2) throws RemoteException;

    Map queryNbStatsCount(List<String> list, long j, long j2, long j3) throws RemoteException;

    List queryNbStatsDistribution(List<String> list, long j, long j2, long j3) throws RemoteException;

    Map queryNbStatsTime(List<String> list, long j, long j2) throws RemoteException;

    Map queryNbStatsTotalTime(List<String> list) throws RemoteException;

    StorageStats queryStatsForPackage(String str, String str2, UserHandle userHandle) throws RemoteException;

    List<UsageStats> queryUsageStats(int i, long j, long j2) throws RemoteException;

    public static abstract class Stub extends Binder implements INbUsageStatsManager {
        static final int TRANSACTION_queryEvents = 1;
        static final int TRANSACTION_queryNbStatsCount = 7;
        static final int TRANSACTION_queryNbStatsDistribution = 6;
        static final int TRANSACTION_queryNbStatsTime = 5;
        static final int TRANSACTION_queryNbStatsTotalTime = 4;
        static final int TRANSACTION_queryStatsForPackage = 3;
        static final int TRANSACTION_queryUsageStats = 2;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, INbUsageStatsManager.DESCRIPTOR);
        }

        public static INbUsageStatsManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(INbUsageStatsManager.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof INbUsageStatsManager)) {
                return (INbUsageStatsManager) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(INbUsageStatsManager.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(INbUsageStatsManager.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    UsageEvents queryEvents = queryEvents(parcel.readLong(), parcel.readLong());
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, queryEvents, 1);
                    return true;
                case 2:
                    List<UsageStats> queryUsageStats = queryUsageStats(parcel.readInt(), parcel.readLong(), parcel.readLong());
                    parcel2.writeNoException();
                    _Parcel.writeTypedList(parcel2, queryUsageStats, 1);
                    return true;
                case 3:
                    StorageStats queryStatsForPackage = queryStatsForPackage(parcel.readString(), parcel.readString(), (UserHandle) _Parcel.readTypedObject(parcel, UserHandle.CREATOR));
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, queryStatsForPackage, 1);
                    return true;
                case 4:
                    Map queryNbStatsTotalTime = queryNbStatsTotalTime(parcel.createStringArrayList());
                    parcel2.writeNoException();
                    parcel2.writeMap(queryNbStatsTotalTime);
                    return true;
                case 5:
                    Map queryNbStatsTime = queryNbStatsTime(parcel.createStringArrayList(), parcel.readLong(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeMap(queryNbStatsTime);
                    return true;
                case 6:
                    List queryNbStatsDistribution = queryNbStatsDistribution(parcel.createStringArrayList(), parcel.readLong(), parcel.readLong(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeList(queryNbStatsDistribution);
                    return true;
                case 7:
                    Map queryNbStatsCount = queryNbStatsCount(parcel.createStringArrayList(), parcel.readLong(), parcel.readLong(), parcel.readLong());
                    parcel2.writeNoException();
                    parcel2.writeMap(queryNbStatsCount);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INbUsageStatsManager {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return INbUsageStatsManager.DESCRIPTOR;
            }

            @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
            public UsageEvents queryEvents(long j, long j2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbUsageStatsManager.DESCRIPTOR);
                    obtain.writeLong(j);
                    obtain.writeLong(j2);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return (UsageEvents) _Parcel.readTypedObject(obtain2, UsageEvents.CREATOR);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
            public List<UsageStats> queryUsageStats(int i, long j, long j2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbUsageStatsManager.DESCRIPTOR);
                    obtain.writeInt(i);
                    obtain.writeLong(j);
                    obtain.writeLong(j2);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.createTypedArrayList(UsageStats.CREATOR);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
            public StorageStats queryStatsForPackage(String str, String str2, UserHandle userHandle) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbUsageStatsManager.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    _Parcel.writeTypedObject(obtain, userHandle, 0);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                    return (StorageStats) _Parcel.readTypedObject(obtain2, StorageStats.CREATOR);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
            public Map queryNbStatsTotalTime(List<String> list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbUsageStatsManager.DESCRIPTOR);
                    obtain.writeStringList(list);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readHashMap(getClass().getClassLoader());
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
            public Map queryNbStatsTime(List<String> list, long j, long j2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbUsageStatsManager.DESCRIPTOR);
                    obtain.writeStringList(list);
                    obtain.writeLong(j);
                    obtain.writeLong(j2);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readHashMap(getClass().getClassLoader());
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
            public List queryNbStatsDistribution(List<String> list, long j, long j2, long j3) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbUsageStatsManager.DESCRIPTOR);
                    obtain.writeStringList(list);
                    obtain.writeLong(j);
                    obtain.writeLong(j2);
                    obtain.writeLong(j3);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readArrayList(getClass().getClassLoader());
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.permissioncapsule.INbUsageStatsManager
            public Map queryNbStatsCount(List<String> list, long j, long j2, long j3) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbUsageStatsManager.DESCRIPTOR);
                    obtain.writeStringList(list);
                    obtain.writeLong(j);
                    obtain.writeLong(j2);
                    obtain.writeLong(j3);
                    this.mRemote.transact(7, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readHashMap(getClass().getClassLoader());
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }

    public static class _Parcel {
        /* JADX INFO: Access modifiers changed from: private */
        public static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> creator) {
            if (parcel.readInt() != 0) {
                return creator.createFromParcel(parcel);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedObject(Parcel parcel, T t, int i) {
            if (t != null) {
                parcel.writeInt(1);
                t.writeToParcel(parcel, i);
            } else {
                parcel.writeInt(0);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedList(Parcel parcel, List<T> list, int i) {
            if (list == null) {
                parcel.writeInt(-1);
                return;
            }
            int size = list.size();
            parcel.writeInt(size);
            for (int i2 = 0; i2 < size; i2++) {
                writeTypedObject(parcel, list.get(i2), i);
            }
        }
    }
}

package cn.nubia.service;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes2.dex */
public interface IProcessManagerService extends IInterface {
    long addWhiteList(String str) throws RemoteException;

    long addWhiteListWithId(String str, int i) throws RemoteException;

    int deleteWhiteList(String str) throws RemoteException;

    int deleteWhiteListWithId(String str, int i) throws RemoteException;

    List<Bundle> getCanBeKilledRunningApps() throws RemoteException;

    int killRunningApps(List<Bundle> list) throws RemoteException;

    int nbForceStopPackage(String str) throws RemoteException;

    int nbForceStopPackageWithId(String str, int i) throws RemoteException;

    int oneKeyCleanExcludeCurrentApp(String str) throws RemoteException;

    int oneKeyCleanExcludeCurrentAppWithId(String str, int i) throws RemoteException;

    int oneKeyCleanIncludeCurrentApp() throws RemoteException;

    List<String> queryWhiteList() throws RemoteException;

    public static abstract class Stub extends Binder implements IProcessManagerService {
        private static final String DESCRIPTOR = "NBProcessManagerService";
        static final int TRANSACTION_addWhiteList = 7;
        static final int TRANSACTION_addWhiteListWithId = 11;
        static final int TRANSACTION_deleteWhiteList = 8;
        static final int TRANSACTION_deleteWhiteListWithId = 12;
        static final int TRANSACTION_getCanBeKilledRunningApps = 2;
        static final int TRANSACTION_killRunningApps = 3;
        static final int TRANSACTION_nbForceStopPackage = 9;
        static final int TRANSACTION_nbForceStopPackageWithId = 13;
        static final int TRANSACTION_oneKeyCleanExcludeCurrentApp = 4;
        static final int TRANSACTION_oneKeyCleanExcludeCurrentAppWithId = 10;
        static final int TRANSACTION_oneKeyCleanIncludeCurrentApp = 5;
        static final int TRANSACTION_queryWhiteList = 6;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IProcessManagerService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IProcessManagerService)) {
                return (IProcessManagerService) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            try {
                if (i == 1598968902) {
                    parcel2.writeString(DESCRIPTOR);
                    return true;
                }
                switch (i) {
                    case 2:
                        parcel.enforceInterface(DESCRIPTOR);
                        List<Bundle> canBeKilledRunningApps = getCanBeKilledRunningApps();
                        parcel2.writeNoException();
                        parcel2.writeTypedList(canBeKilledRunningApps);
                        return true;
                    case 3:
                        parcel.enforceInterface(DESCRIPTOR);
                        int killRunningApps = killRunningApps(parcel.createTypedArrayList(Bundle.CREATOR));
                        parcel2.writeNoException();
                        parcel2.writeInt(killRunningApps);
                        return true;
                    case 4:
                        parcel.enforceInterface(DESCRIPTOR);
                        int oneKeyCleanExcludeCurrentApp = oneKeyCleanExcludeCurrentApp(parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(oneKeyCleanExcludeCurrentApp);
                        return true;
                    case 5:
                        parcel.enforceInterface(DESCRIPTOR);
                        int oneKeyCleanIncludeCurrentApp = oneKeyCleanIncludeCurrentApp();
                        parcel2.writeNoException();
                        parcel2.writeInt(oneKeyCleanIncludeCurrentApp);
                        return true;
                    case 6:
                        parcel.enforceInterface(DESCRIPTOR);
                        List<String> queryWhiteList = queryWhiteList();
                        parcel2.writeNoException();
                        parcel2.writeStringList(queryWhiteList);
                        return true;
                    case 7:
                        parcel.enforceInterface(DESCRIPTOR);
                        long addWhiteList = addWhiteList(parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeLong(addWhiteList);
                        return true;
                    case 8:
                        parcel.enforceInterface(DESCRIPTOR);
                        int deleteWhiteList = deleteWhiteList(parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(deleteWhiteList);
                        return true;
                    case 9:
                        parcel.enforceInterface(DESCRIPTOR);
                        int nbForceStopPackage = nbForceStopPackage(parcel.readString());
                        parcel2.writeNoException();
                        parcel2.writeInt(nbForceStopPackage);
                        return true;
                    case 10:
                        parcel.enforceInterface(DESCRIPTOR);
                        int oneKeyCleanExcludeCurrentAppWithId = oneKeyCleanExcludeCurrentAppWithId(parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(oneKeyCleanExcludeCurrentAppWithId);
                        return true;
                    case 11:
                        parcel.enforceInterface(DESCRIPTOR);
                        long addWhiteListWithId = addWhiteListWithId(parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeLong(addWhiteListWithId);
                        return true;
                    case 12:
                        parcel.enforceInterface(DESCRIPTOR);
                        int deleteWhiteListWithId = deleteWhiteListWithId(parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(deleteWhiteListWithId);
                        return true;
                    case 13:
                        parcel.enforceInterface(DESCRIPTOR);
                        int nbForceStopPackageWithId = nbForceStopPackageWithId(parcel.readString(), parcel.readInt());
                        parcel2.writeNoException();
                        parcel2.writeInt(nbForceStopPackageWithId);
                        return true;
                    default:
                        return super.onTransact(i, parcel, parcel2, i2);
                }
            } catch (Exception e) {
                if (e instanceof SecurityException) {
                    parcel2.writeException(e);
                } else if (e instanceof IllegalArgumentException) {
                    parcel2.writeException(e);
                } else if (e instanceof NullPointerException) {
                    parcel2.writeException(e);
                } else if (e instanceof IllegalStateException) {
                    parcel2.writeException(e);
                } else if (e instanceof UnsupportedOperationException) {
                    parcel2.writeException(e);
                } else {
                    parcel2.writeException(new IllegalStateException(e));
                }
                return true;
            }
        }

        private static class Proxy implements IProcessManagerService {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override // cn.nubia.service.IProcessManagerService
            public List<Bundle> getCanBeKilledRunningApps() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                this.mRemote.transact(2, obtain, obtain2, 0);
                obtain2.readException();
                ArrayList createTypedArrayList = obtain2.createTypedArrayList(Bundle.CREATOR);
                obtain.recycle();
                obtain2.recycle();
                return createTypedArrayList;
            }

            @Override // cn.nubia.service.IProcessManagerService
            public int killRunningApps(List<Bundle> list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeTypedList(list);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public int oneKeyCleanExcludeCurrentApp(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public int oneKeyCleanExcludeCurrentAppWithId(String str, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeInt(i);
                    this.mRemote.transact(10, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public int oneKeyCleanIncludeCurrentApp() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public List<String> queryWhiteList() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.createStringArrayList();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public long addWhiteList(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(7, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readLong();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public long addWhiteListWithId(String str, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeInt(i);
                    this.mRemote.transact(11, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readLong();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public int deleteWhiteList(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(8, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public int deleteWhiteListWithId(String str, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeInt(i);
                    this.mRemote.transact(12, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public int nbForceStopPackageWithId(String str, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeInt(i);
                    this.mRemote.transact(13, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.service.IProcessManagerService
            public int nbForceStopPackage(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(9, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}

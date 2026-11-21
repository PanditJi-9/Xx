package com.zte.activityevent;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import java.util.List;

/* loaded from: classes2.dex */
public interface IActivityEventsServer extends IInterface {
    public static final String DESCRIPTOR = "com.zte.activityevent.IActivityEventsServer";

    public static class Default implements IActivityEventsServer {
        @Override // com.zte.activityevent.IActivityEventsServer
        public boolean addCallBack(String str, IBinder iBinder, int i) throws RemoteException {
            return false;
        }

        @Override // com.zte.activityevent.IActivityEventsServer
        public boolean addCallBackNubia(String str, IBinder iBinder, int i) throws RemoteException {
            return false;
        }

        @Override // com.zte.activityevent.IActivityEventsServer
        public boolean addInnerCallBack(String str, IBinder iBinder, int i) throws RemoteException {
            return false;
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // com.zte.activityevent.IActivityEventsServer
        public void collectActivityEventDetail(int i, String str, String str2, String str3, int i2, int i3, int i4, String str4) throws RemoteException {
        }

        @Override // com.zte.activityevent.IActivityEventsServer
        public boolean delCallBack(String str) throws RemoteException {
            return false;
        }

        @Override // com.zte.activityevent.IActivityEventsServer
        public boolean delCallBackNubia(String str) throws RemoteException {
            return false;
        }

        @Override // com.zte.activityevent.IActivityEventsServer
        public boolean delInnerCallBack(String str) throws RemoteException {
            return false;
        }

        @Override // com.zte.activityevent.IActivityEventsServer
        public long getFirstInstallTime(String str) throws RemoteException {
            return 0L;
        }

        @Override // com.zte.activityevent.IActivityEventsServer
        public List<Bundle> getVisiblePackageDates() throws RemoteException {
            return null;
        }
    }

    boolean addCallBack(String str, IBinder iBinder, int i) throws RemoteException;

    boolean addCallBackNubia(String str, IBinder iBinder, int i) throws RemoteException;

    boolean addInnerCallBack(String str, IBinder iBinder, int i) throws RemoteException;

    void collectActivityEventDetail(int i, String str, String str2, String str3, int i2, int i3, int i4, String str4) throws RemoteException;

    boolean delCallBack(String str) throws RemoteException;

    boolean delCallBackNubia(String str) throws RemoteException;

    boolean delInnerCallBack(String str) throws RemoteException;

    long getFirstInstallTime(String str) throws RemoteException;

    List<Bundle> getVisiblePackageDates() throws RemoteException;

    public static abstract class Stub extends Binder implements IActivityEventsServer {
        static final int TRANSACTION_addCallBack = 2;
        static final int TRANSACTION_addCallBackNubia = 3;
        static final int TRANSACTION_addInnerCallBack = 1;
        static final int TRANSACTION_collectActivityEventDetail = 7;
        static final int TRANSACTION_delCallBack = 4;
        static final int TRANSACTION_delCallBackNubia = 6;
        static final int TRANSACTION_delInnerCallBack = 5;
        static final int TRANSACTION_getFirstInstallTime = 9;
        static final int TRANSACTION_getVisiblePackageDates = 8;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IActivityEventsServer.DESCRIPTOR);
        }

        public static IActivityEventsServer asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(IActivityEventsServer.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IActivityEventsServer)) {
                return (IActivityEventsServer) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IActivityEventsServer.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IActivityEventsServer.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    boolean addInnerCallBack = addInnerCallBack(parcel.readString(), parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(addInnerCallBack ? 1 : 0);
                    return true;
                case 2:
                    boolean addCallBack = addCallBack(parcel.readString(), parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(addCallBack ? 1 : 0);
                    return true;
                case 3:
                    boolean addCallBackNubia = addCallBackNubia(parcel.readString(), parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(addCallBackNubia ? 1 : 0);
                    return true;
                case 4:
                    boolean delCallBack = delCallBack(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(delCallBack ? 1 : 0);
                    return true;
                case 5:
                    boolean delInnerCallBack = delInnerCallBack(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(delInnerCallBack ? 1 : 0);
                    return true;
                case 6:
                    boolean delCallBackNubia = delCallBackNubia(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(delCallBackNubia ? 1 : 0);
                    return true;
                case 7:
                    collectActivityEventDetail(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 8:
                    List<Bundle> visiblePackageDates = getVisiblePackageDates();
                    parcel2.writeNoException();
                    _Parcel.writeTypedList(parcel2, visiblePackageDates, 1);
                    return true;
                case 9:
                    long firstInstallTime = getFirstInstallTime(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeLong(firstInstallTime);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IActivityEventsServer {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IActivityEventsServer.DESCRIPTOR;
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public boolean addInnerCallBack(String str, IBinder iBinder, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeStrongBinder(iBinder);
                    obtain.writeInt(i);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public boolean addCallBack(String str, IBinder iBinder, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeStrongBinder(iBinder);
                    obtain.writeInt(i);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public boolean addCallBackNubia(String str, IBinder iBinder, int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeStrongBinder(iBinder);
                    obtain.writeInt(i);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public boolean delCallBack(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public boolean delInnerCallBack(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public boolean delCallBackNubia(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public void collectActivityEventDetail(int i, String str, String str2, String str3, int i2, int i3, int i4, String str4) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    obtain.writeInt(i);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeString(str3);
                    obtain.writeInt(i2);
                    obtain.writeInt(i3);
                    obtain.writeInt(i4);
                    obtain.writeString(str4);
                    this.mRemote.transact(7, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public List<Bundle> getVisiblePackageDates() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    this.mRemote.transact(8, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.createTypedArrayList(Bundle.CREATOR);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.activityevent.IActivityEventsServer
            public long getFirstInstallTime(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityEventsServer.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(9, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readLong();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }

    public static class _Parcel {
        private static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> creator) {
            if (parcel.readInt() != 0) {
                return creator.createFromParcel(parcel);
            }
            return null;
        }

        private static <T extends Parcelable> void writeTypedObject(Parcel parcel, T t, int i) {
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

package cn.nubia.arkbase.nbaccount;

import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public interface INbAccountLoginCallback extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.arkbase.nbaccount.INbAccountLoginCallback";

    public static class Default implements INbAccountLoginCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onAccountInfo(Map map, Bitmap bitmap) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onAccountLabel(List list) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onError(String str) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onGameHighLights(List list) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onGameNotes(List list) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onGamePowers(List list) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onGameRecords(List list) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onGameScores(List list) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
        public void onSuccess(String str, Map map, Bitmap bitmap) throws RemoteException {
        }
    }

    void onAccountInfo(Map map, Bitmap bitmap) throws RemoteException;

    void onAccountLabel(List list) throws RemoteException;

    void onError(String str) throws RemoteException;

    void onGameHighLights(List list) throws RemoteException;

    void onGameNotes(List list) throws RemoteException;

    void onGamePowers(List list) throws RemoteException;

    void onGameRecords(List list) throws RemoteException;

    void onGameScores(List list) throws RemoteException;

    void onSuccess(String str, Map map, Bitmap bitmap) throws RemoteException;

    public static abstract class Stub extends Binder implements INbAccountLoginCallback {
        static final int TRANSACTION_onAccountInfo = 4;
        static final int TRANSACTION_onAccountLabel = 5;
        static final int TRANSACTION_onError = 2;
        static final int TRANSACTION_onGameHighLights = 7;
        static final int TRANSACTION_onGameNotes = 6;
        static final int TRANSACTION_onGamePowers = 9;
        static final int TRANSACTION_onGameRecords = 3;
        static final int TRANSACTION_onGameScores = 8;
        static final int TRANSACTION_onSuccess = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, INbAccountLoginCallback.DESCRIPTOR);
        }

        public static INbAccountLoginCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(INbAccountLoginCallback.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof INbAccountLoginCallback)) {
                return (INbAccountLoginCallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(INbAccountLoginCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(INbAccountLoginCallback.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    onSuccess(parcel.readString(), parcel.readHashMap(getClass().getClassLoader()), (Bitmap) _Parcel.readTypedObject(parcel, Bitmap.CREATOR));
                    parcel2.writeNoException();
                    return true;
                case 2:
                    onError(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    onGameRecords(parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                case 4:
                    onAccountInfo(parcel.readHashMap(getClass().getClassLoader()), (Bitmap) _Parcel.readTypedObject(parcel, Bitmap.CREATOR));
                    parcel2.writeNoException();
                    return true;
                case 5:
                    onAccountLabel(parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                case 6:
                    onGameNotes(parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                case 7:
                    onGameHighLights(parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                case 8:
                    onGameScores(parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                case 9:
                    onGamePowers(parcel.readArrayList(getClass().getClassLoader()));
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INbAccountLoginCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return INbAccountLoginCallback.DESCRIPTOR;
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onSuccess(String str, Map map, Bitmap bitmap) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeMap(map);
                    _Parcel.writeTypedObject(obtain, bitmap, 0);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onError(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onGameRecords(List list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeList(list);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onAccountInfo(Map map, Bitmap bitmap) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeMap(map);
                    _Parcel.writeTypedObject(obtain, bitmap, 0);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onAccountLabel(List list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeList(list);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onGameNotes(List list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeList(list);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onGameHighLights(List list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeList(list);
                    this.mRemote.transact(7, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onGameScores(List list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeList(list);
                    this.mRemote.transact(8, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLoginCallback
            public void onGamePowers(List list) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLoginCallback.DESCRIPTOR);
                    obtain.writeList(list);
                    this.mRemote.transact(9, obtain, obtain2, 0);
                    obtain2.readException();
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
    }
}

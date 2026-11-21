package cn.nubia.game.networkacceleration.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface INetworkAccelerationCallback extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback";

    public static class Default implements INetworkAccelerationCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback
        public void onAccessTokenExpired() throws RemoteException {
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback
        public void onNBAccountLoginError(String str) throws RemoteException {
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback
        public void onNBAccountLoginSuccess(String str, String str2, String str3) throws RemoteException {
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback
        public void onXunyouUserState(int i) throws RemoteException {
        }
    }

    void onAccessTokenExpired() throws RemoteException;

    void onNBAccountLoginError(String str) throws RemoteException;

    void onNBAccountLoginSuccess(String str, String str2, String str3) throws RemoteException;

    void onXunyouUserState(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkAccelerationCallback {
        static final int TRANSACTION_onAccessTokenExpired = 4;
        static final int TRANSACTION_onNBAccountLoginError = 2;
        static final int TRANSACTION_onNBAccountLoginSuccess = 1;
        static final int TRANSACTION_onXunyouUserState = 3;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, INetworkAccelerationCallback.DESCRIPTOR);
        }

        public static INetworkAccelerationCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(INetworkAccelerationCallback.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof INetworkAccelerationCallback)) {
                return (INetworkAccelerationCallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(INetworkAccelerationCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(INetworkAccelerationCallback.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                onNBAccountLoginSuccess(parcel.readString(), parcel.readString(), parcel.readString());
                parcel2.writeNoException();
            } else if (i == 2) {
                onNBAccountLoginError(parcel.readString());
                parcel2.writeNoException();
            } else if (i == 3) {
                onXunyouUserState(parcel.readInt());
                parcel2.writeNoException();
            } else if (i == 4) {
                onAccessTokenExpired();
                parcel2.writeNoException();
            } else {
                return super.onTransact(i, parcel, parcel2, i2);
            }
            return true;
        }

        private static class Proxy implements INetworkAccelerationCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return INetworkAccelerationCallback.DESCRIPTOR;
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback
            public void onNBAccountLoginSuccess(String str, String str2, String str3) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationCallback.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeString(str3);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback
            public void onNBAccountLoginError(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationCallback.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback
            public void onXunyouUserState(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationCallback.DESCRIPTOR);
                    obtain.writeInt(i);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback
            public void onAccessTokenExpired() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationCallback.DESCRIPTOR);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}

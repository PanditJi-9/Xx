package cn.nubia.game.networkacceleration.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import cn.nubia.game.networkacceleration.service.INetworkAccelerationCallback;

/* loaded from: classes.dex */
public interface INetworkAccelerationService extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.game.networkacceleration.service.INetworkAccelerationService";

    public static class Default implements INetworkAccelerationService {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
        public void doCloseVPN() throws RemoteException {
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
        public int getAccelerationState() throws RemoteException {
            return 0;
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
        public int getTestUserState() throws RemoteException {
            return 0;
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
        public boolean isAccountLogined() throws RemoteException {
            return false;
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
        public void loginNubiaAccount() throws RemoteException {
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
        public void queryXunyouUserState(String str, String str2) throws RemoteException {
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
        public void registerCallback(INetworkAccelerationCallback iNetworkAccelerationCallback) throws RemoteException {
        }

        @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
        public void unregisterCallback(INetworkAccelerationCallback iNetworkAccelerationCallback) throws RemoteException {
        }
    }

    void doCloseVPN() throws RemoteException;

    int getAccelerationState() throws RemoteException;

    int getTestUserState() throws RemoteException;

    boolean isAccountLogined() throws RemoteException;

    void loginNubiaAccount() throws RemoteException;

    void queryXunyouUserState(String str, String str2) throws RemoteException;

    void registerCallback(INetworkAccelerationCallback iNetworkAccelerationCallback) throws RemoteException;

    void unregisterCallback(INetworkAccelerationCallback iNetworkAccelerationCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements INetworkAccelerationService {
        static final int TRANSACTION_doCloseVPN = 6;
        static final int TRANSACTION_getAccelerationState = 4;
        static final int TRANSACTION_getTestUserState = 5;
        static final int TRANSACTION_isAccountLogined = 1;
        static final int TRANSACTION_loginNubiaAccount = 2;
        static final int TRANSACTION_queryXunyouUserState = 3;
        static final int TRANSACTION_registerCallback = 7;
        static final int TRANSACTION_unregisterCallback = 8;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, INetworkAccelerationService.DESCRIPTOR);
        }

        public static INetworkAccelerationService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(INetworkAccelerationService.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof INetworkAccelerationService)) {
                return (INetworkAccelerationService) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(INetworkAccelerationService.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(INetworkAccelerationService.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    boolean isAccountLogined = isAccountLogined();
                    parcel2.writeNoException();
                    parcel2.writeInt(isAccountLogined ? 1 : 0);
                    return true;
                case 2:
                    loginNubiaAccount();
                    parcel2.writeNoException();
                    return true;
                case 3:
                    queryXunyouUserState(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 4:
                    int accelerationState = getAccelerationState();
                    parcel2.writeNoException();
                    parcel2.writeInt(accelerationState);
                    return true;
                case 5:
                    int testUserState = getTestUserState();
                    parcel2.writeNoException();
                    parcel2.writeInt(testUserState);
                    return true;
                case 6:
                    doCloseVPN();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    registerCallback(INetworkAccelerationCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 8:
                    unregisterCallback(INetworkAccelerationCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INetworkAccelerationService {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return INetworkAccelerationService.DESCRIPTOR;
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
            public boolean isAccountLogined() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationService.DESCRIPTOR);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
            public void loginNubiaAccount() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationService.DESCRIPTOR);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
            public void queryXunyouUserState(String str, String str2) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationService.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
            public int getAccelerationState() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationService.DESCRIPTOR);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
            public int getTestUserState() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationService.DESCRIPTOR);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
            public void doCloseVPN() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationService.DESCRIPTOR);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
            public void registerCallback(INetworkAccelerationCallback iNetworkAccelerationCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationService.DESCRIPTOR);
                    obtain.writeStrongInterface(iNetworkAccelerationCallback);
                    this.mRemote.transact(7, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.game.networkacceleration.service.INetworkAccelerationService
            public void unregisterCallback(INetworkAccelerationCallback iNetworkAccelerationCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INetworkAccelerationService.DESCRIPTOR);
                    obtain.writeStrongInterface(iNetworkAccelerationCallback);
                    this.mRemote.transact(8, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}

package dev.redlab.gamespace.gamehandle;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IGameHandleService extends IInterface {
    public static final String DESCRIPTOR = "dev.redlab.gamespace.gamehandle.IGameHandleService";

    public static class Default implements IGameHandleService {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // dev.redlab.gamespace.gamehandle.IGameHandleService
        public boolean connect(String str) throws RemoteException {
            return false;
        }

        @Override // dev.redlab.gamespace.gamehandle.IGameHandleService
        public void disconnect() throws RemoteException {
        }

        @Override // dev.redlab.gamespace.gamehandle.IGameHandleService
        public int getConnectState() throws RemoteException {
            return 0;
        }

        @Override // dev.redlab.gamespace.gamehandle.IGameHandleService
        public boolean isGameHandleConnected() throws RemoteException {
            return false;
        }
    }

    boolean connect(String str) throws RemoteException;

    void disconnect() throws RemoteException;

    int getConnectState() throws RemoteException;

    boolean isGameHandleConnected() throws RemoteException;

    public static abstract class Stub extends Binder implements IGameHandleService {
        static final int TRANSACTION_connect = 2;
        static final int TRANSACTION_disconnect = 3;
        static final int TRANSACTION_getConnectState = 4;
        static final int TRANSACTION_isGameHandleConnected = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IGameHandleService.DESCRIPTOR);
        }

        public static IGameHandleService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(IGameHandleService.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IGameHandleService)) {
                return (IGameHandleService) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IGameHandleService.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IGameHandleService.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                boolean isGameHandleConnected = isGameHandleConnected();
                parcel2.writeNoException();
                parcel2.writeInt(isGameHandleConnected ? 1 : 0);
            } else if (i == 2) {
                boolean connect = connect(parcel.readString());
                parcel2.writeNoException();
                parcel2.writeInt(connect ? 1 : 0);
            } else if (i == 3) {
                disconnect();
                parcel2.writeNoException();
            } else if (i == 4) {
                int connectState = getConnectState();
                parcel2.writeNoException();
                parcel2.writeInt(connectState);
            } else {
                return super.onTransact(i, parcel, parcel2, i2);
            }
            return true;
        }

        private static class Proxy implements IGameHandleService {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IGameHandleService.DESCRIPTOR;
            }

            @Override // dev.redlab.gamespace.gamehandle.IGameHandleService
            public boolean isGameHandleConnected() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGameHandleService.DESCRIPTOR);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // dev.redlab.gamespace.gamehandle.IGameHandleService
            public boolean connect(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGameHandleService.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // dev.redlab.gamespace.gamehandle.IGameHandleService
            public void disconnect() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGameHandleService.DESCRIPTOR);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // dev.redlab.gamespace.gamehandle.IGameHandleService
            public int getConnectState() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGameHandleService.DESCRIPTOR);
                    this.mRemote.transact(4, obtain, obtain2, 0);
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

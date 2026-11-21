package cn.nubia.gamepi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import cn.nubia.gamepi.IGameSceneCallback;

/* loaded from: classes2.dex */
public interface IGameSceneInterface extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.gamepi.IGameSceneInterface";

    public static class Default implements IGameSceneInterface {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.gamepi.IGameSceneInterface
        public void setCallback(IGameSceneCallback iGameSceneCallback) throws RemoteException {
        }
    }

    void setCallback(IGameSceneCallback iGameSceneCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements IGameSceneInterface {
        static final int TRANSACTION_setCallback = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IGameSceneInterface.DESCRIPTOR);
        }

        public static IGameSceneInterface asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(IGameSceneInterface.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IGameSceneInterface)) {
                return (IGameSceneInterface) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IGameSceneInterface.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IGameSceneInterface.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                setCallback(IGameSceneCallback.Stub.asInterface(parcel.readStrongBinder()));
                parcel2.writeNoException();
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        private static class Proxy implements IGameSceneInterface {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IGameSceneInterface.DESCRIPTOR;
            }

            @Override // cn.nubia.gamepi.IGameSceneInterface
            public void setCallback(IGameSceneCallback iGameSceneCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGameSceneInterface.DESCRIPTOR);
                    obtain.writeStrongInterface(iGameSceneCallback);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}

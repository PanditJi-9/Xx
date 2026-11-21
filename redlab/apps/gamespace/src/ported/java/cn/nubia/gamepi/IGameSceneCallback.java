package cn.nubia.gamepi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes2.dex */
public interface IGameSceneCallback extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.gamepi.IGameSceneCallback";

    public static class Default implements IGameSceneCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.gamepi.IGameSceneCallback
        public void onSceneChange(int i) throws RemoteException {
        }
    }

    void onSceneChange(int i) throws RemoteException;

    public static abstract class Stub extends Binder implements IGameSceneCallback {
        static final int TRANSACTION_onSceneChange = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IGameSceneCallback.DESCRIPTOR);
        }

        public static IGameSceneCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(IGameSceneCallback.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IGameSceneCallback)) {
                return (IGameSceneCallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IGameSceneCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IGameSceneCallback.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                onSceneChange(parcel.readInt());
                parcel2.writeNoException();
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        private static class Proxy implements IGameSceneCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IGameSceneCallback.DESCRIPTOR;
            }

            @Override // cn.nubia.gamepi.IGameSceneCallback
            public void onSceneChange(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGameSceneCallback.DESCRIPTOR);
                    obtain.writeInt(i);
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

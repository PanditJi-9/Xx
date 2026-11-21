package cn.nubia.gpu.drivers.gpuprofile;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes2.dex */
public interface IGpuCallback extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.gpu.drivers.gpuprofile.IGpuCallback";

    public static class Default implements IGpuCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuCallback
        public void onError(int i, String str) throws RemoteException {
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuCallback
        public void onResult(String str) throws RemoteException {
        }
    }

    void onError(int i, String str) throws RemoteException;

    void onResult(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IGpuCallback {
        static final int TRANSACTION_onError = 2;
        static final int TRANSACTION_onResult = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IGpuCallback.DESCRIPTOR);
        }

        public static IGpuCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(IGpuCallback.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IGpuCallback)) {
                return (IGpuCallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IGpuCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IGpuCallback.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                onResult(parcel.readString());
                parcel2.writeNoException();
            } else if (i == 2) {
                onError(parcel.readInt(), parcel.readString());
                parcel2.writeNoException();
            } else {
                return super.onTransact(i, parcel, parcel2, i2);
            }
            return true;
        }

        private static class Proxy implements IGpuCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IGpuCallback.DESCRIPTOR;
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuCallback
            public void onResult(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuCallback.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuCallback
            public void onError(int i, String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuCallback.DESCRIPTOR);
                    obtain.writeInt(i);
                    obtain.writeString(str);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}

package com.zte.performance.zperf;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes2.dex */
public interface ITGPASceneCallback extends IInterface {
    public static final String DESCRIPTOR = "com.zte.performance.zperf.ITGPASceneCallback";

    public static class Default implements ITGPASceneCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // com.zte.performance.zperf.ITGPASceneCallback
        public void onSceneChanged(int i, String str) throws RemoteException {
        }
    }

    void onSceneChanged(int i, String str) throws RemoteException;

    public static abstract class Stub extends Binder implements ITGPASceneCallback {
        static final int TRANSACTION_onSceneChanged = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, ITGPASceneCallback.DESCRIPTOR);
        }

        public static ITGPASceneCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(ITGPASceneCallback.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof ITGPASceneCallback)) {
                return (ITGPASceneCallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(ITGPASceneCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(ITGPASceneCallback.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                onSceneChanged(parcel.readInt(), parcel.readString());
                parcel2.writeNoException();
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        private static class Proxy implements ITGPASceneCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITGPASceneCallback.DESCRIPTOR;
            }

            @Override // com.zte.performance.zperf.ITGPASceneCallback
            public void onSceneChanged(int i, String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(ITGPASceneCallback.DESCRIPTOR);
                    obtain.writeInt(i);
                    obtain.writeString(str);
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

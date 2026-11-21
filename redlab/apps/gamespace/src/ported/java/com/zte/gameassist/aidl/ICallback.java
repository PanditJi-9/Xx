package com.zte.gameassist.aidl;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes2.dex */
public interface ICallback extends IInterface {
    public static final String DESCRIPTOR = "com.zte.gameassist.aidl.ICallback";

    public static class Default implements ICallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // com.zte.gameassist.aidl.ICallback
        public void callback(String str, Bundle bundle) throws RemoteException {
        }
    }

    void callback(String str, Bundle bundle) throws RemoteException;

    public static abstract class Stub extends Binder implements ICallback {
        static final int TRANSACTION_callback = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, ICallback.DESCRIPTOR);
        }

        public static ICallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(ICallback.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof ICallback)) {
                return (ICallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(ICallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(ICallback.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                callback(parcel.readString(), (Bundle) parcel.readTypedObject(Bundle.CREATOR));
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        private static class Proxy implements ICallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ICallback.DESCRIPTOR;
            }

            @Override // com.zte.gameassist.aidl.ICallback
            public void callback(String str, Bundle bundle) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(ICallback.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeTypedObject(bundle, 0);
                    this.mRemote.transact(1, obtain, null, 1);
                } finally {
                    obtain.recycle();
                }
            }
        }
    }
}

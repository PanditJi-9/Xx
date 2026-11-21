package com.zte.performance.scene;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes2.dex */
public interface ITaskCallback extends IInterface {
    public static final String DESCRIPTOR = "com.zte.performance.scene.ITaskCallback";

    public static class Default implements ITaskCallback {
        @Override // com.zte.performance.scene.ITaskCallback
        public void actionPerformed(Message message) throws RemoteException {
        }

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }
    }

    void actionPerformed(Message message) throws RemoteException;

    public static abstract class Stub extends Binder implements ITaskCallback {
        static final int TRANSACTION_actionPerformed = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, ITaskCallback.DESCRIPTOR);
        }

        public static ITaskCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(ITaskCallback.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof ITaskCallback)) {
                return (ITaskCallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(ITaskCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(ITaskCallback.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                actionPerformed((Message) parcel.readTypedObject(Message.CREATOR));
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        private static class Proxy implements ITaskCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return ITaskCallback.DESCRIPTOR;
            }

            @Override // com.zte.performance.scene.ITaskCallback
            public void actionPerformed(Message message) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(ITaskCallback.DESCRIPTOR);
                    obtain.writeTypedObject(message, 0);
                    this.mRemote.transact(1, obtain, null, 1);
                } finally {
                    obtain.recycle();
                }
            }
        }
    }
}

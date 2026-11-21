package com.zte.streamgame.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.zte.streamgame.aidl.IStreamGameCallback;

/* loaded from: classes2.dex */
public interface IStreamGame extends IInterface {
    public static final String DESCRIPTOR = "com.zte.streamgame.aidl.IStreamGame";

    public static class Default implements IStreamGame {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // com.zte.streamgame.aidl.IStreamGame
        public void init(IStreamGameCallback iStreamGameCallback) throws RemoteException {
        }

        @Override // com.zte.streamgame.aidl.IStreamGame
        public void request(String str) throws RemoteException {
        }
    }

    void init(IStreamGameCallback iStreamGameCallback) throws RemoteException;

    void request(String str) throws RemoteException;

    public static abstract class Stub extends Binder implements IStreamGame {
        static final int TRANSACTION_init = 1;
        static final int TRANSACTION_request = 2;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IStreamGame.DESCRIPTOR);
        }

        public static IStreamGame asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(IStreamGame.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IStreamGame)) {
                return (IStreamGame) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IStreamGame.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IStreamGame.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                init(IStreamGameCallback.Stub.asInterface(parcel.readStrongBinder()));
                parcel2.writeNoException();
            } else if (i == 2) {
                request(parcel.readString());
                parcel2.writeNoException();
            } else {
                return super.onTransact(i, parcel, parcel2, i2);
            }
            return true;
        }

        private static class Proxy implements IStreamGame {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IStreamGame.DESCRIPTOR;
            }

            @Override // com.zte.streamgame.aidl.IStreamGame
            public void init(IStreamGameCallback iStreamGameCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IStreamGame.DESCRIPTOR);
                    obtain.writeStrongInterface(iStreamGameCallback);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.streamgame.aidl.IStreamGame
            public void request(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IStreamGame.DESCRIPTOR);
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

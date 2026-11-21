package com.zte.toolsmanager;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes2.dex */
public interface IToolsListener extends IInterface {
    public static final String DESCRIPTOR = "com.zte.toolsmanager.IToolsListener";

    public static class Default implements IToolsListener {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // com.zte.toolsmanager.IToolsListener
        public void onAppFunctionsChanged(String appFunctions) throws RemoteException {
        }

        @Override // com.zte.toolsmanager.IToolsListener
        public void onAppFunctionsFetchComplete(String appFunctions) throws RemoteException {
        }
    }

    void onAppFunctionsChanged(String appFunctions) throws RemoteException;

    void onAppFunctionsFetchComplete(String appFunctions) throws RemoteException;

    public static abstract class Stub extends Binder implements IToolsListener {
        static final int TRANSACTION_onAppFunctionsChanged = 2;
        static final int TRANSACTION_onAppFunctionsFetchComplete = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IToolsListener.DESCRIPTOR);
        }

        public static IToolsListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface queryLocalInterface = obj.queryLocalInterface(IToolsListener.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IToolsListener)) {
                return (IToolsListener) queryLocalInterface;
            }
            return new Proxy(obj);
        }

        @Override // android.os.Binder
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code >= 1 && code <= 16777215) {
                data.enforceInterface(IToolsListener.DESCRIPTOR);
            }
            if (code == 1598968902) {
                reply.writeString(IToolsListener.DESCRIPTOR);
                return true;
            }
            if (code == 1) {
                onAppFunctionsFetchComplete(data.readString());
                reply.writeNoException();
            } else if (code == 2) {
                onAppFunctionsChanged(data.readString());
                reply.writeNoException();
            } else {
                return super.onTransact(code, data, reply, flags);
            }
            return true;
        }

        private static class Proxy implements IToolsListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IToolsListener.DESCRIPTOR;
            }

            @Override // com.zte.toolsmanager.IToolsListener
            public void onAppFunctionsFetchComplete(String appFunctions) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IToolsListener.DESCRIPTOR);
                    obtain.writeString(appFunctions);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.toolsmanager.IToolsListener
            public void onAppFunctionsChanged(String appFunctions) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IToolsListener.DESCRIPTOR);
                    obtain.writeString(appFunctions);
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

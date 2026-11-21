package com.zte.toolsmanager;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import com.zte.toolsmanager.IToolsListener;

/* loaded from: classes2.dex */
public interface IToolsManager extends IInterface {
    public static final String DESCRIPTOR = "com.zte.toolsmanager.IToolsManager";

    public static class Default implements IToolsManager {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // com.zte.toolsmanager.IToolsManager
        public String getToolCapabilities() throws RemoteException {
            return null;
        }

        @Override // com.zte.toolsmanager.IToolsManager
        public String getToolCapabilityByFeature(String filterJson) throws RemoteException {
            return null;
        }

        @Override // com.zte.toolsmanager.IToolsManager
        public ToolsInfo getToolsInfoByToolsName(String toolName) throws RemoteException {
            return null;
        }

        @Override // com.zte.toolsmanager.IToolsManager
        public boolean registerAppFunctionsListener(IToolsListener listener) throws RemoteException {
            return false;
        }

        @Override // com.zte.toolsmanager.IToolsManager
        public boolean registerFilterAppFunctionsListener(IToolsListener listener, String filterJson) throws RemoteException {
            return false;
        }

        @Override // com.zte.toolsmanager.IToolsManager
        public boolean unregisterAppFunctionsListener(IToolsListener listener) throws RemoteException {
            return false;
        }
    }

    String getToolCapabilities() throws RemoteException;

    String getToolCapabilityByFeature(String filterJson) throws RemoteException;

    ToolsInfo getToolsInfoByToolsName(String toolName) throws RemoteException;

    boolean registerAppFunctionsListener(IToolsListener listener) throws RemoteException;

    boolean registerFilterAppFunctionsListener(IToolsListener listener, String filterJson) throws RemoteException;

    boolean unregisterAppFunctionsListener(IToolsListener listener) throws RemoteException;

    public static abstract class Stub extends Binder implements IToolsManager {
        static final int TRANSACTION_getToolCapabilities = 1;
        static final int TRANSACTION_getToolCapabilityByFeature = 2;
        static final int TRANSACTION_getToolsInfoByToolsName = 6;
        static final int TRANSACTION_registerAppFunctionsListener = 3;
        static final int TRANSACTION_registerFilterAppFunctionsListener = 4;
        static final int TRANSACTION_unregisterAppFunctionsListener = 5;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IToolsManager.DESCRIPTOR);
        }

        public static IToolsManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface queryLocalInterface = obj.queryLocalInterface(IToolsManager.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IToolsManager)) {
                return (IToolsManager) queryLocalInterface;
            }
            return new Proxy(obj);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IToolsManager.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IToolsManager.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    String toolCapabilities = getToolCapabilities();
                    parcel2.writeNoException();
                    parcel2.writeString(toolCapabilities);
                    return true;
                case 2:
                    String toolCapabilityByFeature = getToolCapabilityByFeature(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(toolCapabilityByFeature);
                    return true;
                case 3:
                    boolean registerAppFunctionsListener = registerAppFunctionsListener(IToolsListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(registerAppFunctionsListener ? 1 : 0);
                    return true;
                case 4:
                    boolean registerFilterAppFunctionsListener = registerFilterAppFunctionsListener(IToolsListener.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(registerFilterAppFunctionsListener ? 1 : 0);
                    return true;
                case 5:
                    boolean unregisterAppFunctionsListener = unregisterAppFunctionsListener(IToolsListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(unregisterAppFunctionsListener ? 1 : 0);
                    return true;
                case 6:
                    ToolsInfo toolsInfoByToolsName = getToolsInfoByToolsName(parcel.readString());
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, toolsInfoByToolsName, 1);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IToolsManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IToolsManager.DESCRIPTOR;
            }

            @Override // com.zte.toolsmanager.IToolsManager
            public String getToolCapabilities() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IToolsManager.DESCRIPTOR);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readString();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.toolsmanager.IToolsManager
            public String getToolCapabilityByFeature(String filterJson) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IToolsManager.DESCRIPTOR);
                    obtain.writeString(filterJson);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readString();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.toolsmanager.IToolsManager
            public boolean registerAppFunctionsListener(IToolsListener listener) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IToolsManager.DESCRIPTOR);
                    obtain.writeStrongInterface(listener);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.toolsmanager.IToolsManager
            public boolean registerFilterAppFunctionsListener(IToolsListener listener, String filterJson) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IToolsManager.DESCRIPTOR);
                    obtain.writeStrongInterface(listener);
                    obtain.writeString(filterJson);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.toolsmanager.IToolsManager
            public boolean unregisterAppFunctionsListener(IToolsListener listener) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IToolsManager.DESCRIPTOR);
                    obtain.writeStrongInterface(listener);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // com.zte.toolsmanager.IToolsManager
            public ToolsInfo getToolsInfoByToolsName(String toolName) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IToolsManager.DESCRIPTOR);
                    obtain.writeString(toolName);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                    return (ToolsInfo) _Parcel.readTypedObject(obtain2, ToolsInfo.CREATOR);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }

    public static class _Parcel {
        /* JADX INFO: Access modifiers changed from: private */
        public static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> c) {
            if (parcel.readInt() != 0) {
                return c.createFromParcel(parcel);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedObject(Parcel parcel, T value, int parcelableFlags) {
            if (value != null) {
                parcel.writeInt(1);
                value.writeToParcel(parcel, parcelableFlags);
            } else {
                parcel.writeInt(0);
            }
        }
    }
}

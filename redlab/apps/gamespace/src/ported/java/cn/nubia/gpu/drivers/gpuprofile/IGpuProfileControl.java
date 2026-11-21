package cn.nubia.gpu.drivers.gpuprofile;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import cn.nubia.gpu.drivers.gpuprofile.IGpuCallback;

/* loaded from: classes2.dex */
public interface IGpuProfileControl extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl";

    public static class Default implements IGpuProfileControl {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
        public GpuSettingsEntry getGpuSettingsEntry(String str) throws RemoteException {
            return null;
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
        public GpuSettingsEntry getGpuSettingsEntryInSprd(String str) throws RemoteException {
            return null;
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
        public boolean isSupportGpuSettingsInSprd(String str) throws RemoteException {
            return false;
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
        public void performGetGPUInfoTask(IGpuCallback iGpuCallback) throws RemoteException {
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
        public boolean save(String str, int i, int i2, int i3, int i4, int i5) throws RemoteException {
            return false;
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
        public boolean store(String str, int i, int i2, int i3, int i4, int i5, int i6) throws RemoteException {
            return false;
        }

        @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
        public boolean storeInSprd(String str, int i, int i2, int i3, int i4) throws RemoteException {
            return false;
        }
    }

    GpuSettingsEntry getGpuSettingsEntry(String str) throws RemoteException;

    GpuSettingsEntry getGpuSettingsEntryInSprd(String str) throws RemoteException;

    boolean isSupportGpuSettingsInSprd(String str) throws RemoteException;

    void performGetGPUInfoTask(IGpuCallback iGpuCallback) throws RemoteException;

    boolean save(String str, int i, int i2, int i3, int i4, int i5) throws RemoteException;

    boolean store(String str, int i, int i2, int i3, int i4, int i5, int i6) throws RemoteException;

    boolean storeInSprd(String str, int i, int i2, int i3, int i4) throws RemoteException;

    public static abstract class Stub extends Binder implements IGpuProfileControl {
        static final int TRANSACTION_getGpuSettingsEntry = 2;
        static final int TRANSACTION_getGpuSettingsEntryInSprd = 7;
        static final int TRANSACTION_isSupportGpuSettingsInSprd = 5;
        static final int TRANSACTION_performGetGPUInfoTask = 4;
        static final int TRANSACTION_save = 1;
        static final int TRANSACTION_store = 3;
        static final int TRANSACTION_storeInSprd = 6;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IGpuProfileControl.DESCRIPTOR);
        }

        public static IGpuProfileControl asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(IGpuProfileControl.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IGpuProfileControl)) {
                return (IGpuProfileControl) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IGpuProfileControl.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IGpuProfileControl.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    boolean save = save(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(save ? 1 : 0);
                    return true;
                case 2:
                    GpuSettingsEntry gpuSettingsEntry = getGpuSettingsEntry(parcel.readString());
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, gpuSettingsEntry, 1);
                    return true;
                case 3:
                    boolean store = store(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(store ? 1 : 0);
                    return true;
                case 4:
                    performGetGPUInfoTask(IGpuCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 5:
                    boolean isSupportGpuSettingsInSprd = isSupportGpuSettingsInSprd(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(isSupportGpuSettingsInSprd ? 1 : 0);
                    return true;
                case 6:
                    boolean storeInSprd = storeInSprd(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(storeInSprd ? 1 : 0);
                    return true;
                case 7:
                    GpuSettingsEntry gpuSettingsEntryInSprd = getGpuSettingsEntryInSprd(parcel.readString());
                    parcel2.writeNoException();
                    _Parcel.writeTypedObject(parcel2, gpuSettingsEntryInSprd, 1);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IGpuProfileControl {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IGpuProfileControl.DESCRIPTOR;
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
            public boolean save(String str, int i, int i2, int i3, int i4, int i5) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuProfileControl.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    obtain.writeInt(i3);
                    obtain.writeInt(i4);
                    obtain.writeInt(i5);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
            public GpuSettingsEntry getGpuSettingsEntry(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuProfileControl.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                    return (GpuSettingsEntry) _Parcel.readTypedObject(obtain2, GpuSettingsEntry.CREATOR);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
            public boolean store(String str, int i, int i2, int i3, int i4, int i5, int i6) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuProfileControl.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    obtain.writeInt(i3);
                    obtain.writeInt(i4);
                    obtain.writeInt(i5);
                    obtain.writeInt(i6);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
            public void performGetGPUInfoTask(IGpuCallback iGpuCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuProfileControl.DESCRIPTOR);
                    obtain.writeStrongInterface(iGpuCallback);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
            public boolean isSupportGpuSettingsInSprd(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuProfileControl.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
            public boolean storeInSprd(String str, int i, int i2, int i3, int i4) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuProfileControl.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeInt(i);
                    obtain.writeInt(i2);
                    obtain.writeInt(i3);
                    obtain.writeInt(i4);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.gpu.drivers.gpuprofile.IGpuProfileControl
            public GpuSettingsEntry getGpuSettingsEntryInSprd(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IGpuProfileControl.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(7, obtain, obtain2, 0);
                    obtain2.readException();
                    return (GpuSettingsEntry) _Parcel.readTypedObject(obtain2, GpuSettingsEntry.CREATOR);
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }

    public static class _Parcel {
        /* JADX INFO: Access modifiers changed from: private */
        public static <T> T readTypedObject(Parcel parcel, Parcelable.Creator<T> creator) {
            if (parcel.readInt() != 0) {
                return creator.createFromParcel(parcel);
            }
            return null;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static <T extends Parcelable> void writeTypedObject(Parcel parcel, T t, int i) {
            if (t != null) {
                parcel.writeInt(1);
                t.writeToParcel(parcel, i);
            } else {
                parcel.writeInt(0);
            }
        }
    }
}

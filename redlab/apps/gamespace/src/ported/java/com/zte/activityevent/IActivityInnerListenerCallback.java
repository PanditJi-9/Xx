package com.zte.activityevent;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes2.dex */
public interface IActivityInnerListenerCallback extends IInterface {
    public static final String DESCRIPTOR = "com.zte.activityevent.IActivityInnerListenerCallback";

    public static class Default implements IActivityInnerListenerCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // com.zte.activityevent.IActivityInnerListenerCallback
        public void onNotifyActivityEvent(int i, String str, String str2, String str3, int i2, int i3, int i4) throws RemoteException {
        }
    }

    void onNotifyActivityEvent(int i, String str, String str2, String str3, int i2, int i3, int i4) throws RemoteException;

    public static abstract class Stub extends Binder implements IActivityInnerListenerCallback {
        static final int TRANSACTION_onNotifyActivityEvent = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, IActivityInnerListenerCallback.DESCRIPTOR);
        }

        public static IActivityInnerListenerCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(IActivityInnerListenerCallback.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof IActivityInnerListenerCallback)) {
                return (IActivityInnerListenerCallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(IActivityInnerListenerCallback.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(IActivityInnerListenerCallback.DESCRIPTOR);
                return true;
            }
            if (i == 1) {
                onNotifyActivityEvent(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt());
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        private static class Proxy implements IActivityInnerListenerCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return IActivityInnerListenerCallback.DESCRIPTOR;
            }

            @Override // com.zte.activityevent.IActivityInnerListenerCallback
            public void onNotifyActivityEvent(int i, String str, String str2, String str3, int i2, int i3, int i4) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(IActivityInnerListenerCallback.DESCRIPTOR);
                    obtain.writeInt(i);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeString(str3);
                    obtain.writeInt(i2);
                    obtain.writeInt(i3);
                    obtain.writeInt(i4);
                    this.mRemote.transact(1, obtain, null, 1);
                } finally {
                    obtain.recycle();
                }
            }
        }
    }
}

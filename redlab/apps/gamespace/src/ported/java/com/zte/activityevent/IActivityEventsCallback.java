package com.zte.activityevent;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes2.dex */
public interface IActivityEventsCallback extends IInterface {
    public static final String DESCRIPTOR = "com.zte.activityevent.IActivityEventsCallback";

    public static class Default implements IActivityEventsCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // com.zte.activityevent.IActivityEventsCallback
        public void notifiyActivityEvent(int i, String str, String str2, String str3) throws RemoteException {
        }
    }

    void notifiyActivityEvent(int i, String str, String str2, String str3) throws RemoteException;

    public static abstract class Stub extends Binder implements IActivityEventsCallback {
        static final int TRANSACTION_notifiyActivityEvent = 1;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, "com.zte.activityevent.IActivityEventsCallback");
        }

        public static IActivityEventsCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.zte.activityevent.IActivityEventsCallback");
            if (queryLocalInterface != null && (queryLocalInterface instanceof IActivityEventsCallback)) {
                return (IActivityEventsCallback) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface("com.zte.activityevent.IActivityEventsCallback");
            }
            if (i == 1598968902) {
                parcel2.writeString("com.zte.activityevent.IActivityEventsCallback");
                return true;
            }
            if (i == 1) {
                notifiyActivityEvent(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString());
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        private static class Proxy implements IActivityEventsCallback {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return "com.zte.activityevent.IActivityEventsCallback";
            }

            @Override // com.zte.activityevent.IActivityEventsCallback
            public void notifiyActivityEvent(int i, String str, String str2, String str3) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.zte.activityevent.IActivityEventsCallback");
                    obtain.writeInt(i);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeString(str3);
                    this.mRemote.transact(1, obtain, null, 1);
                } finally {
                    obtain.recycle();
                }
            }
        }
    }
}

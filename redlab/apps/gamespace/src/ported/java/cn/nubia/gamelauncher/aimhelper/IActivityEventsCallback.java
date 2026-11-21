package dev.redlab.gamespace.aimhelper;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/* loaded from: classes.dex */
public interface IActivityEventsCallback extends IInterface {
    public static final String DESCRIPTOR = "com.zte.activityevent.IActivityEventsCallback";

    public static class Default implements IActivityEventsCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // dev.redlab.gamespace.aimhelper.IActivityEventsCallback
        public void notifiyActivityEvent(int i, String str, String str2, String str3) throws RemoteException {
        }
    }

    void notifiyActivityEvent(int i, String str, String str2, String str3) throws RemoteException;

    public static abstract class BaseStub extends Binder implements IActivityEventsCallback {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public BaseStub() {
            attachInterface(this, "com.zte.activityevent.IActivityEventsCallback");
        }

        public static IActivityEventsCallback asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface("com.zte.activityevent.IActivityEventsCallback");
            return (queryLocalInterface == null || !(queryLocalInterface instanceof IActivityEventsCallback)) ? new Proxy(iBinder) : (IActivityEventsCallback) queryLocalInterface;
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString("com.zte.activityevent.IActivityEventsCallback");
                return true;
            }
            if (i == 1) {
                parcel.enforceInterface("com.zte.activityevent.IActivityEventsCallback");
                notifiyActivityEvent(parcel.readInt(), parcel.readString(), parcel.readString(), parcel.readString());
                return true;
            }
            return super.onTransact(i, parcel, parcel2, i2);
        }

        public static boolean setDefaultImpl(IActivityEventsCallback iActivityEventsCallback) {
            if (Proxy.sDefaultImpl != null) {
                throw new IllegalStateException("setDefaultImpl() called twice");
            }
            if (iActivityEventsCallback == null) {
                return false;
            }
            Proxy.sDefaultImpl = iActivityEventsCallback;
            return true;
        }

        public static IActivityEventsCallback getDefaultImpl() {
            return Proxy.sDefaultImpl;
        }

        private static class Proxy implements IActivityEventsCallback {
            public static IActivityEventsCallback sDefaultImpl;
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

            @Override // dev.redlab.gamespace.aimhelper.IActivityEventsCallback
            public void notifiyActivityEvent(int i, String str, String str2, String str3) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken("com.zte.activityevent.IActivityEventsCallback");
                    obtain.writeInt(i);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeString(str3);
                    if (this.mRemote.transact(1, obtain, null, 1) || BaseStub.getDefaultImpl() == null) {
                        return;
                    }
                    BaseStub.getDefaultImpl().notifiyActivityEvent(i, str, str2, str3);
                } finally {
                    obtain.recycle();
                }
            }
        }
    }
}

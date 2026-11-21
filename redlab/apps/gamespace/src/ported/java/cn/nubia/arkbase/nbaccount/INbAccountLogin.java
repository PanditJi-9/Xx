package cn.nubia.arkbase.nbaccount;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import cn.nubia.arkbase.nbaccount.INbAccountLoginCallback;

/* loaded from: classes.dex */
public interface INbAccountLogin extends IInterface {
    public static final String DESCRIPTOR = "cn.nubia.arkbase.nbaccount.INbAccountLogin";

    public static class Default implements INbAccountLogin {
        @Override // android.os.IInterface
        public IBinder asBinder() {
            return null;
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public boolean isAccountLogined() throws RemoteException {
            return false;
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void loadAccountInfo() throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void loadAccountLabel() throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void loadGameHighLights() throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void loadGameNotes() throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void loadGamePowers(String str) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void loadGameRecords() throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void loadGameScores(String str) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void loginNubiaAccount() throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void registerCallback(INbAccountLoginCallback iNbAccountLoginCallback) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void reportChatAssistant(String str) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void reportInstalledGameApp(int i) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void reportRedMagicTime(String str, String str2, String str3) throws RemoteException {
        }

        @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
        public void unregisterCallback(INbAccountLoginCallback iNbAccountLoginCallback) throws RemoteException {
        }
    }

    boolean isAccountLogined() throws RemoteException;

    void loadAccountInfo() throws RemoteException;

    void loadAccountLabel() throws RemoteException;

    void loadGameHighLights() throws RemoteException;

    void loadGameNotes() throws RemoteException;

    void loadGamePowers(String str) throws RemoteException;

    void loadGameRecords() throws RemoteException;

    void loadGameScores(String str) throws RemoteException;

    void loginNubiaAccount() throws RemoteException;

    void registerCallback(INbAccountLoginCallback iNbAccountLoginCallback) throws RemoteException;

    void reportChatAssistant(String str) throws RemoteException;

    void reportInstalledGameApp(int i) throws RemoteException;

    void reportRedMagicTime(String str, String str2, String str3) throws RemoteException;

    void unregisterCallback(INbAccountLoginCallback iNbAccountLoginCallback) throws RemoteException;

    public static abstract class Stub extends Binder implements INbAccountLogin {
        static final int TRANSACTION_isAccountLogined = 1;
        static final int TRANSACTION_loadAccountInfo = 6;
        static final int TRANSACTION_loadAccountLabel = 7;
        static final int TRANSACTION_loadGameHighLights = 12;
        static final int TRANSACTION_loadGameNotes = 11;
        static final int TRANSACTION_loadGamePowers = 14;
        static final int TRANSACTION_loadGameRecords = 5;
        static final int TRANSACTION_loadGameScores = 13;
        static final int TRANSACTION_loginNubiaAccount = 2;
        static final int TRANSACTION_registerCallback = 3;
        static final int TRANSACTION_reportChatAssistant = 9;
        static final int TRANSACTION_reportInstalledGameApp = 8;
        static final int TRANSACTION_reportRedMagicTime = 10;
        static final int TRANSACTION_unregisterCallback = 4;

        @Override // android.os.IInterface
        public IBinder asBinder() {
            return this;
        }

        public Stub() {
            attachInterface(this, INbAccountLogin.DESCRIPTOR);
        }

        public static INbAccountLogin asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface queryLocalInterface = iBinder.queryLocalInterface(INbAccountLogin.DESCRIPTOR);
            if (queryLocalInterface != null && (queryLocalInterface instanceof INbAccountLogin)) {
                return (INbAccountLogin) queryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override // android.os.Binder
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i >= 1 && i <= 16777215) {
                parcel.enforceInterface(INbAccountLogin.DESCRIPTOR);
            }
            if (i == 1598968902) {
                parcel2.writeString(INbAccountLogin.DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    boolean isAccountLogined = isAccountLogined();
                    parcel2.writeNoException();
                    parcel2.writeInt(isAccountLogined ? 1 : 0);
                    return true;
                case 2:
                    loginNubiaAccount();
                    parcel2.writeNoException();
                    return true;
                case 3:
                    registerCallback(INbAccountLoginCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 4:
                    unregisterCallback(INbAccountLoginCallback.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 5:
                    loadGameRecords();
                    parcel2.writeNoException();
                    return true;
                case 6:
                    loadAccountInfo();
                    parcel2.writeNoException();
                    return true;
                case 7:
                    loadAccountLabel();
                    parcel2.writeNoException();
                    return true;
                case 8:
                    reportInstalledGameApp(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 9:
                    reportChatAssistant(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 10:
                    reportRedMagicTime(parcel.readString(), parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 11:
                    loadGameNotes();
                    parcel2.writeNoException();
                    return true;
                case 12:
                    loadGameHighLights();
                    parcel2.writeNoException();
                    return true;
                case 13:
                    loadGameScores(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 14:
                    loadGamePowers(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements INbAccountLogin {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override // android.os.IInterface
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return INbAccountLogin.DESCRIPTOR;
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public boolean isAccountLogined() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    this.mRemote.transact(1, obtain, obtain2, 0);
                    obtain2.readException();
                    return obtain2.readInt() != 0;
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void loginNubiaAccount() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    this.mRemote.transact(2, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void registerCallback(INbAccountLoginCallback iNbAccountLoginCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    obtain.writeStrongInterface(iNbAccountLoginCallback);
                    this.mRemote.transact(3, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void unregisterCallback(INbAccountLoginCallback iNbAccountLoginCallback) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    obtain.writeStrongInterface(iNbAccountLoginCallback);
                    this.mRemote.transact(4, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void loadGameRecords() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    this.mRemote.transact(5, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void loadAccountInfo() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    this.mRemote.transact(6, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void loadAccountLabel() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    this.mRemote.transact(7, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void reportInstalledGameApp(int i) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    obtain.writeInt(i);
                    this.mRemote.transact(8, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void reportChatAssistant(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(9, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void reportRedMagicTime(String str, String str2, String str3) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    obtain.writeString(str);
                    obtain.writeString(str2);
                    obtain.writeString(str3);
                    this.mRemote.transact(10, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void loadGameNotes() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    this.mRemote.transact(11, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void loadGameHighLights() throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    this.mRemote.transact(12, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void loadGameScores(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(13, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }

            @Override // cn.nubia.arkbase.nbaccount.INbAccountLogin
            public void loadGamePowers(String str) throws RemoteException {
                Parcel obtain = Parcel.obtain();
                Parcel obtain2 = Parcel.obtain();
                try {
                    obtain.writeInterfaceToken(INbAccountLogin.DESCRIPTOR);
                    obtain.writeString(str);
                    this.mRemote.transact(14, obtain, obtain2, 0);
                    obtain2.readException();
                } finally {
                    obtain2.recycle();
                    obtain.recycle();
                }
            }
        }
    }
}

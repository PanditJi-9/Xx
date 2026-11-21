package cn.nubia.gamecenter.settings.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import com.zte.streamgame.aidl.IStreamGame;
import com.zte.streamgame.aidl.IStreamGameCallback;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class IStreamGameMgr {
    private static final String TAG = "IStreamGameMgr";
    private static volatile IStreamGameMgr sIStreamGameService;
    private Context mContext;
    private IStreamGame mIStreamGame;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mIsConn = false;
    private boolean mInForeground = false;
    final ArrayList<String> mPendingRequest = new ArrayList<>();
    private IBinder.DeathRecipient mStreamGameServiceDeath = new IBinder.DeathRecipient() { // from class: cn.nubia.gamecenter.settings.utils.IStreamGameMgr.1
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Log.e(IStreamGameMgr.TAG, "binderDied");
            IStreamGameMgr.this.unbindService();
            IStreamGameMgr.this.mHandler.postDelayed(new Runnable() { // from class: cn.nubia.gamecenter.settings.utils.IStreamGameMgr.1.1
                @Override // java.lang.Runnable
                public void run() {
                    IStreamGameMgr.this.binderService();
                }
            }, 1000L);
        }
    };
    private final IStreamGameCallback.Stub mCallbackBinder = new IStreamGameCallback.Stub() { // from class: cn.nubia.gamecenter.settings.utils.IStreamGameMgr.2
        @Override // com.zte.streamgame.aidl.IStreamGameCallback
        public void bundleCallback(Bundle bundle) throws RemoteException {
        }

        @Override // com.zte.streamgame.aidl.IStreamGameCallback
        public void responseCallback(final String str) throws RemoteException {
            IStreamGameMgr.this.mHandler.post(new Runnable() { // from class: cn.nubia.gamecenter.settings.utils.IStreamGameMgr.2.1
                @Override // java.lang.Runnable
                public void run() {
                    IStreamGameMgr.this.response(str);
                }
            });
        }
    };
    private final ServiceConnection mServiceConnection = new ServiceConnection() { // from class: cn.nubia.gamecenter.settings.utils.IStreamGameMgr.3
        @Override // android.content.ServiceConnection
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(IStreamGameMgr.TAG, "onServiceConnected");
            try {
                iBinder.linkToDeath(IStreamGameMgr.this.mStreamGameServiceDeath, 0);
            } catch (Exception e) {
                Log.e(IStreamGameMgr.TAG, "linkToDeath");
                e.printStackTrace();
            }
            IStreamGameMgr.this.mIStreamGame = IStreamGame.Stub.asInterface(iBinder);
            IStreamGameMgr.this.mIsConn = true;
            try {
                IStreamGameMgr.this.mIStreamGame.init(IStreamGameMgr.this.mCallbackBinder);
                Iterator<String> it = IStreamGameMgr.this.mPendingRequest.iterator();
                while (it.hasNext()) {
                    String next = it.next();
                    IStreamGameMgr.this.mIStreamGame.request(next);
                    Log.w(IStreamGameMgr.TAG, "===== request0 requsetData=" + next);
                }
                IStreamGameMgr.this.mPendingRequest.clear();
            } catch (Exception e2) {
                Log.e(IStreamGameMgr.TAG, "onServiceConnected e=" + e2.getMessage());
                e2.printStackTrace();
            }
        }

        @Override // android.content.ServiceConnection
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(IStreamGameMgr.TAG, "onServiceDisconnected");
            IStreamGameMgr.this.mIStreamGame = null;
            IStreamGameMgr.this.mIsConn = false;
        }
    };

    /* JADX INFO: Access modifiers changed from: private */
    public void response(String str) {
    }

    public static IStreamGameMgr getInstance(Context context) {
        if (sIStreamGameService == null) {
            synchronized (IStreamGameMgr.class) {
                if (sIStreamGameService == null) {
                    sIStreamGameService = new IStreamGameMgr(context);
                }
            }
        }
        return sIStreamGameService;
    }

    public IStreamGameMgr(Context context) {
        this.mContext = context;
    }

    public void binderService() {
        if (this.mIsConn) {
            return;
        }
        Intent intent = new Intent();
        intent.setClassName("com.zte.streamgame", "com.zte.streamgame.StreamGameService");
        try {
            this.mIsConn = this.mContext.bindService(intent, this.mServiceConnection, 1);
        } catch (Exception e) {
            this.mIStreamGame = null;
            this.mIsConn = false;
            Log.e(TAG, "binderService   binder error = " + e.getMessage());
        }
        if (this.mIsConn) {
            return;
        }
        Log.e(TAG, "binderService   binderService fail");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unbindService() {
        if (this.mIsConn) {
            this.mContext.unbindService(this.mServiceConnection);
            this.mIsConn = false;
        }
        IStreamGame iStreamGame = this.mIStreamGame;
        if (iStreamGame != null) {
            iStreamGame.asBinder().unlinkToDeath(this.mStreamGameServiceDeath, 0);
            this.mIStreamGame = null;
        }
    }

    public void request(String str) {
        IStreamGame iStreamGame;
        if (!this.mIsConn || (iStreamGame = this.mIStreamGame) == null) {
            Log.e(TAG, "request not bind service");
            this.mPendingRequest.add(str);
            binderService();
            return;
        }
        try {
            iStreamGame.request(str);
            Log.w(TAG, "===== request requsetData=" + str);
        } catch (Exception e) {
            Log.e(TAG, "request e=" + e.getMessage());
            e.printStackTrace();
            this.mIsConn = false;
            binderService();
        }
    }
}

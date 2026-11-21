package cn.nubia.gamepanel;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public class ArkBaseTrackManager {
    private static final String ARK_BASE_COLLECTION_SERVICE = "cn.nubia.arkbase.service.ArkBaseCollectionService";
    private static final String ARK_BASE_PKG = "cn.nubia.arkbase";
    private static final String TAG = "ArkBaseTrackManager";
    private static final long TIMEOUT = 3000;
    private static TrackHandler mTrackHandler;
    private static HandlerThread mTrackThread;
    private boolean isConn;
    private ServiceConnection mConn;
    private Context mContext;
    private Messenger mService;

    private static class SingleInstance {
        static ArkBaseTrackManager instance = new ArkBaseTrackManager();

        private SingleInstance() {
        }
    }

    private ArkBaseTrackManager() {
        this.mConn = new ServiceConnection() { // from class: cn.nubia.gamepanel.ArkBaseTrackManager.1
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                synchronized (ArkBaseTrackManager.mTrackThread) {
                    ArkBaseTrackManager.this.mService = new Messenger(iBinder);
                    ArkBaseTrackManager.this.isConn = true;
                    ArkBaseTrackManager.mTrackThread.notify();
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName componentName) {
                synchronized (ArkBaseTrackManager.mTrackThread) {
                    ArkBaseTrackManager.this.mService = null;
                    ArkBaseTrackManager.this.isConn = false;
                    ArkBaseTrackManager.mTrackThread.notify();
                }
            }
        };
    }

    public static ArkBaseTrackManager getInstance() {
        return SingleInstance.instance;
    }

    public void init(Context context) {
        this.mContext = context.getApplicationContext();
        if (mTrackHandler == null) {
            HandlerThread handlerThread = new HandlerThread("ArkBaseTrackEvent", 10);
            mTrackThread = handlerThread;
            handlerThread.start();
            mTrackHandler = new TrackHandler(mTrackThread.getLooper());
        }
    }

    final class TrackHandler extends Handler {
        static final int TRACK_EVENT_MSG = 1;
        static final int UNBIND_SERVICE_MSG = 2;

        public TrackHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            Log.i(ArkBaseTrackManager.TAG, "MSG msg.what = " + message.what);
            int i = message.what;
            if (i == 1) {
                if (ActivityManager.isUserAMonkey()) {
                    return;
                }
                try {
                    synchronized (ArkBaseTrackManager.mTrackThread) {
                        if (!ArkBaseTrackManager.this.isConn || ArkBaseTrackManager.this.mService == null) {
                            ArkBaseTrackManager.this.bindServiceInvoked();
                            ArkBaseTrackManager.mTrackThread.wait();
                            if (ArkBaseTrackManager.this.isConn && ArkBaseTrackManager.this.mService != null) {
                                ArkBaseTrackManager.this.mService.send(message);
                            }
                            return;
                        }
                        ArkBaseTrackManager.this.mService.send(message);
                        return;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return;
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                    return;
                }
            }
            if (i == 2) {
                try {
                    synchronized (ArkBaseTrackManager.mTrackThread) {
                        if (ArkBaseTrackManager.this.isConn) {
                            ArkBaseTrackManager.this.mService = null;
                            ArkBaseTrackManager.this.isConn = false;
                            if (ArkBaseTrackManager.this.mContext != null) {
                                ArkBaseTrackManager.this.mContext.unbindService(ArkBaseTrackManager.this.mConn);
                                return;
                            }
                            return;
                        }
                        return;
                    }
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
            }
            super.handleMessage(message);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindServiceInvoked() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("cn.nubia.arkbase", ARK_BASE_COLLECTION_SERVICE));
        Context context = this.mContext;
        if (context != null) {
            context.bindService(intent, this.mConn, 1);
        }
    }

    public void sendEvent(int i, ArrayList<Bundle> arrayList) {
        Log.i(TAG, "sendEvent");
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("datas", arrayList);
        bundle.putInt("eventId", i);
        mTrackHandler.removeMessages(2);
        Message obtainMessage = mTrackHandler.obtainMessage(1);
        obtainMessage.setData(bundle);
        mTrackHandler.sendMessage(obtainMessage);
        TrackHandler trackHandler = mTrackHandler;
        trackHandler.sendMessageDelayed(trackHandler.obtainMessage(2), 3000L);
    }
}

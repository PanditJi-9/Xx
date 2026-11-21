package cn.nubia.settings.trackclient;

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
import cn.nubia.gamecenter.settings.applearning.AppDbSchema;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes2.dex */
public class NubiaTrackManager {
    private static final String CLS = "com.zte.analytics.datacollection.DataCollectionService";
    public static final String EVENT_NAME = "event_name";
    public static final String GAMESPACE_PACKAGENAME = "dev.redlab.gamespace";
    private static final String OWNER_NAME = "owner_name";
    private static final String PKG = "com.zte.analytics";
    private static final String TAG = "NubiaTrackManager";
    private static final long TIEMOUT = 3000;
    static TrackHandler sTrackHandler;
    static HandlerThread sTrackThread;
    private boolean isConn;
    private ServiceConnection mConn;
    private Context mContext;
    private Messenger mService;

    private NubiaTrackManager() {
        this.mConn = new ServiceConnection() { // from class: cn.nubia.settings.trackclient.NubiaTrackManager.1
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                synchronized (NubiaTrackManager.sTrackThread) {
                    NubiaTrackManager.this.mService = new Messenger(iBinder);
                    NubiaTrackManager.this.isConn = true;
                    NubiaTrackManager.sTrackThread.notify();
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName componentName) {
                synchronized (NubiaTrackManager.sTrackThread) {
                    NubiaTrackManager.this.mService = null;
                    NubiaTrackManager.this.isConn = false;
                    NubiaTrackManager.sTrackThread.notify();
                }
            }
        };
    }

    public static NubiaTrackManager getInstance() {
        return SingleInstance.instance;
    }

    private static class SingleInstance {
        static NubiaTrackManager instance = new NubiaTrackManager();

        private SingleInstance() {
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
            int i = message.what;
            if (i != 1) {
                if (i == 2) {
                    synchronized (NubiaTrackManager.sTrackThread) {
                        if (NubiaTrackManager.this.isConn) {
                            NubiaTrackManager.this.mService = null;
                            NubiaTrackManager.this.isConn = false;
                            if (NubiaTrackManager.this.mContext != null) {
                                try {
                                    NubiaTrackManager.this.mContext.unbindService(NubiaTrackManager.this.mConn);
                                    return;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return;
                                }
                            }
                            return;
                        }
                        return;
                    }
                }
                super.handleMessage(message);
                return;
            }
            if (ActivityManager.isUserAMonkey()) {
                return;
            }
            try {
                synchronized (NubiaTrackManager.sTrackThread) {
                    if (!NubiaTrackManager.this.isConn || NubiaTrackManager.this.mService == null) {
                        NubiaTrackManager.this.bindServiceInvoked();
                        NubiaTrackManager.sTrackThread.wait();
                        if (NubiaTrackManager.this.isConn && NubiaTrackManager.this.mService != null) {
                            NubiaTrackManager.this.mService.send(message);
                        }
                        return;
                    }
                    NubiaTrackManager.this.mService.send(message);
                }
            } catch (RemoteException e2) {
                e2.printStackTrace();
            } catch (InterruptedException e3) {
                e3.printStackTrace();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void bindServiceInvoked() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PKG, CLS));
        Context context = this.mContext;
        if (context != null) {
            context.bindService(intent, this.mConn, 1);
        }
    }

    public void init(Context context) {
        this.mContext = context;
        if (sTrackHandler == null) {
            HandlerThread handlerThread = new HandlerThread("NubiaTrackEvent", 10);
            sTrackThread = handlerThread;
            handlerThread.start();
            sTrackHandler = new TrackHandler(sTrackThread.getLooper());
        }
    }

    public void unbindServiceInvoked() {
        synchronized (sTrackThread) {
            if (this.isConn) {
                this.mService = null;
                this.isConn = false;
                Context context = this.mContext;
                if (context != null) {
                    try {
                        context.unbindService(this.mConn);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    this.mContext = null;
                }
            }
        }
    }

    public void sendEvent(String str, String str2) {
        if (sTrackHandler == null) {
            LogUtil.w(TAG, "sTrackHandler is null!!");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(OWNER_NAME, str);
        bundle.putString("event_name", str2);
        sTrackHandler.removeMessages(2);
        Message obtainMessage = sTrackHandler.obtainMessage(1);
        obtainMessage.setData(bundle);
        sTrackHandler.sendMessage(obtainMessage);
        TrackHandler trackHandler = sTrackHandler;
        trackHandler.sendMessageDelayed(trackHandler.obtainMessage(2), 3000L);
        LogUtil.v(TAG, str2);
    }

    public void sendEvent(String str, String str2, String str3, String str4) {
        if (sTrackHandler == null) {
            LogUtil.w(TAG, "sTrackHandler is null!!");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(OWNER_NAME, str);
        bundle.putString("event_name", str2);
        bundle.putString(str3, str4);
        sTrackHandler.removeMessages(2);
        Message obtainMessage = sTrackHandler.obtainMessage(1);
        obtainMessage.setData(bundle);
        sTrackHandler.sendMessage(obtainMessage);
        TrackHandler trackHandler = sTrackHandler;
        trackHandler.sendMessageDelayed(trackHandler.obtainMessage(2), 3000L);
        LogUtil.v(TAG, str2 + " " + str3 + " " + str4);
    }

    public void sendEventEveryDay(String str, String str2, String str3, String str4) {
        if (sTrackHandler == null) {
            LogUtil.w(TAG, "sTrackHandler is null!!");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString("package_name", str);
        bundle.putString("event_name", str2);
        bundle.putString("action_type", str3);
        bundle.putString(AppDbSchema.AppTable.OneDayCols.ACTION_VALUE, str4);
        bundle.putInt(AppDbSchema.AppTable.OneDayCols.REPORT_INTERVAL, 1);
        sTrackHandler.removeMessages(2);
        sendEvent("dev.redlab.gamespace", bundle);
    }

    public void sendEvent(String str, String str2, String str3, boolean z) {
        if (sTrackHandler == null) {
            LogUtil.w(TAG, "sTrackHandler is null!!");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(OWNER_NAME, str);
        bundle.putString("event_name", str2);
        bundle.putBoolean(str3, z);
        sTrackHandler.removeMessages(2);
        Message obtainMessage = sTrackHandler.obtainMessage(1);
        obtainMessage.setData(bundle);
        sTrackHandler.sendMessage(obtainMessage);
        TrackHandler trackHandler = sTrackHandler;
        trackHandler.sendMessageDelayed(trackHandler.obtainMessage(2), 3000L);
        LogUtil.v(TAG, str2 + " " + str3 + " " + z);
    }

    public void sendEvent(String str, String str2, String str3, int i) {
        if (sTrackHandler == null) {
            LogUtil.w(TAG, "sTrackHandler is null!!");
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putString(OWNER_NAME, str);
        bundle.putString("event_name", str2);
        bundle.putInt(str3, i);
        sTrackHandler.removeMessages(2);
        Message obtainMessage = sTrackHandler.obtainMessage(1);
        obtainMessage.setData(bundle);
        sTrackHandler.sendMessage(obtainMessage);
        TrackHandler trackHandler = sTrackHandler;
        trackHandler.sendMessageDelayed(trackHandler.obtainMessage(2), 3000L);
        LogUtil.v(TAG, str2 + " " + str3 + " " + i);
    }

    public void sendEvent(String str, Bundle bundle) {
        if (sTrackHandler == null) {
            LogUtil.w(TAG, "sTrackHandler is null!!");
            return;
        }
        bundle.putString(OWNER_NAME, str);
        sTrackHandler.removeMessages(2);
        Message obtainMessage = sTrackHandler.obtainMessage(1);
        obtainMessage.setData(bundle);
        sTrackHandler.sendMessage(obtainMessage);
        TrackHandler trackHandler = sTrackHandler;
        trackHandler.sendMessageDelayed(trackHandler.obtainMessage(2), 3000L);
        LogUtil.v(TAG, bundle.toString());
    }

    public void sendEvent(String str, String str2, Bundle bundle) {
        if (sTrackHandler == null) {
            LogUtil.w(TAG, "sTrackHandler is null!!");
            return;
        }
        bundle.putString(OWNER_NAME, str);
        bundle.putString("event_name", str2);
        sTrackHandler.removeMessages(2);
        Message obtainMessage = sTrackHandler.obtainMessage(1);
        obtainMessage.setData(bundle);
        sTrackHandler.sendMessage(obtainMessage);
        TrackHandler trackHandler = sTrackHandler;
        trackHandler.sendMessageDelayed(trackHandler.obtainMessage(2), 3000L);
        LogUtil.v(TAG, bundle.toString());
    }
}

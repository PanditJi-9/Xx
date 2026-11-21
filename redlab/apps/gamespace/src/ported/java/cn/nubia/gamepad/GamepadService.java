package cn.nubia.gamepad;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import cn.nubia.gamepad.utils.GamepadContentHelper;

/* loaded from: classes2.dex */
public class GamepadService extends Service {
    public static final String ACTION_CHEKED = "enable";
    public static final String ACTION_TYPE = "action_type";
    public static final int MSG_ENABLE_OPERATION_VIBRATE_DEVICES = 0;
    public static final int MSG_STOP_SERVICE = 1000;
    public static final String TAG = "GameLauncher_GamepadService";
    private Context mContext;
    private Handler mHandler;

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        this.mHandler = new WorkHandler(getMainLooper());
        this.mContext = getApplicationContext();
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        if (intent == null) {
            Log.e(TAG, "intent is null!");
        } else if (intent.getIntExtra("action_type", -1) == 0) {
            sendMsgDelayed(0, Integer.valueOf(intent.getIntExtra("enable", 0)), 0);
        }
        sendMsgDelayed(1000, null, 2000);
        return super.onStartCommand(intent, i, i2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendMsgDelayed(int i, Object obj, int i2) {
        this.mHandler.removeMessages(1000);
        this.mHandler.removeMessages(i);
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(i, obj), i2);
    }

    private class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            Log.e(GamepadService.TAG, "MSG msg.what = " + message.what);
            int i = message.what;
            if (i == 0) {
                GamepadContentHelper.setVibrateDevices(GamepadService.this.getApplicationContext(), ((Integer) message.obj).intValue());
                GamepadService.this.sendMsgDelayed(1000, null, 1000);
            } else if (i == 1000) {
                GamepadService.this.stopService();
            }
            super.handleMessage(message);
        }
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopService() {
        stopSelf();
    }
}

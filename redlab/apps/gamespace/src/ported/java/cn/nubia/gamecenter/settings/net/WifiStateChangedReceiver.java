package cn.nubia.gamecenter.settings.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import androidx.media3.common.C;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes.dex */
public class WifiStateChangedReceiver extends BroadcastReceiver {
    private static final String DB_NAME_GAME_ASSISTANT_SIM = "game_assistant_sim_enable";
    private static final String TAG = "WifiStateChangedReceiver";
    private Handler mWiFiHandler;

    private boolean isSupportShieldAssistantSim() {
        return true;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtil.d(TAG, "WifiStateChangedReceiver receiver action=" + action);
        if (!TextUtils.isEmpty(action) && action.equals("android.net.wifi.STATE_CHANGE") && isSupportShieldAssistantSim()) {
            updateGameAssistantSimState(context);
        }
    }

    private void updateGameAssistantSimState(final Context context) {
        if (this.mWiFiHandler == null) {
            this.mWiFiHandler = new Handler();
        }
        this.mWiFiHandler.removeCallbacksAndMessages(null);
        this.mWiFiHandler.postDelayed(new Runnable() { // from class: cn.nubia.gamecenter.settings.net.WifiStateChangedReceiver.1
            @Override // java.lang.Runnable
            public void run() {
                int i = Settings.Global.getInt(context.getContentResolver(), "game_assistant_sim_enable", 0);
                if (WifiStateChangedReceiver.this.isWifiConnected(context)) {
                    LogUtil.d(WifiStateChangedReceiver.TAG, "WifiStateChangedReceiver Wifi Connected assistantSimState=" + i);
                    if (i == 1) {
                        Settings.Global.putInt(context.getContentResolver(), "game_assistant_sim_enable", -1);
                        return;
                    }
                    return;
                }
                LogUtil.d(WifiStateChangedReceiver.TAG, "WifiStateChangedReceiver Wifi not Connected assistantSimState=" + i);
                if (i == -1) {
                    Settings.Global.putInt(context.getContentResolver(), "game_assistant_sim_enable", 1);
                }
            }
        }, C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService("connectivity");
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        if (networkCapabilities != null) {
            return networkCapabilities.hasTransport(1);
        }
        return false;
    }
}

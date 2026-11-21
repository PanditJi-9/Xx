package cn.nubia.settings.owlsysaciton;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes2.dex */
public class OwlSysCvReceiver extends BroadcastReceiver {
    private static final String ACTION_OWL_FISTBOOTDAY = "cn.nubia.owlsystem.firstbootdayaction";
    private static final String TAG = "OwlSysCvReceiver";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        LogUtil.i(TAG, "receive Reboot or Update msg.");
        String action = intent.getAction();
        if (TextUtils.isEmpty(action) || !action.equals(ACTION_OWL_FISTBOOTDAY)) {
            return;
        }
        context.startService(new Intent(context.getApplicationContext(), (Class<?>) OwlSysCvInitService.class));
    }
}

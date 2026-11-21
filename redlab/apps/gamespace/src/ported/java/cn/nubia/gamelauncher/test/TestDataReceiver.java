package dev.redlab.gamespace.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import dev.redlab.gamespace.util.WorkThread;

/* loaded from: classes.dex */
public class TestDataReceiver extends BroadcastReceiver {
    public static String TEST_ACTION = "dev.redlab.gamespace.datatest";

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (TEST_ACTION.equals(intent.getAction())) {
            WorkThread.runOnWorkThread(new Runnable() { // from class: dev.redlab.gamespace.test.TestDataReceiver.1
                @Override // java.lang.Runnable
                public void run() {
                    Log.i("lsm", "TestDataReceiver exportAppDbFile");
                    new CopyTestDataUtil().exportAppDbFile();
                }
            });
        }
    }
}

package cn.nubia.common.helper;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

/* loaded from: classes.dex */
public class WorkHelper {
    private static Handler sHandler;
    private static HandlerThread sWorkThread;

    static {
        HandlerThread handlerThread = new HandlerThread("CommonWorkThread");
        sWorkThread = handlerThread;
        handlerThread.start();
        sHandler = new Handler(sWorkThread.getLooper());
    }

    public static void runOnWorkThread(Runnable runnable) {
        if (Process.myTid() == sWorkThread.getThreadId()) {
            runnable.run();
        } else {
            sHandler.post(runnable);
        }
    }

    public static Handler getHandler() {
        return sHandler;
    }
}

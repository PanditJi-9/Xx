package dev.redlab.gamespace.redmagicplanet.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.util.Locale;

/* loaded from: classes.dex */
public class NetUtil {
    private static final String TAG = "NetUtil";

    public static boolean getNetworkConnectState(Context context) {
        boolean z = isWifiConnected(context) || isMobileConnected(context);
        LogUtil.i(TAG, "getNetworkConnectState isConnected : " + z);
        return z;
    }

    /* JADX WARN: Code restructure failed: missing block: B:11:0x0026, code lost:
    
        if (r3.getType() == 9) goto L12;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static boolean isWifiConnected(android.content.Context r3) {
        /*
            android.content.Context r3 = r3.getApplicationContext()
            java.lang.String r0 = "connectivity"
            java.lang.Object r3 = r3.getSystemService(r0)
            android.net.ConnectivityManager r3 = (android.net.ConnectivityManager) r3
            r0 = 0
            android.net.NetworkInfo r3 = r3.getActiveNetworkInfo()     // Catch: java.lang.Exception -> L2a
            if (r3 == 0) goto L2e
            boolean r1 = r3.isConnected()     // Catch: java.lang.Exception -> L2a
            if (r1 == 0) goto L2e
            int r1 = r3.getType()     // Catch: java.lang.Exception -> L2a
            r2 = 1
            if (r1 == r2) goto L28
            int r3 = r3.getType()     // Catch: java.lang.Exception -> L2a
            r1 = 9
            if (r3 != r1) goto L2e
        L28:
            r0 = r2
            goto L2e
        L2a:
            r3 = move-exception
            r3.printStackTrace()
        L2e:
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            java.lang.String r1 = "isWifiConnected : "
            r3.<init>(r1)
            java.lang.StringBuilder r3 = r3.append(r0)
            java.lang.String r3 = r3.toString()
            java.lang.String r1 = "NetUtil"
            dev.redlab.gamespace.redmagicplanet.util.LogUtil.i(r1, r3)
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: dev.redlab.gamespace.redmagicplanet.util.NetUtil.isWifiConnected(android.content.Context):boolean");
    }

    private static boolean isMobileConnected(Context context) {
        boolean z = false;
        try {
            NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getApplicationContext().getSystemService("connectivity")).getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                if (activeNetworkInfo.getType() == 0) {
                    z = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        LogUtil.i(TAG, "isMobileConnected : " + z);
        return z;
    }

    public static String formatTimeAI(long j) {
        if (j <= 0 || j >= 86400000) {
            return "00:00";
        }
        long j2 = j / 1000;
        long j3 = j2 / 3600;
        long j4 = (j2 % 3600) / 60;
        long j5 = j2 % 60;
        if (j3 > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", Long.valueOf(j3), Long.valueOf(j4), Long.valueOf(j5));
        }
        return String.format(Locale.getDefault(), "%02d:%02d", Long.valueOf(j4), Long.valueOf(j5));
    }
}

package com.zte.shared.wrapper;

import android.os.Debug;

/* loaded from: classes2.dex */
public class DebugWrapper {
    public static String getCallers(int i) {
        return Debug.getCallers(i);
    }

    public static String getCallers(int i, int i2) {
        return Debug.getCallers(i, i2);
    }
}

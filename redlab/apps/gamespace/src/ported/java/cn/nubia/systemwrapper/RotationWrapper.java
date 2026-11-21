package cn.nubia.systemwrapper;

import android.content.Context;

/* loaded from: classes2.dex */
public abstract class RotationWrapper {
    public void disable() {
    }

    public void enable() {
    }

    protected abstract void onRotationChanged(int i);

    public RotationWrapper(Context context) {
    }
}

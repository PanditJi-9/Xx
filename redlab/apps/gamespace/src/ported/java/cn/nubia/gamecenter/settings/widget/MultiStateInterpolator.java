package cn.nubia.gamecenter.settings.widget;

import android.view.animation.Interpolator;

/* loaded from: classes.dex */
public class MultiStateInterpolator implements Interpolator {
    private static final String TAG = "MultiStateInterpolator";
    private final Interpolator m_i;
    private final float m_state_1;
    private final float m_state_2;

    public MultiStateInterpolator(Interpolator interpolator, float f, float f2) {
        this.m_i = interpolator;
        f = f > 1.0f ? 1.0f : f;
        f2 = f2 > 1.0f ? 1.0f : f2;
        f2 = f2 < f ? f : f2;
        this.m_state_1 = f;
        this.m_state_2 = f2;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float f) {
        float f2 = this.m_state_1;
        if (f < f2) {
            return this.m_i.getInterpolation(f / f2);
        }
        float f3 = this.m_state_2;
        if (f >= f3) {
            return 0.0f;
        }
        return 1.0f - this.m_i.getInterpolation((f - f2) / (f3 - f2));
    }
}

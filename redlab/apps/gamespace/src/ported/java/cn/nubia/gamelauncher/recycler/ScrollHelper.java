package dev.redlab.gamespace.recycler;

import android.content.Context;
import android.view.ViewConfiguration;

/* loaded from: classes.dex */
public class ScrollHelper {
    private static float DECELERATION_RATE = (float) (Math.log(0.78d) / Math.log(0.9d));
    private static final float INFLEXION = 0.35f;
    Context mContext;
    private final String TAG = "scroll";
    private float mFlingFriction = ViewConfiguration.getScrollFriction();
    private float mPhysicalCoeff = -1.0f;

    public ScrollHelper(Context context) {
        this.mContext = context;
        initPhysicalCoeffIfNeed();
    }

    private void initPhysicalCoeffIfNeed() {
        if (0.0f <= this.mPhysicalCoeff) {
            return;
        }
        this.mPhysicalCoeff = this.mContext.getResources().getDisplayMetrics().density * 160.0f * 386.0878f * 0.84f;
    }

    private double getSplineDeceleration(int i) {
        return Math.log((Math.abs(i) * INFLEXION) / (this.mFlingFriction * this.mPhysicalCoeff));
    }

    public double getSplineFlingDistance(int i) {
        double splineDeceleration = getSplineDeceleration(i);
        float f = DECELERATION_RATE;
        return this.mFlingFriction * this.mPhysicalCoeff * Math.exp((f / (f - 1.0d)) * splineDeceleration);
    }

    public int getSplineFlingDuration(int i) {
        return (int) (Math.exp(getSplineDeceleration(i) / (DECELERATION_RATE - 1.0d)) * 1000.0d);
    }

    public int getVelocityByDistance(double d) {
        double log = Math.log(Math.abs(d) / (this.mFlingFriction * this.mPhysicalCoeff));
        float f = DECELERATION_RATE;
        return (int) (Math.ceil(Math.abs((Math.exp(log / (f / (f - 1.0d))) * (this.mFlingFriction * this.mPhysicalCoeff)) / 0.3499999940395355d)) * Math.signum(d));
    }
}

package cn.nubia.gamecenter.settings.gamekeylamp;

import android.graphics.PointF;
import android.view.animation.Interpolator;

/* loaded from: classes.dex */
public class ThirdBezierInterpolator implements Interpolator {
    private static final int ACCURACY = 4096;
    private static final String TAG = "ThirdBezierInterpolator";
    private final PointF mControlPoint1;
    private final PointF mControlPoint2;
    private int mLastI = 0;

    public static double cubicCurves(double d, double d2, double d3, double d4, double d5) {
        double d6 = 1.0d - d;
        double d7 = d * d;
        double d8 = d6 * d6;
        return (d8 * d6 * d2) + (d8 * 3.0d * d * d3) + (d6 * 3.0d * d7 * d4) + (d7 * d * d5);
    }

    public ThirdBezierInterpolator(float f, float f2, float f3, float f4) {
        PointF pointF = new PointF();
        this.mControlPoint1 = pointF;
        PointF pointF2 = new PointF();
        this.mControlPoint2 = pointF2;
        pointF.x = f;
        pointF.y = f2;
        pointF2.x = f3;
        pointF2.y = f4;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float f) {
        double cubicCurves = cubicCurves(f, 0.0d, this.mControlPoint1.y, this.mControlPoint2.y, 1.0d);
        if (cubicCurves > 0.999d) {
            this.mLastI = 0;
            cubicCurves = 1.0d;
        }
        return (float) cubicCurves;
    }
}

package cn.nubia.gamepanel;

import java.math.BigDecimal;

/* loaded from: classes2.dex */
public class MathUtils {
    public static int divData(int i, int i2, int i3) {
        if (i3 < 0 || i == 0 || i2 == 0) {
            return 0;
        }
        return (int) Math.floor(new BigDecimal(Integer.toString(i)).divide(new BigDecimal(Integer.toString(i2)), i3, 0).doubleValue() * 10.0d * 10.0d);
    }

    public static double divDataOther(double d, double d2, int i) {
        if (i < 0 || d == 0.0d || d2 == 0.0d) {
            return 0.0d;
        }
        return new BigDecimal(Double.toString(d)).divide(new BigDecimal(Double.toString(d2)), i, 4).doubleValue();
    }

    public static int divData(long j, double d) {
        if (j == 0 || d == 0.0d) {
            return 0;
        }
        return new BigDecimal(Long.toString(j)).divide(new BigDecimal(Double.toString(d)), 0, 0).intValue();
    }
}

package cn.nubia.gamecenter.settings.gamekeylamp;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;

/* loaded from: classes.dex */
public class GradientSegmentDrawable extends Drawable {
    private final int[] mColors;
    private final float mCornerRadiusPx;
    private LayerDrawable mLayerDrawable;

    public GradientSegmentDrawable(int[] iArr, float f) {
        this.mColors = iArr;
        this.mCornerRadiusPx = f;
    }

    @Override // android.graphics.drawable.Drawable
    public void setBounds(int i, int i2, int i3, int i4) {
        int[] iArr;
        super.setBounds(i, i2, i3, i4);
        int i5 = i3 - i;
        int i6 = i4 - i2;
        if (i5 <= 0 || i6 <= 0 || (iArr = this.mColors) == null || iArr.length == 0) {
            this.mLayerDrawable = null;
        } else {
            buildLayerDrawable(i5, i6);
        }
    }

    private void buildLayerDrawable(int i, int i2) {
        int length = this.mColors.length;
        GradientDrawable[] gradientDrawableArr = new GradientDrawable[length];
        int i3 = 0;
        for (int i4 = 0; i4 < length; i4++) {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(0);
            gradientDrawable.setColor(this.mColors[i4]);
            gradientDrawable.setCornerRadii(getCornerRadiiForSegment(i4, length));
            gradientDrawableArr[i4] = gradientDrawable;
        }
        this.mLayerDrawable = new LayerDrawable(gradientDrawableArr);
        int i5 = i / length;
        while (true) {
            int i6 = i3;
            if (i6 < length) {
                i3 = i6 + 1;
                this.mLayerDrawable.setLayerInset(i6, i6 * i5, 0, i - (i3 * i5), 0);
            } else {
                this.mLayerDrawable.setBounds(getBounds());
                return;
            }
        }
    }

    private float[] getCornerRadiiForSegment(int i, int i2) {
        float f = this.mCornerRadiusPx;
        if (i2 == 1) {
            return new float[]{f, f, f, f, f, f, f, f};
        }
        if (i == 0) {
            return new float[]{f, f, 0.0f, 0.0f, 0.0f, 0.0f, f, f};
        }
        if (i == i2 - 1) {
            return new float[]{0.0f, 0.0f, f, f, f, f, 0.0f, 0.0f};
        }
        return new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        LayerDrawable layerDrawable = this.mLayerDrawable;
        if (layerDrawable != null) {
            layerDrawable.setBounds(getBounds());
            this.mLayerDrawable.draw(canvas);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        LayerDrawable layerDrawable = this.mLayerDrawable;
        if (layerDrawable != null) {
            layerDrawable.setAlpha(i);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        LayerDrawable layerDrawable = this.mLayerDrawable;
        if (layerDrawable != null) {
            layerDrawable.setColorFilter(colorFilter);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        LayerDrawable layerDrawable = this.mLayerDrawable;
        if (layerDrawable != null) {
            return layerDrawable.getOpacity();
        }
        return -2;
    }
}

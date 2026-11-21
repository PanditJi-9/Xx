package dev.redlab.gamespace.redmagicplanet.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;

/* loaded from: classes.dex */
public class RedMagicTextureView extends TextureView {
    private static final String TAG = "RedMagicTextureView";
    private int videoHeight;
    private int videoWidth;

    public RedMagicTextureView(Context context) {
        super(context);
    }

    public RedMagicTextureView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public RedMagicTextureView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public RedMagicTextureView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
    }

    public void adaptVideoSize(int i, int i2) {
        if (this.videoWidth == i || this.videoHeight == i2) {
            return;
        }
        this.videoWidth = i;
        this.videoHeight = i2;
        requestLayout();
    }

    @Override // android.view.View
    public void setRotation(float f) {
        if (f != getRotation()) {
            super.setRotation(f);
            requestLayout();
        }
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int i3;
        float rotation = getRotation();
        if (rotation == 90.0f || rotation == 270.0f) {
            i2 = i;
            i = i2;
        }
        int defaultSize = getDefaultSize(this.videoWidth, i);
        int defaultSize2 = getDefaultSize(this.videoHeight, i2);
        if (this.videoWidth > 0 && this.videoHeight > 0) {
            int mode = View.MeasureSpec.getMode(i);
            int size = View.MeasureSpec.getSize(i);
            int mode2 = View.MeasureSpec.getMode(i2);
            int size2 = View.MeasureSpec.getSize(i2);
            if (mode == 1073741824 && mode2 == 1073741824) {
                int i4 = this.videoWidth;
                int i5 = i4 * size2;
                int i6 = this.videoHeight;
                defaultSize = i5 < size * i6 ? (i4 * size2) / i6 : size;
            } else if (mode == 1073741824) {
                int i7 = this.videoHeight;
                int i8 = this.videoWidth;
                int i9 = (size * i7) / i8;
                if (mode2 != Integer.MIN_VALUE || i9 <= size2) {
                    defaultSize = size;
                    defaultSize2 = i9;
                } else {
                    defaultSize = (i8 * size2) / i7;
                }
            } else {
                if (mode2 == 1073741824) {
                    int i10 = this.videoWidth;
                    int i11 = this.videoHeight;
                    int i12 = (size2 * i10) / i11;
                    if (mode != Integer.MIN_VALUE || i12 <= size) {
                        defaultSize2 = size2;
                        defaultSize = i12;
                    } else {
                        defaultSize2 = (i11 * size) / i10;
                    }
                } else {
                    int i13 = this.videoWidth;
                    int i14 = this.videoHeight;
                    if (mode2 != Integer.MIN_VALUE || i14 <= size2) {
                        i3 = i13;
                        size2 = i14;
                    } else {
                        i3 = (size2 * i13) / i14;
                    }
                    if (mode != Integer.MIN_VALUE || i3 <= size) {
                        defaultSize = i3;
                    } else {
                        defaultSize2 = (i14 * size) / i13;
                    }
                }
                defaultSize = size;
            }
            defaultSize2 = size2;
        }
        setMeasuredDimension(defaultSize, defaultSize2);
    }
}

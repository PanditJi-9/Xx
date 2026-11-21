package dev.redlab.gamespace.gamecontrolpanel.virtual;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.VideoView;

/* loaded from: classes.dex */
public class MyVideoView extends VideoView {
    private int mHeight;
    private int mWidth;

    public MyVideoView(Context context) {
        super(context);
    }

    public MyVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public MyVideoView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    public void setMeasure(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
    }

    @Override // android.widget.VideoView, android.view.SurfaceView, android.view.View
    protected void onMeasure(int i, int i2) {
        int i3;
        super.onMeasure(i, i2);
        int size = View.MeasureSpec.getSize(i);
        int i4 = this.mWidth;
        if (i4 <= 0 || (i3 = this.mHeight) <= 0) {
            i3 = size;
        } else {
            size = i4;
        }
        setMeasuredDimension(size, i3);
    }
}

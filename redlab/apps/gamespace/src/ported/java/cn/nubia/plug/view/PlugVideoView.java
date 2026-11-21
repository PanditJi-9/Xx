package cn.nubia.plug.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.VideoView;

/* loaded from: classes2.dex */
public class PlugVideoView extends VideoView {
    public PlugVideoView(Context context) {
        this(context, null);
    }

    public PlugVideoView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override // android.widget.VideoView, android.view.SurfaceView, android.view.View
    protected void onMeasure(int i, int i2) {
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        if (mode == 1073741824 && mode2 == 1073741824) {
            setMeasuredDimension(getDefaultSize(0, i), getDefaultSize(0, i2));
        } else {
            super.onMeasure(i, i2);
        }
    }
}

package dev.redlab.gamespace.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.VideoView;

/* loaded from: classes2.dex */
public class MyVideoView extends VideoView {
    public MyVideoView(Context context) {
        super(context.getApplicationContext());
    }

    public MyVideoView(Context context, AttributeSet attributeSet) {
        super(context.getApplicationContext(), attributeSet);
    }

    @Override // android.widget.VideoView, android.view.SurfaceView, android.view.View
    protected void onMeasure(int i, int i2) {
        setMeasuredDimension(getDefaultSize(0, i), getDefaultSize(0, i2));
    }

    public void setVideoSize(int i, int i2) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = i;
        layoutParams.height = i2;
        setLayoutParams(layoutParams);
    }
}

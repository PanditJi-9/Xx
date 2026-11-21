package dev.redlab.gamespace.redmagicplanet.ui;

import android.graphics.Outline;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewOutlineProvider;

/* loaded from: classes.dex */
public class FullScreenOutlineProvider extends ViewOutlineProvider {
    private float mRadius;

    public FullScreenOutlineProvider(float f) {
        this.mRadius = f;
    }

    @Override // android.view.ViewOutlineProvider
    public void getOutline(View view, Outline outline) {
        Rect rect = new Rect();
        view.getLocalVisibleRect(rect);
        outline.setRoundRect(new Rect(0, rect.top, rect.right - rect.left, rect.bottom - rect.top), this.mRadius);
    }
}

package dev.redlab.gamespace.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

/* loaded from: classes2.dex */
public class ZoomImageView extends ImageView {
    private final int DOWN_SCALE_ANIM_DURATION;
    private final float MAX_SCALE_VALUE;
    private final float MIN_SCALE_VALUE;
    private final int UP_SCALE_ANIM_DURATION;
    ValueAnimator mPressAnimator;
    private float mScale;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mScale = 1.0f;
        this.MIN_SCALE_VALUE = 1.0f;
        this.MAX_SCALE_VALUE = 1.1f;
        this.DOWN_SCALE_ANIM_DURATION = 200;
        this.UP_SCALE_ANIM_DURATION = 150;
    }

    @Override // android.view.View
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        super.onFocusChanged(z, i, rect);
        if (z) {
            doPressAnimator(1.1f, 200);
        } else {
            doPressAnimator(1.0f, 150);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScaleAni(float f) {
        this.mScale = f;
        setScaleX(f);
        setScaleY(f);
    }

    private void doPressAnimator(final float f, int i) {
        ValueAnimator valueAnimator = this.mPressAnimator;
        if (valueAnimator != null) {
            valueAnimator.end();
        }
        ValueAnimator ofFloat = ValueAnimator.ofFloat(this.mScale, f);
        this.mPressAnimator = ofFloat;
        ofFloat.setDuration(i);
        this.mPressAnimator.start();
        this.mPressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: dev.redlab.gamespace.view.ZoomImageView.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                ZoomImageView.this.setScaleAni(((Float) valueAnimator2.getAnimatedValue()).floatValue());
            }
        });
        this.mPressAnimator.addListener(new AnimatorListenerAdapter() { // from class: dev.redlab.gamespace.view.ZoomImageView.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                ZoomImageView.this.setScaleAni(1.1f);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                ZoomImageView.this.setScaleAni(f);
            }
        });
    }
}

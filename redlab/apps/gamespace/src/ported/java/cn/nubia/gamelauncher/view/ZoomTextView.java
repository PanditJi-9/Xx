package dev.redlab.gamespace.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

/* loaded from: classes2.dex */
public class ZoomTextView extends TextView {
    private final int DOWN_SCALE_ANIM_DURATION;
    private final float MAX_SCALE_VALUE;
    private final float MIN_SCALE_VALUE;
    private final int UP_SCALE_ANIM_DURATION;
    ValueAnimator mPressAnimator;
    private float mScale;

    @Override // android.view.View
    public boolean isFocused() {
        return true;
    }

    public ZoomTextView(Context context) {
        this(context, null);
    }

    public ZoomTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mScale = 1.0f;
        this.MIN_SCALE_VALUE = 0.9f;
        this.MAX_SCALE_VALUE = 1.0f;
        this.DOWN_SCALE_ANIM_DURATION = 200;
        this.UP_SCALE_ANIM_DURATION = 150;
        setFocusable(true);
        setFocusableInTouchMode(true);
        setSingleLine();
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
    }

    @Override // android.widget.TextView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isClickable()) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            doPressAnimator(0.9f, 200);
        } else if (action == 1 || action == 3) {
            doPressAnimator(1.0f, 150);
        }
        return super.onTouchEvent(motionEvent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setScale(float f) {
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
        this.mPressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: dev.redlab.gamespace.view.ZoomTextView.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                ZoomTextView.this.setScale(((Float) valueAnimator2.getAnimatedValue()).floatValue());
            }
        });
        this.mPressAnimator.addListener(new AnimatorListenerAdapter() { // from class: dev.redlab.gamespace.view.ZoomTextView.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                ZoomTextView.this.setScale(1.0f);
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                ZoomTextView.this.setScale(f);
            }
        });
    }

    @Override // android.view.View
    public void setAlpha(float f) {
        super.setAlpha(f);
        if (0.0f == f) {
            setClickable(false);
        } else {
            setClickable(true);
        }
    }

    @Override // android.widget.TextView, android.view.View
    protected void onFocusChanged(boolean z, int i, Rect rect) {
        if (z) {
            super.onFocusChanged(z, i, rect);
        }
    }

    @Override // android.widget.TextView, android.view.View
    public void onWindowFocusChanged(boolean z) {
        if (z) {
            super.onWindowFocusChanged(z);
        }
    }
}

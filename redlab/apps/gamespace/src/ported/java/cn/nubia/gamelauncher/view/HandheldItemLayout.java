package dev.redlab.gamespace.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.anim.AnimHelper;

/* loaded from: classes2.dex */
public class HandheldItemLayout extends FrameLayout {
    private final int BORDER_W_END;
    private final int BORDER_W_START;
    private int IMAGE_PADDING_SELECT;
    private int IMAGE_PADDING_UNSELECT_L_R;
    private int IMAGE_PADDING_UNSELECT_T_B;
    private float borderAlpha;
    private int borderWidth;
    private int imagePaddingL;
    private int imagePaddingT;
    private boolean isExpand;
    private boolean isSquare;
    private HandheldItemView mBorderView;
    private View mContentView;
    private int mHeight;
    public boolean mSelect;
    private int mWidth;
    private Runnable pauseAnimRunable;
    private Runnable selectAnimRunable;
    private AnimatorSet set;
    private TextView textView;

    /* JADX INFO: Access modifiers changed from: private */
    public HandheldItemLayout getSelf() {
        return this;
    }

    public HandheldItemLayout(Context context) {
        super(context);
        this.BORDER_W_START = 10;
        this.BORDER_W_END = 4;
        this.IMAGE_PADDING_UNSELECT_L_R = 60;
        this.IMAGE_PADDING_UNSELECT_T_B = 60;
        this.IMAGE_PADDING_SELECT = 52;
        this.imagePaddingL = 60;
        this.imagePaddingT = 60;
        this.mSelect = false;
        this.selectAnimRunable = new Runnable() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.2
            @Override // java.lang.Runnable
            public void run() {
                HandheldItemLayout.this.mBorderView.setSelect(HandheldItemLayout.this.mSelect);
                ObjectAnimator ofInt = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingL", HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_L_R, HandheldItemLayout.this.IMAGE_PADDING_SELECT);
                ofInt.setDuration(167L);
                ofInt.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofInt2 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingT", HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_T_B, HandheldItemLayout.this.IMAGE_PADDING_SELECT);
                ofInt2.setDuration(167L);
                ofInt2.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofInt3 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "borderWidth", 10, 4);
                ofInt3.setDuration(250L);
                ofInt3.setInterpolator(AnimHelper.EASY_EASE2);
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", 0.0f, 1.0f);
                ofFloat.setDuration(333L);
                ofFloat.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(HandheldItemLayout.this.textView, AnimatorHelper.Item.ALPHA, HandheldItemLayout.this.isExpand ? 1.0f : 0.0f, 1.0f);
                ofFloat2.setDuration(167L);
                ofFloat2.setInterpolator(AnimHelper.EASY_EASE);
                if (HandheldItemLayout.this.set == null) {
                    HandheldItemLayout.this.set = new AnimatorSet();
                } else {
                    HandheldItemLayout.this.set.cancel();
                }
                HandheldItemLayout.this.set.play(ofInt).with(ofInt2).with(ofInt3).with(ofFloat).with(ofFloat2);
                HandheldItemLayout.this.set.start();
                ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", 1.0f, 0.4f);
                ofFloat3.setRepeatCount(-1);
                ofFloat3.setRepeatMode(2);
                ofFloat3.setDuration(500L);
                ofFloat3.setInterpolator(AnimHelper.EASY_EASE2);
                HandheldItemLayout.this.set.cancel();
                HandheldItemLayout.this.set.play(ofFloat3);
                HandheldItemLayout.this.set.start();
                HandheldItemLayout.this.postInvalidate();
            }
        };
        this.pauseAnimRunable = new Runnable() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.3
            @Override // java.lang.Runnable
            public void run() {
                if (HandheldItemLayout.this.set == null) {
                    HandheldItemLayout.this.set = new AnimatorSet();
                } else {
                    HandheldItemLayout.this.set.cancel();
                }
                if (!HandheldItemLayout.this.isExpand) {
                    ObjectAnimator ofFloat = ObjectAnimator.ofFloat(HandheldItemLayout.this.textView, AnimatorHelper.Item.ALPHA, 1.0f, 0.0f);
                    ofFloat.setDuration(166L);
                    ofFloat.setInterpolator(AnimHelper.EASY_EASE);
                    ofFloat.start();
                }
                ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", HandheldItemLayout.this.mBorderView.getBorderAlpha(), 0.0f);
                ofFloat2.setDuration(166L);
                ofFloat2.setInterpolator(AnimHelper.EASY_EASE);
                ofFloat2.addListener(new Animator.AnimatorListener() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.3.1
                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationRepeat(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        HandheldItemLayout.this.mBorderView.setSelect(HandheldItemLayout.this.mSelect);
                    }
                });
                ofFloat2.start();
                ObjectAnimator ofInt = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingL", HandheldItemLayout.this.IMAGE_PADDING_SELECT, HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_L_R);
                ofInt.setDuration(167L);
                ofInt.setStartDelay(67L);
                ofInt.setInterpolator(AnimHelper.EASY_EASE);
                ofInt.start();
                ObjectAnimator ofInt2 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingT", HandheldItemLayout.this.IMAGE_PADDING_SELECT, HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_T_B);
                ofInt2.setDuration(167L);
                ofInt2.setStartDelay(67L);
                ofInt2.setInterpolator(AnimHelper.EASY_EASE);
                ofInt2.start();
                HandheldItemLayout.this.postInvalidate();
            }
        };
        init();
    }

    public HandheldItemLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.BORDER_W_START = 10;
        this.BORDER_W_END = 4;
        this.IMAGE_PADDING_UNSELECT_L_R = 60;
        this.IMAGE_PADDING_UNSELECT_T_B = 60;
        this.IMAGE_PADDING_SELECT = 52;
        this.imagePaddingL = 60;
        this.imagePaddingT = 60;
        this.mSelect = false;
        this.selectAnimRunable = new Runnable() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.2
            @Override // java.lang.Runnable
            public void run() {
                HandheldItemLayout.this.mBorderView.setSelect(HandheldItemLayout.this.mSelect);
                ObjectAnimator ofInt = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingL", HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_L_R, HandheldItemLayout.this.IMAGE_PADDING_SELECT);
                ofInt.setDuration(167L);
                ofInt.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofInt2 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingT", HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_T_B, HandheldItemLayout.this.IMAGE_PADDING_SELECT);
                ofInt2.setDuration(167L);
                ofInt2.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofInt3 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "borderWidth", 10, 4);
                ofInt3.setDuration(250L);
                ofInt3.setInterpolator(AnimHelper.EASY_EASE2);
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", 0.0f, 1.0f);
                ofFloat.setDuration(333L);
                ofFloat.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(HandheldItemLayout.this.textView, AnimatorHelper.Item.ALPHA, HandheldItemLayout.this.isExpand ? 1.0f : 0.0f, 1.0f);
                ofFloat2.setDuration(167L);
                ofFloat2.setInterpolator(AnimHelper.EASY_EASE);
                if (HandheldItemLayout.this.set == null) {
                    HandheldItemLayout.this.set = new AnimatorSet();
                } else {
                    HandheldItemLayout.this.set.cancel();
                }
                HandheldItemLayout.this.set.play(ofInt).with(ofInt2).with(ofInt3).with(ofFloat).with(ofFloat2);
                HandheldItemLayout.this.set.start();
                ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", 1.0f, 0.4f);
                ofFloat3.setRepeatCount(-1);
                ofFloat3.setRepeatMode(2);
                ofFloat3.setDuration(500L);
                ofFloat3.setInterpolator(AnimHelper.EASY_EASE2);
                HandheldItemLayout.this.set.cancel();
                HandheldItemLayout.this.set.play(ofFloat3);
                HandheldItemLayout.this.set.start();
                HandheldItemLayout.this.postInvalidate();
            }
        };
        this.pauseAnimRunable = new Runnable() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.3
            @Override // java.lang.Runnable
            public void run() {
                if (HandheldItemLayout.this.set == null) {
                    HandheldItemLayout.this.set = new AnimatorSet();
                } else {
                    HandheldItemLayout.this.set.cancel();
                }
                if (!HandheldItemLayout.this.isExpand) {
                    ObjectAnimator ofFloat = ObjectAnimator.ofFloat(HandheldItemLayout.this.textView, AnimatorHelper.Item.ALPHA, 1.0f, 0.0f);
                    ofFloat.setDuration(166L);
                    ofFloat.setInterpolator(AnimHelper.EASY_EASE);
                    ofFloat.start();
                }
                ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", HandheldItemLayout.this.mBorderView.getBorderAlpha(), 0.0f);
                ofFloat2.setDuration(166L);
                ofFloat2.setInterpolator(AnimHelper.EASY_EASE);
                ofFloat2.addListener(new Animator.AnimatorListener() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.3.1
                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationRepeat(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        HandheldItemLayout.this.mBorderView.setSelect(HandheldItemLayout.this.mSelect);
                    }
                });
                ofFloat2.start();
                ObjectAnimator ofInt = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingL", HandheldItemLayout.this.IMAGE_PADDING_SELECT, HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_L_R);
                ofInt.setDuration(167L);
                ofInt.setStartDelay(67L);
                ofInt.setInterpolator(AnimHelper.EASY_EASE);
                ofInt.start();
                ObjectAnimator ofInt2 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingT", HandheldItemLayout.this.IMAGE_PADDING_SELECT, HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_T_B);
                ofInt2.setDuration(167L);
                ofInt2.setStartDelay(67L);
                ofInt2.setInterpolator(AnimHelper.EASY_EASE);
                ofInt2.start();
                HandheldItemLayout.this.postInvalidate();
            }
        };
        init();
    }

    public HandheldItemLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.BORDER_W_START = 10;
        this.BORDER_W_END = 4;
        this.IMAGE_PADDING_UNSELECT_L_R = 60;
        this.IMAGE_PADDING_UNSELECT_T_B = 60;
        this.IMAGE_PADDING_SELECT = 52;
        this.imagePaddingL = 60;
        this.imagePaddingT = 60;
        this.mSelect = false;
        this.selectAnimRunable = new Runnable() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.2
            @Override // java.lang.Runnable
            public void run() {
                HandheldItemLayout.this.mBorderView.setSelect(HandheldItemLayout.this.mSelect);
                ObjectAnimator ofInt = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingL", HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_L_R, HandheldItemLayout.this.IMAGE_PADDING_SELECT);
                ofInt.setDuration(167L);
                ofInt.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofInt2 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingT", HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_T_B, HandheldItemLayout.this.IMAGE_PADDING_SELECT);
                ofInt2.setDuration(167L);
                ofInt2.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofInt3 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "borderWidth", 10, 4);
                ofInt3.setDuration(250L);
                ofInt3.setInterpolator(AnimHelper.EASY_EASE2);
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", 0.0f, 1.0f);
                ofFloat.setDuration(333L);
                ofFloat.setInterpolator(AnimHelper.EASY_EASE);
                ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(HandheldItemLayout.this.textView, AnimatorHelper.Item.ALPHA, HandheldItemLayout.this.isExpand ? 1.0f : 0.0f, 1.0f);
                ofFloat2.setDuration(167L);
                ofFloat2.setInterpolator(AnimHelper.EASY_EASE);
                if (HandheldItemLayout.this.set == null) {
                    HandheldItemLayout.this.set = new AnimatorSet();
                } else {
                    HandheldItemLayout.this.set.cancel();
                }
                HandheldItemLayout.this.set.play(ofInt).with(ofInt2).with(ofInt3).with(ofFloat).with(ofFloat2);
                HandheldItemLayout.this.set.start();
                ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", 1.0f, 0.4f);
                ofFloat3.setRepeatCount(-1);
                ofFloat3.setRepeatMode(2);
                ofFloat3.setDuration(500L);
                ofFloat3.setInterpolator(AnimHelper.EASY_EASE2);
                HandheldItemLayout.this.set.cancel();
                HandheldItemLayout.this.set.play(ofFloat3);
                HandheldItemLayout.this.set.start();
                HandheldItemLayout.this.postInvalidate();
            }
        };
        this.pauseAnimRunable = new Runnable() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.3
            @Override // java.lang.Runnable
            public void run() {
                if (HandheldItemLayout.this.set == null) {
                    HandheldItemLayout.this.set = new AnimatorSet();
                } else {
                    HandheldItemLayout.this.set.cancel();
                }
                if (!HandheldItemLayout.this.isExpand) {
                    ObjectAnimator ofFloat = ObjectAnimator.ofFloat(HandheldItemLayout.this.textView, AnimatorHelper.Item.ALPHA, 1.0f, 0.0f);
                    ofFloat.setDuration(166L);
                    ofFloat.setInterpolator(AnimHelper.EASY_EASE);
                    ofFloat.start();
                }
                ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(HandheldItemLayout.this.getSelf(), "borderAlpha", HandheldItemLayout.this.mBorderView.getBorderAlpha(), 0.0f);
                ofFloat2.setDuration(166L);
                ofFloat2.setInterpolator(AnimHelper.EASY_EASE);
                ofFloat2.addListener(new Animator.AnimatorListener() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.3.1
                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationRepeat(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationStart(Animator animator) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animator) {
                        HandheldItemLayout.this.mBorderView.setSelect(HandheldItemLayout.this.mSelect);
                    }
                });
                ofFloat2.start();
                ObjectAnimator ofInt = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingL", HandheldItemLayout.this.IMAGE_PADDING_SELECT, HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_L_R);
                ofInt.setDuration(167L);
                ofInt.setStartDelay(67L);
                ofInt.setInterpolator(AnimHelper.EASY_EASE);
                ofInt.start();
                ObjectAnimator ofInt2 = ObjectAnimator.ofInt(HandheldItemLayout.this.getSelf(), "imagePaddingT", HandheldItemLayout.this.IMAGE_PADDING_SELECT, HandheldItemLayout.this.IMAGE_PADDING_UNSELECT_T_B);
                ofInt2.setDuration(167L);
                ofInt2.setStartDelay(67L);
                ofInt2.setInterpolator(AnimHelper.EASY_EASE);
                ofInt2.start();
                HandheldItemLayout.this.postInvalidate();
            }
        };
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.HandheldItemLayout, i, 0);
        this.IMAGE_PADDING_UNSELECT_L_R = (int) obtainStyledAttributes.getDimension(0, 0.0f);
        this.IMAGE_PADDING_UNSELECT_T_B = (int) obtainStyledAttributes.getDimension(1, 0.0f);
        obtainStyledAttributes.recycle();
        init();
    }

    private void init() {
        this.imagePaddingL = this.IMAGE_PADDING_UNSELECT_L_R;
        this.imagePaddingT = this.IMAGE_PADDING_UNSELECT_T_B;
        ViewGroup.LayoutParams layoutParams = new FrameLayout.LayoutParams(-1, -1);
        HandheldItemView handheldItemView = new HandheldItemView(getContext());
        this.mBorderView = handheldItemView;
        addView(handheldItemView, layoutParams);
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.mWidth = View.MeasureSpec.getSize(i);
        this.mHeight = View.MeasureSpec.getSize(i2);
    }

    public void setContentView(final View view) {
        View view2 = this.mContentView;
        if (view2 != null) {
            removeView(view2);
        }
        if (this.mWidth == 0 || this.mHeight == 0) {
            post(new Runnable() { // from class: dev.redlab.gamespace.view.HandheldItemLayout.1
                @Override // java.lang.Runnable
                public void run() {
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(-2, -2);
                    layoutParams.gravity = 17;
                    layoutParams.width = (HandheldItemLayout.this.mWidth - HandheldItemLayout.this.imagePaddingL) - HandheldItemLayout.this.imagePaddingL;
                    layoutParams.height = (HandheldItemLayout.this.mHeight - HandheldItemLayout.this.imagePaddingT) - HandheldItemLayout.this.imagePaddingT;
                    HandheldItemLayout.this.addView(view, layoutParams);
                    HandheldItemLayout.this.mContentView = view;
                }
            });
        }
    }

    private void reLayoutContentView() {
        View view = this.mContentView;
        if (view != null) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
            int i = this.mWidth;
            int i2 = this.imagePaddingL;
            layoutParams.width = (i - i2) - i2;
            int i3 = this.mHeight;
            int i4 = this.imagePaddingT;
            layoutParams.height = (i3 - i4) - i4;
            this.mContentView.setLayoutParams(layoutParams);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        this.mBorderView.draw(canvas);
        super.dispatchDraw(canvas);
    }

    public int getImagePaddingL() {
        return this.imagePaddingL;
    }

    public void setImagePaddingL(int i) {
        this.imagePaddingL = i;
        reLayoutContentView();
    }

    public int getImagePaddingT() {
        return this.imagePaddingT;
    }

    public void setImagePaddingT(int i) {
        this.imagePaddingT = i;
        reLayoutContentView();
    }

    public int getBorderWidth() {
        return this.borderWidth;
    }

    public void setBorderWidth(int i) {
        this.borderWidth = i;
        this.mBorderView.setBorderWidth(i);
        invalidate();
    }

    public float getBorderAlpha() {
        return this.borderAlpha;
    }

    public void setBorderAlpha(float f) {
        this.borderAlpha = f;
        this.mBorderView.setBorderAlpha(f);
        invalidate();
    }

    public void setSelect(boolean z, TextView textView, boolean z2) {
        this.mSelect = z;
        this.textView = textView;
        this.isExpand = z2;
        if (z) {
            removeCallbacks(this.pauseAnimRunable);
            removeCallbacks(this.selectAnimRunable);
            postDelayed(this.selectAnimRunable, 333L);
        } else {
            removeCallbacks(this.pauseAnimRunable);
            removeCallbacks(this.selectAnimRunable);
            postDelayed(this.pauseAnimRunable, 100L);
        }
    }

    public void setSquare(boolean z) {
        this.isSquare = z;
        this.mBorderView.isSquare = z;
    }
}

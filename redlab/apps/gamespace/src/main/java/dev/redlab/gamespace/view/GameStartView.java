package dev.redlab.gamespace.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

import dev.redlab.gamespace.R;
import dev.redlab.gamespace.anim.AnimBean;
import dev.redlab.gamespace.anim.AnimHelper;

/**
 * Owned port of the original {@code cn.nubia.gamelauncher.view.GameStartView}.
 *
 * <p>The original class differs only in asking {@code Controller.isPureMode()} whether to use the
 * pure-mode text size. That mode needs the Nubia controller, so this port stays on the normal
 * lobby shape: the original {@code game_start_button} layout, the original pill, the press scale
 * and the enter reveal. No layout file is changed.
 */
public final class GameStartView extends ConstraintLayout implements View.OnTouchListener {

    private static final float MIN_SCALE_VALUE = 0.8f;
    private static final float MAX_SCALE_VALUE = 1.0f;
    private static final int DOWN_SCALE_ANIM_DURATION = 200;
    private static final int UP_SCALE_ANIM_DURATION = 150;
    private static final int ENTER_DURATION = 300;

    private AnimatorSet mAnimatorSet;
    private ValueAnimator mPressAnimator;
    private ImageView mLeft;
    private ImageView mRight;
    private ImageView mStart;
    private MarqueeTextView mText;
    private float mValue = MAX_SCALE_VALUE;

    public GameStartView(Context context) {
        this(context, null);
    }

    public GameStartView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initChild(context);
    }

    private void initChild(Context context) {
        LayoutInflater.from(context).inflate(R.layout.game_start_button, this);
        mLeft = findViewById(R.id.game_start_left);
        mRight = findViewById(R.id.game_start_right);
        mText = findViewById(R.id.game_start_text);
        mStart = findViewById(R.id.game_start);
        mStart.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (!mStart.isEnabled()) {
            return false;
        }
        int action = motionEvent.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            doPressAnimator(MIN_SCALE_VALUE, DOWN_SCALE_ANIM_DURATION);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            doPressAnimator(MAX_SCALE_VALUE, UP_SCALE_ANIM_DURATION);
        }
        return false;
    }

    private void setState(float value) {
        mValue = value;
        setScaleX(value);
        setScaleY(value);
        setAlpha(value);
    }

    private void doPressAnimator(final float target, int duration) {
        if (mPressAnimator != null) {
            mPressAnimator.end();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(mValue, target);
        mPressAnimator = animator;
        animator.setDuration(duration);
        animator.start();
        animator.addUpdateListener(animation ->
                setState((Float) animation.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animator) {
                super.onAnimationCancel(animator);
                setState(MAX_SCALE_VALUE);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                super.onAnimationEnd(animator);
                setState(target);
            }
        });
    }

    public void cancelAnimator() {
        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility != VISIBLE) {
            cancelAnimator();
        }
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (mStart == null) {
            // View's constructor can reach this override before the layout is inflated.
            return;
        }
        mStart.setAlpha(alpha);
        if (alpha == 0f) {
            setClickable(false);
            mStart.setClickable(false);
            mStart.setVisibility(GONE);
        } else {
            mStart.setVisibility(VISIBLE);
            setClickable(true);
            mStart.setClickable(true);
        }
    }

    /** Reveals the label and the two side marks exactly as the original lobby entry does. */
    public void doEnterAnim() {
        cancelAnimator();
        AnimBean fade = new AnimBean(View.ALPHA, 0f, 1f);
        AnimBean fromLeft = new AnimBean(View.TRANSLATION_X, -55f, 0f);
        AnimBean fromRight = new AnimBean(View.TRANSLATION_X, 55f, 0f);
        AnimBean scaleX = new AnimBean(View.SCALE_X, 0.5f, 1f);
        AnimBean scaleY = new AnimBean(View.SCALE_Y, 0.5f, 1f);
        AnimBean textFade = new AnimBean(View.ALPHA, 0.2f, 1f);
        ObjectAnimator left = AnimHelper.createPropertyAnim(
                mLeft, ENTER_DURATION, fade, fromLeft, scaleX, scaleY);
        ObjectAnimator right = AnimHelper.createPropertyAnim(
                mRight, ENTER_DURATION, fade, fromRight, scaleX, scaleY);
        ObjectAnimator text = AnimHelper.createPropertyAnim(mText, ENTER_DURATION, textFade);
        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.play(left).with(right).with(text);
        mAnimatorSet.start();
        mAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                resetViewByAnimEnd();
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                resetViewByAnimEnd();
            }
        });
    }

    private void resetViewByAnimEnd() {
        mLeft.setAlpha(1f);
        mLeft.setTranslationX(0f);
        mLeft.setScaleX(1f);
        mLeft.setScaleY(1f);
        mText.setAlpha(1f);
        mRight.setAlpha(1f);
        mRight.setTranslationX(0f);
        mRight.setScaleX(1f);
        mRight.setScaleY(1f);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mStart.setEnabled(enabled);
    }
}

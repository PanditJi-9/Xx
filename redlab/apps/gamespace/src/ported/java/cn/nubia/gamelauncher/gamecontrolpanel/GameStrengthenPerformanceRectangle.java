package dev.redlab.gamespace.gamecontrolpanel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.gamecontrolpanel.utils.LogUtil;

/* loaded from: classes.dex */
public class GameStrengthenPerformanceRectangle extends LinearLayout {
    private static final String TAG = "dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceRectangle";
    private Handler mHandler;
    private boolean mIsReset;
    private int mPerformanceMode;
    private TextView vPerformanceMode;
    private View vRectangle1;
    private View vRectangle2;
    private View vRectangle3;
    private View vRectangle4;

    public GameStrengthenPerformanceRectangle(Context context) {
        this(context, null);
    }

    public GameStrengthenPerformanceRectangle(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GameStrengthenPerformanceRectangle(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPerformanceMode = -1;
        initView();
    }

    private void initView() {
        this.mHandler = new Handler();
        setOrientation(1);
        LayoutInflater.from(getContext()).inflate(R.layout.nubia_game_strengthen_performance_rectangle_view, this);
        this.vPerformanceMode = (TextView) findViewById(R.id.nubia_game_strengthen_performance_mode);
        this.vRectangle1 = findViewById(R.id.nubia_game_strengthen_performance_rectangle1);
        this.vRectangle2 = findViewById(R.id.nubia_game_strengthen_performance_rectangle2);
        this.vRectangle3 = findViewById(R.id.nubia_game_strengthen_performance_rectangle3);
        this.vRectangle4 = findViewById(R.id.nubia_game_strengthen_performance_rectangle4);
    }

    public void setPerformanceModeName(String str) {
        if (TextUtils.isEmpty(str)) {
            return;
        }
        this.vPerformanceMode.setText(str);
    }

    public void setPerformanceModeRectangleBg(int i, int i2, boolean z) {
        LogUtil.d(TAG, "setPerformanceModeRectangleBg:performanceMode=" + i2 + ",mPerformanceMode=" + this.mPerformanceMode);
        if (i == 0) {
            if (this.mPerformanceMode == 3) {
                startSuperToAutoRectangleAnim();
            } else {
                resetRectangleToAutoMode(i2, z);
            }
        } else {
            if (this.mPerformanceMode == 0 && ((i2 == 1 && i == 1) || ((i2 == 2 && i == 2) || i2 == 3))) {
                this.vRectangle1.setAlpha(0.3f);
                this.vRectangle2.setAlpha(0.3f);
                this.vRectangle3.setAlpha(0.3f);
            }
            View view = this.vRectangle1;
            int i3 = R.drawable.nubia_game_strengthen_performance_rectangle_blue_bg;
            view.setBackgroundResource(i == 1 ? R.drawable.nubia_game_strengthen_performance_rectangle_blue_bg : R.drawable.nubia_game_strengthen_performance_rectangle_red_bg);
            this.vRectangle2.setBackgroundResource(i == 1 ? R.drawable.nubia_game_strengthen_performance_rectangle_blue_bg : R.drawable.nubia_game_strengthen_performance_rectangle_red_bg);
            this.vRectangle3.setBackgroundResource(i == 1 ? R.drawable.nubia_game_strengthen_performance_rectangle_blue_bg : R.drawable.nubia_game_strengthen_performance_rectangle_red_bg);
            View view2 = this.vRectangle4;
            if (i != 1) {
                i3 = R.drawable.nubia_game_strengthen_performance_rectangle_red_bg;
            }
            view2.setBackgroundResource(i3);
        }
        this.mPerformanceMode = i2;
    }

    private void resetRectangleToAutoMode(int i, boolean z) {
        this.vRectangle1.setBackgroundResource(R.drawable.nubia_game_strengthen_performance_rectangle_color_bg1);
        this.vRectangle2.setBackgroundResource(R.drawable.nubia_game_strengthen_performance_rectangle_color_bg2);
        this.vRectangle3.setBackgroundResource(R.drawable.nubia_game_strengthen_performance_rectangle_color_bg3);
        this.vRectangle4.setBackgroundResource(R.drawable.nubia_game_strengthen_performance_rectangle_color_bg4);
        startGradientAnimFromBottomToTop(i, z);
    }

    private void startSuperToAutoRectangleAnim() {
        ValueAnimator ofFloat = ValueAnimator.ofFloat(1.0f, 0.5f, 1.0f);
        ofFloat.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceRectangle.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (valueAnimator.getCurrentPlayTime() > 150 && !GameStrengthenPerformanceRectangle.this.mIsReset) {
                    GameStrengthenPerformanceRectangle.this.vRectangle1.setBackgroundResource(R.drawable.nubia_game_strengthen_performance_rectangle_color_bg1);
                    GameStrengthenPerformanceRectangle.this.vRectangle2.setBackgroundResource(R.drawable.nubia_game_strengthen_performance_rectangle_color_bg2);
                    GameStrengthenPerformanceRectangle.this.vRectangle3.setBackgroundResource(R.drawable.nubia_game_strengthen_performance_rectangle_color_bg3);
                    GameStrengthenPerformanceRectangle.this.vRectangle4.setBackgroundResource(R.drawable.nubia_game_strengthen_performance_rectangle_color_bg4);
                    GameStrengthenPerformanceRectangle.this.mIsReset = true;
                }
                float floatValue = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                GameStrengthenPerformanceRectangle.this.vRectangle1.setAlpha(floatValue);
                GameStrengthenPerformanceRectangle.this.vRectangle2.setAlpha(floatValue);
                GameStrengthenPerformanceRectangle.this.vRectangle3.setAlpha(floatValue);
                GameStrengthenPerformanceRectangle.this.vRectangle4.setAlpha(floatValue);
            }
        });
        ofFloat.addListener(new AnimatorListenerAdapter() { // from class: dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceRectangle.2
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                GameStrengthenPerformanceRectangle.this.mIsReset = false;
            }
        });
        ofFloat.setDuration(300L);
        ofFloat.start();
    }

    public void startGradientAnimFromTopToBottom(int i, boolean z) {
        clearViewAnimation();
        setPerformanceRectangleViewAlpha(0.3f, i, z);
        if (this.vRectangle2.getAlpha() == 1.0f || z) {
            if (z || i == 0 || (i != 0 && this.vRectangle1.getAlpha() == 1.0f)) {
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.vRectangle1, AnimatorHelper.Item.ALPHA, 1.0f, 0.3f);
                ofFloat.setDuration(300L);
                ofFloat.start();
            }
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.vRectangle2, AnimatorHelper.Item.ALPHA, 1.0f, 0.3f);
            ofFloat2.setDuration(300L);
            ofFloat2.setStartDelay(100L);
            ofFloat2.start();
            ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(this.vRectangle3, AnimatorHelper.Item.ALPHA, 1.0f, 0.3f);
            ofFloat3.setDuration(300L);
            ofFloat3.setStartDelay(200L);
            ofFloat3.start();
        }
    }

    public void startGradientAnimFromBottomToTop(int i, boolean z) {
        clearViewAnimation();
        setPerformanceRectangleViewAlpha(1.0f, i, z);
        if (this.vRectangle2.getAlpha() != 1.0f || z) {
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.vRectangle3, AnimatorHelper.Item.ALPHA, 0.3f, 1.0f);
            ofFloat.setDuration(300L);
            ofFloat.start();
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.vRectangle2, AnimatorHelper.Item.ALPHA, 0.3f, 1.0f);
            ofFloat2.setDuration(300L);
            ofFloat2.setStartDelay(100L);
            ofFloat2.start();
            if (z || i == 0) {
                ObjectAnimator ofFloat3 = ObjectAnimator.ofFloat(this.vRectangle1, AnimatorHelper.Item.ALPHA, 0.3f, 1.0f);
                ofFloat3.setDuration(300L);
                ofFloat3.setStartDelay(200L);
                ofFloat3.start();
            }
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mPerformanceMode = -1;
        this.mHandler.removeCallbacksAndMessages(null);
        clearViewAnimation();
    }

    private void clearViewAnimation() {
        View view = this.vRectangle1;
        if (view != null) {
            view.clearAnimation();
        }
        View view2 = this.vRectangle2;
        if (view2 != null) {
            view2.clearAnimation();
        }
        View view3 = this.vRectangle3;
        if (view3 != null) {
            view3.clearAnimation();
        }
        View view4 = this.vRectangle4;
        if (view4 != null) {
            view4.clearAnimation();
        }
    }

    private void setPerformanceRectangleViewAlpha(final float f, final int i, final boolean z) {
        this.mHandler.removeCallbacksAndMessages(null);
        this.mHandler.postDelayed(new Runnable() { // from class: dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceRectangle.3
            @Override // java.lang.Runnable
            public void run() {
                GameStrengthenPerformanceRectangle.this.vRectangle2.setAlpha(f);
                GameStrengthenPerformanceRectangle.this.vRectangle3.setAlpha(f);
                if (z || i == 0) {
                    GameStrengthenPerformanceRectangle.this.vRectangle1.setAlpha(1.0f);
                } else {
                    GameStrengthenPerformanceRectangle.this.vRectangle1.setAlpha(0.3f);
                }
            }
        }, 500L);
    }
}

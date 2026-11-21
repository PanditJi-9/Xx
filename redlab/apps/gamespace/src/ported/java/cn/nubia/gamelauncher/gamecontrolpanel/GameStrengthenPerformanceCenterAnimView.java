package dev.redlab.gamespace.gamecontrolpanel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.gamecontrolpanel.utils.LogUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/* loaded from: classes.dex */
public class GameStrengthenPerformanceCenterAnimView extends FrameLayout {
    private static final String DB_GAMES_CHICKEN_MODE = "game_chicken_mode_switch";
    private static final int SWITCH_CLOSED_STATUS = 0;
    private static final int SWITCH_OPENED_STATUS = 2;
    private static final int SWITCH_OPENED_STATUS_OLD = 1;
    private static final String TAG = "GameStrengthenPerformanceCenterAnimView";
    private final ContentObserver mChickenModeChangeObserver;
    private ObjectAnimator mFanAcceDeceInterpolatorAnim;
    private ObjectAnimator mFanLinearInterpolatorAnim;
    private ValueAnimator mFogAnim;
    private ValueAnimator mFogAnim1;
    private ObjectAnimator mLogoAutoAnimBright;
    private ObjectAnimator mLogoAutoAnimDark;
    private ObjectAnimator mLogoLightAnim1;
    private ObjectAnimator mLogoLightAnim2;
    private int mPerformanceMode;
    private GameStrengthenPerformanceRectangle vCPUMode;
    private View vFogBlue;
    private View vFogBlue1;
    private View vFogRed;
    private View vFogRed1;
    private GameStrengthenPerformanceRectangle vGPUMode;
    private View vLogoAuto;
    private View vLogoLight1;
    private View vLogoLight2;
    private View vSuperModeFan;
    private View vSuperModeFanBg;

    public GameStrengthenPerformanceCenterAnimView(Context context) {
        this(context, null);
    }

    public GameStrengthenPerformanceCenterAnimView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public GameStrengthenPerformanceCenterAnimView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPerformanceMode = -1;
        this.mChickenModeChangeObserver = new ContentObserver(new Handler()) { // from class: dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceCenterAnimView.1
            @Override // android.database.ContentObserver
            public void onChange(boolean z) {
                GameStrengthenPerformanceCenterAnimView.this.startPerformanceModeAnim();
            }
        };
        initView();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor(DB_GAMES_CHICKEN_MODE), true, this.mChickenModeChangeObserver);
    }

    private void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.nubia_game_strengthen_performance_center_anim_view, this);
        this.vCPUMode = (GameStrengthenPerformanceRectangle) findViewById(R.id.nubia_game_strengthen_performance_animation_cpu);
        this.vGPUMode = (GameStrengthenPerformanceRectangle) findViewById(R.id.nubia_game_strengthen_performance_animation_gpu);
        this.vLogoAuto = findViewById(R.id.nubia_game_strengthen_performance_animation_auto_logo);
        this.vLogoLight1 = findViewById(R.id.nubia_game_strengthen_performance_animation_light_logo1);
        this.vLogoLight2 = findViewById(R.id.nubia_game_strengthen_performance_animation_light_logo2);
        this.vFogBlue = findViewById(R.id.nubia_game_strengthen_performance_animation_fog_blue);
        this.vFogBlue1 = findViewById(R.id.nubia_game_strengthen_performance_animation_fog_blue1);
        this.vFogRed = findViewById(R.id.nubia_game_strengthen_performance_animation_fog_red);
        this.vFogRed1 = findViewById(R.id.nubia_game_strengthen_performance_animation_fog_red1);
        this.vSuperModeFanBg = findViewById(R.id.nubia_game_strengthen_performance_super_mode_fan_bg);
        this.vSuperModeFan = findViewById(R.id.nubia_game_strengthen_performance_super_mode_fan);
        this.vCPUMode.setPerformanceModeName("CPU");
        this.vGPUMode.setPerformanceModeName("GPU");
        this.vSuperModeFanBg.setVisibility(checkIfSupportOverClockMode() ? 0 : 8);
    }

    public void startPerformanceModeAnim(int i) {
        LogUtil.d(TAG, "startPerformanceModeAnim:performanceMode=" + i + ",mPerformanceMode=" + this.mPerformanceMode);
        if (this.mPerformanceMode == i) {
            return;
        }
        this.mPerformanceMode = i;
        startPerformanceModeAnim();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startPerformanceModeAnim() {
        boolean enableGameChicken = enableGameChicken();
        if (enableGameChicken) {
            startLightLogoAnim();
            this.vGPUMode.setPerformanceModeRectangleBg(1, this.mPerformanceMode, enableGameChicken);
            this.vCPUMode.setPerformanceModeRectangleBg(2, this.mPerformanceMode, enableGameChicken);
            this.vGPUMode.startGradientAnimFromBottomToTop(this.mPerformanceMode, enableGameChicken);
            this.vCPUMode.startGradientAnimFromBottomToTop(this.mPerformanceMode, enableGameChicken);
        } else if (this.mPerformanceMode == 0) {
            startAutoLogoAnim();
            this.vCPUMode.setPerformanceModeRectangleBg(0, this.mPerformanceMode, enableGameChicken);
            this.vGPUMode.setPerformanceModeRectangleBg(0, this.mPerformanceMode, enableGameChicken);
        } else {
            startLightLogoAnim();
            this.vGPUMode.setPerformanceModeRectangleBg(1, this.mPerformanceMode, enableGameChicken);
            this.vCPUMode.setPerformanceModeRectangleBg(2, this.mPerformanceMode, enableGameChicken);
            int i = this.mPerformanceMode;
            if (i == 1) {
                this.vGPUMode.startGradientAnimFromBottomToTop(i, enableGameChicken);
                this.vCPUMode.startGradientAnimFromTopToBottom(this.mPerformanceMode, enableGameChicken);
            } else if (i == 2) {
                this.vCPUMode.startGradientAnimFromBottomToTop(i, enableGameChicken);
                this.vGPUMode.startGradientAnimFromTopToBottom(this.mPerformanceMode, enableGameChicken);
            } else if (i == 3) {
                this.vGPUMode.startGradientAnimFromBottomToTop(i, enableGameChicken);
                this.vCPUMode.startGradientAnimFromBottomToTop(this.mPerformanceMode, enableGameChicken);
            }
        }
        if (checkIfSupportOverClockMode()) {
            startOverClockModeAnim(this.mPerformanceMode);
        }
    }

    private void startFogAnim() {
        LogUtil.d(TAG, "startFogAnim");
        if (this.mFogAnim == null) {
            this.mFogAnim = createPerformanceFogAnim(this.vFogRed, this.vFogBlue);
        }
        if (this.mFogAnim1 == null) {
            this.mFogAnim1 = createPerformanceFogAnim(this.vFogRed1, this.vFogBlue1);
        }
        if (!this.mFogAnim.isRunning()) {
            ObjectAnimator.ofFloat(this.vFogRed, AnimatorHelper.Item.ALPHA, 0.0f, 1.0f).setDuration(500L).start();
            ObjectAnimator.ofFloat(this.vFogBlue, AnimatorHelper.Item.ALPHA, 0.0f, 1.0f).setDuration(500L).start();
            LogUtil.d(TAG, "mFogAnim start");
            this.mFogAnim.start();
        }
        if (this.mFogAnim1.isRunning()) {
            return;
        }
        this.mFogAnim1.setStartDelay(1000L);
        this.vFogBlue1.setAlpha(1.0f);
        this.vFogRed1.setAlpha(1.0f);
        this.mFogAnim1.start();
        LogUtil.d(TAG, "mFogAnim1 start");
    }

    private ValueAnimator createPerformanceFogAnim(final View view, final View view2) {
        ValueAnimator ofInt = ValueAnimator.ofInt(0, 1);
        ofInt.setRepeatCount(-1);
        ofInt.setDuration(2000L);
        ofInt.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceCenterAnimView.2
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (GameStrengthenPerformanceCenterAnimView.this.mPerformanceMode != 0) {
                    float animatedFraction = valueAnimator.getAnimatedFraction();
                    View view3 = view;
                    GameStrengthenPerformanceCenterAnimView gameStrengthenPerformanceCenterAnimView = GameStrengthenPerformanceCenterAnimView.this;
                    view3.setTranslationX((-animatedFraction) * gameStrengthenPerformanceCenterAnimView.dp2px(gameStrengthenPerformanceCenterAnimView.getContext(), 112.0f));
                    View view4 = view2;
                    GameStrengthenPerformanceCenterAnimView gameStrengthenPerformanceCenterAnimView2 = GameStrengthenPerformanceCenterAnimView.this;
                    view4.setTranslationX(animatedFraction * gameStrengthenPerformanceCenterAnimView2.dp2px(gameStrengthenPerformanceCenterAnimView2.getContext(), 112.0f));
                }
            }
        });
        return ofInt;
    }

    private void stopFogAnim() {
        cancelFogAnim(this.mFogAnim, this.vFogRed);
        cancelFogAnim(this.mFogAnim, this.vFogBlue);
        cancelFogAnim(this.mFogAnim1, this.vFogRed1);
        cancelFogAnim(this.mFogAnim1, this.vFogBlue1);
    }

    private void startAutoLogoAnim() {
        stopFogAnim();
        if (checkIfSupportOverClockMode()) {
            return;
        }
        this.vLogoLight1.setAlpha(0.0f);
        this.vLogoLight1.setTranslationY(0.0f);
        this.vLogoLight2.setAlpha(0.0f);
        this.vLogoLight2.setTranslationY(0.0f);
        this.vLogoAuto.setAlpha(0.0f);
        cancelAnim(this.mLogoLightAnim1, this.vLogoLight1);
        cancelAnim(this.mLogoLightAnim2, this.vLogoLight2);
        if (this.mLogoAutoAnimBright == null) {
            View view = this.vLogoAuto;
            ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.ALPHA, view.getAlpha(), 1.0f);
            this.mLogoAutoAnimBright = ofFloat;
            ofFloat.setDuration(300L);
            this.mLogoAutoAnimBright.addListener(new AnimatorListenerAdapter() { // from class: dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceCenterAnimView.3
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    if (GameStrengthenPerformanceCenterAnimView.this.mPerformanceMode == 0) {
                        GameStrengthenPerformanceCenterAnimView.this.mLogoAutoAnimDark.setStartDelay(200L);
                        GameStrengthenPerformanceCenterAnimView.this.mLogoAutoAnimDark.start();
                    }
                }
            });
        }
        if (this.mLogoAutoAnimDark == null) {
            ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.vLogoAuto, AnimatorHelper.Item.ALPHA, 1.0f, 0.15f);
            this.mLogoAutoAnimDark = ofFloat2;
            ofFloat2.setDuration(500L);
            this.mLogoAutoAnimDark.addListener(new AnimatorListenerAdapter() { // from class: dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceCenterAnimView.4
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animator) {
                    if (GameStrengthenPerformanceCenterAnimView.this.mPerformanceMode == 0) {
                        GameStrengthenPerformanceCenterAnimView.this.mLogoAutoAnimBright.start();
                    }
                }
            });
        }
        if (this.mLogoAutoAnimBright.isRunning()) {
            return;
        }
        this.mLogoAutoAnimBright.start();
    }

    private void startLightLogoAnim() {
        startFogAnim();
        if (checkIfSupportOverClockMode()) {
            return;
        }
        this.vLogoAuto.setAlpha(0.0f);
        this.vLogoLight1.setAlpha(1.0f);
        cancelAnim(this.mLogoAutoAnimBright, this.vLogoAuto);
        cancelAnim(this.mLogoAutoAnimDark, this.vLogoAuto);
        if (this.mLogoLightAnim1 == null) {
            this.mLogoLightAnim1 = createLogoLightAnim(this.vLogoLight1);
        }
        if (this.mLogoLightAnim2 == null) {
            this.mLogoLightAnim2 = createLogoLightAnim(this.vLogoLight2);
        }
        if (!this.mLogoLightAnim1.isRunning()) {
            this.mLogoLightAnim1.start();
        }
        if (this.mLogoLightAnim2.isRunning()) {
            return;
        }
        this.mLogoLightAnim2.setStartDelay(500L);
        this.vLogoLight2.setAlpha(1.0f);
        this.mLogoLightAnim2.start();
    }

    private ObjectAnimator createLogoLightAnim(View view) {
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(view, AnimatorHelper.Item.TRANSLATIONY, 0.0f, dp2px(getContext(), 55.0f));
        ofFloat.setDuration(1000L);
        ofFloat.setRepeatCount(-1);
        return ofFloat;
    }

    private void startOverClockModeAnim(int i) {
        if (i == 0 || i == 2) {
            cancelAnim(this.mFanAcceDeceInterpolatorAnim, this.vSuperModeFan);
            if (this.mFanLinearInterpolatorAnim == null) {
                ObjectAnimator duration = ObjectAnimator.ofFloat(this.vSuperModeFan, AnimatorHelper.Item.ROTATE, 0.0f, 360.0f).setDuration(1000L);
                this.mFanLinearInterpolatorAnim = duration;
                duration.setRepeatCount(-1);
                this.mFanLinearInterpolatorAnim.setInterpolator(new LinearInterpolator());
            }
            if (this.mFanLinearInterpolatorAnim.isRunning()) {
                return;
            }
            this.mFanLinearInterpolatorAnim.start();
            return;
        }
        cancelAnim(this.mFanLinearInterpolatorAnim, this.vSuperModeFan);
        if (this.mFanAcceDeceInterpolatorAnim == null) {
            ObjectAnimator duration2 = ObjectAnimator.ofFloat(this.vSuperModeFan, AnimatorHelper.Item.ROTATE, 0.0f, 720.0f).setDuration(1000L);
            this.mFanAcceDeceInterpolatorAnim = duration2;
            duration2.setRepeatCount(-1);
            this.mFanAcceDeceInterpolatorAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        }
        if (this.mFanAcceDeceInterpolatorAnim.isRunning()) {
            return;
        }
        this.mFanAcceDeceInterpolatorAnim.start();
    }

    private void cancelAnim(ObjectAnimator objectAnimator, View view) {
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        if (view != null) {
            view.clearAnimation();
        }
    }

    private void cancelFogAnim(ValueAnimator valueAnimator, View view) {
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        if (view != null) {
            view.clearAnimation();
            view.setTranslationX(0.0f);
            view.setAlpha(0.0f);
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        getContext().getContentResolver().unregisterContentObserver(this.mChickenModeChangeObserver);
        this.mPerformanceMode = -1;
        super.onDetachedFromWindow();
        cancelAnim(this.mLogoLightAnim1, this.vLogoLight1);
        cancelAnim(this.mLogoLightAnim2, this.vLogoLight2);
        cancelAnim(this.mLogoAutoAnimBright, this.vLogoAuto);
        cancelAnim(this.mLogoAutoAnimDark, this.vLogoAuto);
        cancelFogAnim(this.mFogAnim, this.vFogBlue);
        cancelFogAnim(this.mFogAnim, this.vFogRed);
        cancelFogAnim(this.mFogAnim1, this.vFogBlue1);
        cancelFogAnim(this.mFogAnim1, this.vFogRed1);
        cancelAnim(this.mFanLinearInterpolatorAnim, this.vSuperModeFan);
        cancelAnim(this.mFanAcceDeceInterpolatorAnim, this.vSuperModeFan);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp2px(Context context, float f) {
        return (int) ((f * context.getResources().getDisplayMetrics().density) + 0.5f);
    }

    private boolean checkIfSupportOverClockMode() {
        try {
            if (Settings.Global.getInt(getContext().getContentResolver(), "overclocking_mode_switch", 0) == 1) {
                return checkIfConnectedDock2();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkIfConnectedDock2() {
        String readOverClockNode = readOverClockNode();
        return !TextUtils.isEmpty(readOverClockNode) && readOverClockNode.equals("1");
    }

    private String readOverClockNode() {
        BufferedReader bufferedReader;
        Throwable th;
        FileReader fileReader;
        try {
            fileReader = new FileReader(new File("sys/kernel/usb_enhance/dock2_detection"));
            try {
                bufferedReader = new BufferedReader(fileReader);
            } catch (Exception unused) {
                bufferedReader = null;
            } catch (Throwable th2) {
                bufferedReader = null;
                th = th2;
            }
        } catch (Exception unused2) {
            fileReader = null;
            bufferedReader = null;
        } catch (Throwable th3) {
            bufferedReader = null;
            th = th3;
            fileReader = null;
        }
        try {
            String readLine = bufferedReader.readLine();
            if (readLine != null) {
                try {
                    fileReader.close();
                    bufferedReader.close();
                } catch (Exception unused3) {
                }
                return readLine;
            }
            try {
                fileReader.close();
                bufferedReader.close();
            } catch (Exception unused4) {
            }
            return readLine;
        } catch (Exception unused5) {
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (Exception unused6) {
                    return null;
                }
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            return null;
        } catch (Throwable th4) {
            th = th4;
            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (Exception unused7) {
                    throw th;
                }
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            throw th;
        }
    }

    private boolean enableGameChicken() {
        return Settings.Global.getInt(getContext().getContentResolver(), DB_GAMES_CHICKEN_MODE, 0) != 0;
    }
}

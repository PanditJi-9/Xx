package dev.redlab.gamespace.xgravitation.util;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import dev.redlab.gamespace.xgravitation.IController;
import java.lang.ref.WeakReference;

/* loaded from: classes2.dex */
public class InterUIController {
    public static final int PROGRESSBAR_MAX = 100;
    private static final String TAG = "InterUIController";
    public static final int UPDATE_UI_MSG = 1001;
    private static InterUIController mInstance;
    private static UIHandler mUIHandler;
    private IController mIController;
    private WeakReference<Context> mWeakReferenceContext;
    private static final Long UPDATE_UI_DELAY_TIME = 6000L;
    private static final Long TIME_INTERNAL = 10L;
    private boolean mIsForceStopFlag = false;
    private int mCurrentProgress = 0;
    private CountDownTimer mInterUpdateUITimer = new CountDownTimer(UPDATE_UI_DELAY_TIME.longValue(), TIME_INTERNAL.longValue()) { // from class: dev.redlab.gamespace.xgravitation.util.InterUIController.1
        @Override // android.os.CountDownTimer
        public void onTick(long j) {
            if (InterUIController.this.mIsForceStopFlag) {
                return;
            }
            InterUIController interUIController = InterUIController.this;
            interUIController.mCurrentProgress = interUIController.getProgress(InterUIController.UPDATE_UI_DELAY_TIME.longValue(), j);
            if (InterUIController.this.mIController != null) {
                InterUIController.this.mIController.updateProgress(InterUIController.this.mCurrentProgress);
            }
        }

        @Override // android.os.CountDownTimer
        public void onFinish() {
            LogUtils.d(InterUIController.TAG, "onFinish: ");
            InterUIController.this.mIsForceStopFlag = true;
            if (InterUIController.this.mIController != null) {
                InterUIController.this.mIController.setProgressMax(100);
            }
        }
    };

    public InterUIController(Context context) {
        this.mWeakReferenceContext = new WeakReference<>(context);
    }

    public static InterUIController getInstance(Context context, Looper looper) {
        if (mInstance == null) {
            mInstance = new InterUIController(context);
        }
        if (mUIHandler == null) {
            mUIHandler = new UIHandler(looper);
        }
        return mInstance;
    }

    public void setIController(IController iController) {
        this.mIController = iController;
        iController.setProgressMax(100);
        mUIHandler.setIController(this.mIController);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int getProgress(long j, long j2) {
        return Math.round(((j - j2) / j) * 100.0f);
    }

    static class UIHandler extends Handler {
        IController mIController;

        public UIHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            IController iController;
            super.handleMessage(message);
            if (message.what == 1001 && (iController = this.mIController) != null) {
                iController.updateUI();
            }
        }

        public void setIController(IController iController) {
            this.mIController = iController;
        }

        public void release() {
            removeMessages(1001);
            this.mIController = null;
        }
    }

    public void stop() {
        LogUtils.d(TAG, "stop: ");
        UIHandler uIHandler = mUIHandler;
        if (uIHandler != null) {
            uIHandler.removeMessages(1001);
        }
        CountDownTimer countDownTimer = this.mInterUpdateUITimer;
        if (countDownTimer != null) {
            this.mIsForceStopFlag = true;
            countDownTimer.cancel();
        }
    }

    public void restart() {
        LogUtils.d(TAG, "restart: ");
        CountDownTimer countDownTimer = this.mInterUpdateUITimer;
        if (countDownTimer != null) {
            countDownTimer.cancel();
            this.mIsForceStopFlag = false;
            this.mInterUpdateUITimer.start();
        }
        UIHandler uIHandler = mUIHandler;
        if (uIHandler != null) {
            uIHandler.removeMessages(1001);
            mUIHandler.sendEmptyMessageDelayed(1001, UPDATE_UI_DELAY_TIME.longValue());
        }
    }

    public void release() {
        LogUtils.d(TAG, "release: ");
        UIHandler uIHandler = mUIHandler;
        if (uIHandler != null) {
            uIHandler.release();
            mUIHandler = null;
        }
        CountDownTimer countDownTimer = this.mInterUpdateUITimer;
        if (countDownTimer != null) {
            this.mIsForceStopFlag = true;
            countDownTimer.cancel();
        }
        this.mIController = null;
    }

    public void start() {
        LogUtils.d(TAG, "start: ");
        if (mUIHandler != null) {
            CountDownTimer countDownTimer = this.mInterUpdateUITimer;
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            mUIHandler.removeMessages(1001);
            mUIHandler.sendEmptyMessage(1001);
        }
    }
}

package com.zte.gameassist.ai;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;
import com.zte.gameassist.ai.AIFlickerTips;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/* loaded from: classes2.dex */
public class AIFlickerTips {
    public static final int FLICKER_ID = 539234819;
    public static final int FLICKER_VIEW_ID = 539234820;
    public static final int NAME_ID = 539234817;
    public static final int PADDING_ID = 539234818;
    public static final String TAG = "AIFlickerTips_0.0520";
    private static volatile AIFlickerTips instance = null;
    private static final int mFillFlickerAlpha = 51;
    private static final int mFillFlickerColor = 15547195;
    private static final int mStrokeFlickerAlpha = 102;
    private static final int mStrokeFlickerColor = 15547195;
    private final Paint mPaint;
    public static boolean DEBUG = "userdebug".equals(Build.TYPE);
    private static final PathInterpolator mInterpolator = new PathInterpolator(0.33f, 0.0f, 0.67f, 1.0f);
    private final Map<String, List<Flicker>> mFlickerMap = new ArrayMap();
    private final List<String> mPropNames = new ArrayList();
    private final List<View> mFlickerViews = new ArrayList();
    private final ThreadLocal<Handler> mHandlers = new ThreadLocal<>();
    private final float mStrokeWidth = 1.0f;
    private final Runnable mDelayShowFlicker = new Runnable() { // from class: com.zte.gameassist.ai.AIFlickerTips$$ExternalSyntheticLambda3
        @Override // java.lang.Runnable
        public final void run() {
            AIFlickerTips.this.m453lambda$new$4$comztegameassistaiAIFlickerTips();
        }
    };

    private AIFlickerTips() {
        Paint paint = new Paint();
        this.mPaint = paint;
        paint.setColor(-1);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);
    }

    public static AIFlickerTips getInstance() {
        if (instance == null) {
            synchronized (AIFlickerTips.class) {
                if (instance == null) {
                    instance = new AIFlickerTips();
                }
            }
        }
        return instance;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void logi(String str) {
        Log.i(TAG, str);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void logv(String str) {
        if (DEBUG) {
            logi(str);
        } else {
            Log.v(TAG, str);
        }
    }

    private static void logv(Supplier<String> supplier) {
        if (DEBUG) {
            logi(supplier.get());
        } else {
            Log.v(TAG, supplier.get());
        }
    }

    public boolean hasFlickerName(String str) {
        return this.mPropNames.contains(str);
    }

    public List<String> getNames() {
        return new ArrayList(this.mPropNames);
    }

    private static float dpToPix(Context context, float f) {
        return TypedValue.applyDimension(1, f, context.getResources().getDisplayMetrics());
    }

    public void addFlickerView(String str, final View view) {
        if (this.mPaint.getStrokeWidth() == 0.0f) {
            this.mPaint.setStrokeWidth(dpToPix(view.getContext(), 1.0f));
        }
        this.mFlickerMap.forEach(new BiConsumer() { // from class: com.zte.gameassist.ai.AIFlickerTips$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                AIFlickerTips.lambda$addFlickerView$0(view, (String) obj, (List) obj2);
            }
        });
        List<Flicker> list = this.mFlickerMap.get(str);
        if (list == null) {
            list = new ArrayList<>();
            this.mFlickerMap.put(str, list);
        }
        Flicker flicker = new Flicker(str, view);
        if (list.contains(flicker)) {
            return;
        }
        list.add(flicker);
    }

    static /* synthetic */ void lambda$addFlickerView$0(View view, String str, List list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (int size = list.size() - 1; size >= 0; size--) {
            Flicker flicker = (Flicker) list.get(size);
            if (flicker.mTargetView == view) {
                flicker.release();
            }
        }
    }

    public static void setFlickerName(View view, String str) {
        if (view == null || str == null) {
            return;
        }
        getInstance().addFlickerView(str, view);
        if (getInstance().hasFlickerName(str)) {
            logv("setFlickerName view=" + view.getClass().getSimpleName() + " name=" + str + "@" + Integer.toHexString(view.hashCode()));
            showFlicker(str);
        }
    }

    public static void setFlickerPaddingDP(View view, float f, float f2, float f3, float f4) {
        Context context = view.getContext();
        setFlickerPadding(view, new RectF(dpToPix(context, f), dpToPix(context, f2), dpToPix(context, f3), dpToPix(context, f4)));
    }

    public static void setFlickerPadding(View view, int i, int i2, int i3, int i4) {
        setFlickerPadding(view, new RectF(i, i2, i3, i4));
    }

    public static void setFlickerPadding(View view, Rect rect) {
        setFlickerPadding(view, new RectF(rect));
    }

    public static void setFlickerPadding(View view, RectF rectF) {
        view.setTag(PADDING_ID, rectF);
    }

    public static void hideAllFlicker() {
        getInstance().release();
    }

    public static void hideFlicker(String str) {
        getInstance().hideFlickerView(str);
    }

    public static void showFlicker(String str) {
        showFlicker(str, 0L);
    }

    public static void showFlicker(String str, long j) {
        getInstance().showFlickerView(str, j);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void release() {
        final int sum = this.mFlickerMap.values().stream().mapToInt(new ToIntFunction() { // from class: com.zte.gameassist.ai.AIFlickerTips$$ExternalSyntheticLambda5
            @Override // java.util.function.ToIntFunction
            public final int applyAsInt(Object obj) {
                int size;
                size = ((List) obj).size();
                return size;
            }
        }).sum();
        logi("release = " + this.mPropNames + " flickers count =" + sum);
        this.mPropNames.clear();
        this.mFlickerMap.forEach(new BiConsumer() { // from class: com.zte.gameassist.ai.AIFlickerTips$$ExternalSyntheticLambda6
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                AIFlickerTips.lambda$release$2(sum, (String) obj, (List) obj2);
            }
        });
        for (int size = this.mFlickerViews.size() - 1; size >= 0; size--) {
            View remove = this.mFlickerViews.remove(size);
            if (remove != null) {
                ViewGroup viewGroup = (ViewGroup) remove.getParent();
                if (viewGroup == null) {
                    return;
                }
                viewGroup.removeView(remove);
                viewGroup.setTag(FLICKER_VIEW_ID, null);
            }
        }
    }

    static /* synthetic */ void lambda$release$2(int i, String str, List list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        for (int size = list.size() - 1; size >= 0; size--) {
            ((Flicker) list.get(size)).release(i < 5);
        }
    }

    public void hideFlickerView(String str) {
        logi("hideFlickerView name=" + str);
        if (this.mPropNames.contains(str)) {
            this.mPropNames.remove(str);
        }
    }

    /* renamed from: lambda$new$4$com-zte-gameassist-ai-AIFlickerTips, reason: not valid java name */
    /* synthetic */ void m453lambda$new$4$comztegameassistaiAIFlickerTips() {
        this.mPropNames.forEach(new Consumer() { // from class: com.zte.gameassist.ai.AIFlickerTips$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AIFlickerTips.this.m452lambda$new$3$comztegameassistaiAIFlickerTips((String) obj);
            }
        });
    }

    /* renamed from: lambda$new$3$com-zte-gameassist-ai-AIFlickerTips, reason: not valid java name */
    /* synthetic */ void m452lambda$new$3$comztegameassistaiAIFlickerTips(String str) {
        List<Flicker> list = this.mFlickerMap.get(str);
        if (list != null) {
            list.forEach(new Consumer() { // from class: com.zte.gameassist.ai.AIFlickerTips$$ExternalSyntheticLambda7
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AIFlickerTips.Flicker) obj).showAnimation();
                }
            });
        }
    }

    public void showFlickerView(String str, long j) {
        if (!this.mPropNames.contains(str)) {
            this.mPropNames.add(str);
        }
        getHandler().removeCallbacks(this.mDelayShowFlicker);
        getHandler().postDelayed(this.mDelayShowFlicker, 100L);
    }

    public void showFlickerView(Flicker flicker, long j) {
        if (flicker.mTargetView.isAttachedToWindow()) {
            flicker.onViewAttachedToWindow(flicker.mTargetView);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void drawFlickerTips(final Canvas canvas) {
        this.mPropNames.forEach(new Consumer() { // from class: com.zte.gameassist.ai.AIFlickerTips$$ExternalSyntheticLambda4
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                AIFlickerTips.this.m451lambda$drawFlickerTips$6$comztegameassistaiAIFlickerTips(canvas, (String) obj);
            }
        });
    }

    /* renamed from: lambda$drawFlickerTips$6$com-zte-gameassist-ai-AIFlickerTips, reason: not valid java name */
    /* synthetic */ void m451lambda$drawFlickerTips$6$comztegameassistaiAIFlickerTips(final Canvas canvas, String str) {
        List<Flicker> list = this.mFlickerMap.get(str);
        if (list == null || list.size() == 0) {
            logi(str + " filckers is null");
        } else {
            this.mFlickerMap.get(str).forEach(new Consumer() { // from class: com.zte.gameassist.ai.AIFlickerTips$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((AIFlickerTips.Flicker) obj).draw(canvas);
                }
            });
        }
    }

    private class FlickerView extends View implements View.OnLayoutChangeListener {
        private final CheckFlicker mCheckFlicker;
        private boolean mIsFingerTouch;

        public FlickerView(Context context) {
            super(context);
            this.mCheckFlicker = new CheckFlicker();
        }

        @Override // android.view.View
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            getRootView().addOnLayoutChangeListener(this);
        }

        @Override // android.view.View
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            getRootView().removeOnLayoutChangeListener(this);
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            AIFlickerTips.getInstance().drawFlickerTips(canvas);
            if (!this.mIsFingerTouch || AIFlickerTips.this.mPropNames.isEmpty()) {
                return;
            }
            postInvalidate();
        }

        private class CheckFlicker implements Runnable {
            private MotionEvent mMotionEvent;

            private CheckFlicker() {
            }

            public void check(MotionEvent motionEvent) {
                MotionEvent motionEvent2 = this.mMotionEvent;
                if (motionEvent2 != null) {
                    motionEvent2.recycle();
                    this.mMotionEvent = null;
                }
                this.mMotionEvent = MotionEvent.obtain(motionEvent);
                AIFlickerTips.this.getHandler().removeCallbacks(this);
                AIFlickerTips.this.getHandler().postDelayed(this, 20L);
            }

            @Override // java.lang.Runnable
            public void run() {
                if (this.mMotionEvent == null) {
                    return;
                }
                try {
                    Iterator it = AIFlickerTips.this.mPropNames.iterator();
                    long j = 0;
                    while (it.hasNext()) {
                        List<Flicker> list = (List) AIFlickerTips.this.mFlickerMap.get((String) it.next());
                        if (list != null && list.size() > 0) {
                            for (Flicker flicker : list) {
                                if (flicker.allowRelease(this.mMotionEvent)) {
                                    AIFlickerTips.this.release();
                                    return;
                                } else if (flicker.mAnimator != null && flicker.mAnimator.isRunning()) {
                                    j++;
                                }
                            }
                        }
                    }
                    if (j == 0 && AIFlickerTips.this.mPropNames.size() > 0) {
                        AIFlickerTips.logi("allowRelease when no ani");
                        AIFlickerTips.this.release();
                    }
                } finally {
                    this.mMotionEvent.recycle();
                    this.mMotionEvent = null;
                }
            }
        }

        @Override // android.view.View
        public boolean dispatchTouchEvent(MotionEvent motionEvent) {
            if (motionEvent.getActionMasked() == 0) {
                this.mIsFingerTouch = true;
                this.mCheckFlicker.check(motionEvent);
            } else if (motionEvent.getAction() == 1 || motionEvent.getAction() == 3) {
                this.mIsFingerTouch = false;
            }
            return super.dispatchTouchEvent(motionEvent);
        }

        @Override // android.view.View.OnLayoutChangeListener
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            layout(i, i2, i3, i4);
        }
    }

    public static boolean isVisible(View view) {
        if (view == null || view.getVisibility() != 0 || view.getParent() == null) {
            return false;
        }
        if (view.getParent() instanceof View) {
            return isVisible((View) view.getParent());
        }
        return view.getVisibility() == 0;
    }

    public class Flicker implements View.OnAttachStateChangeListener {
        private boolean isAddAttachStateListener;
        private boolean isAttachedToWindow;
        private String mAnimationLog;
        private ValueAnimator mAnimator;
        private final Runnable mDelayDetachedFromWindow = new Runnable() { // from class: com.zte.gameassist.ai.AIFlickerTips$Flicker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AIFlickerTips.Flicker.this.release();
            }
        };
        private String mDrawLog;
        private View mFlickerView;
        private final String mName;
        private final ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
        private final View mTargetView;
        private long updateDrawTime;

        public boolean equals(Object obj) {
            boolean z;
            if (obj == null || !((z = obj instanceof Flicker))) {
                return false;
            }
            if (z) {
                return ((Flicker) obj).mTargetView == this.mTargetView;
            }
            return super.equals(obj);
        }

        public String hashName() {
            return this.mName + "@" + Integer.toHexString(this.mTargetView.hashCode());
        }

        public Flicker(String str, View view) {
            this.mName = str;
            this.mTargetView = view;
            view.setTag(AIFlickerTips.NAME_ID, str);
            view.setTag(AIFlickerTips.FLICKER_ID, this);
            this.mOnPreDrawListener = new AnonymousClass1(AIFlickerTips.this, view);
            if (this.isAddAttachStateListener) {
                return;
            }
            view.addOnAttachStateChangeListener(this);
            this.isAddAttachStateListener = true;
        }

        /* renamed from: com.zte.gameassist.ai.AIFlickerTips$Flicker$1, reason: invalid class name */
        class AnonymousClass1 implements ViewTreeObserver.OnPreDrawListener {
            private long updateAnimationTime;
            final /* synthetic */ AIFlickerTips val$this$0;
            final /* synthetic */ View val$view;

            AnonymousClass1(AIFlickerTips aIFlickerTips, View view) {
                this.val$this$0 = aIFlickerTips;
                this.val$view = view;
            }

            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public boolean onPreDraw() {
                String str = Flicker.this.hashName() + " onPreDraw";
                this.val$view.getViewTreeObserver().removeOnPreDrawListener(this);
                if (Flicker.this.mAnimator == null && TextUtils.equals(Flicker.this.getName(), Flicker.this.mName)) {
                    Flicker.this.mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f);
                    Flicker.this.mAnimator.setDuration(1000L);
                    Flicker.this.mAnimator.setInterpolator(AIFlickerTips.mInterpolator);
                    Flicker.this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.zte.gameassist.ai.AIFlickerTips$Flicker$1$$ExternalSyntheticLambda0
                        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                        public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                            AIFlickerTips.Flicker.AnonymousClass1.this.onFlickerAnimationUpdate(valueAnimator);
                        }
                    });
                    ValueAnimator valueAnimator = Flicker.this.mAnimator;
                    final Flicker flicker = Flicker.this;
                    valueAnimator.addListener(new AnimatorListener(null, new Runnable() { // from class: com.zte.gameassist.ai.AIFlickerTips$Flicker$1$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            AIFlickerTips.Flicker.this.animatorEnd();
                        }
                    }));
                    Flicker.this.mAnimator.start();
                    str = str + ", start animator " + Integer.toHexString(Flicker.this.hashCode()) + " " + Flicker.this.mTargetView;
                }
                AIFlickerTips.logi(str);
                return true;
            }

            /* JADX INFO: Access modifiers changed from: private */
            public void onFlickerAnimationUpdate(ValueAnimator valueAnimator) {
                ViewGroup viewGroup = (ViewGroup) Flicker.this.mTargetView.getRootView().getRootView();
                String name = Flicker.this.getName();
                if (viewGroup == null || !TextUtils.equals(name, Flicker.this.mName)) {
                    return;
                }
                int[] iArr = new int[2];
                Flicker.this.mTargetView.getLocationInWindow(iArr);
                String str = "onFlickerAnimationUpdate mName=" + Flicker.this.hashName() + " attach=" + Flicker.this.mTargetView.isAttachedToWindow() + " vis=" + AIFlickerTips.isVisible(Flicker.this.mTargetView) + " p=" + iArr[0] + "," + iArr[1] + " : " + Flicker.this.mFlickerView.getWidth() + "," + Flicker.this.mFlickerView.getHeight() + " f=" + Flicker.this.mFlickerView.isAttachedToWindow();
                if (!TextUtils.equals(str, Flicker.this.mAnimationLog) && System.currentTimeMillis() - this.updateAnimationTime >= 250) {
                    this.updateAnimationTime = System.currentTimeMillis();
                    Flicker.this.mAnimationLog = str;
                    AIFlickerTips.logi(Flicker.this.mAnimationLog + ", v=" + Flicker.this.mTargetView);
                }
                if (Flicker.this.mFlickerView != null) {
                    Flicker.this.mFlickerView.postInvalidate();
                }
                List list = (List) AIFlickerTips.this.mFlickerMap.get(Flicker.this.mName);
                if (list == null || list.contains(Flicker.this) || !Flicker.this.mTargetView.isAttachedToWindow() || !AIFlickerTips.isVisible(Flicker.this.mTargetView)) {
                    return;
                }
                if (!Flicker.this.isAddAttachStateListener) {
                    Flicker.this.mTargetView.addOnAttachStateChangeListener(Flicker.this);
                    Flicker.this.isAddAttachStateListener = true;
                }
                ((List) AIFlickerTips.this.mFlickerMap.get(Flicker.this.mName)).add(Flicker.this);
                AIFlickerTips.logi("release, but add tower. " + Flicker.this.hashName());
            }
        }

        public String getName() {
            View view = this.mTargetView;
            if (view == null) {
                return "null";
            }
            Object tag = view.getTag(AIFlickerTips.NAME_ID);
            return tag instanceof String ? (String) tag : "null";
        }

        private RectF getTargetViewBounds() {
            View view = this.mTargetView;
            if (view == null || !AIFlickerTips.isVisible(view) || !AIFlickerTips.this.hasFlickerName(getName()) || !this.mTargetView.isAttachedToWindow()) {
                return new RectF();
            }
            Rect rect = new Rect();
            Object tag = this.mTargetView.getTag(AIFlickerTips.PADDING_ID);
            if (tag instanceof Rect) {
                rect.set((Rect) tag);
            }
            int[] r1 = new int[2];
            this.mTargetView.getLocationInWindow(r1);
            return new RectF(r1[0] + rect.left, r1[1] + rect.top, (r1[0] + this.mTargetView.getWidth()) - rect.right, (r1[1] + this.mTargetView.getHeight()) - rect.bottom);
        }

        public boolean allowRelease(MotionEvent motionEvent) {
            StringBuilder sb = new StringBuilder();
            try {
                ValueAnimator valueAnimator = this.mAnimator;
                boolean z = valueAnimator != null && valueAnimator.isRunning();
                sb.append("ani=" + z);
                if (z) {
                    RectF targetViewBounds = getTargetViewBounds();
                    sb.append(" bounds=" + targetViewBounds);
                    if (targetViewBounds.contains(motionEvent.getX(), motionEvent.getY())) {
                        sb.append(" allow");
                        return true;
                    }
                }
                sb.append(" unallow");
                return false;
            } finally {
                AIFlickerTips.logi("allowRelease " + hashName() + "  " + ((Object) sb));
            }
        }

        public void draw(Canvas canvas) {
            RectF targetViewBounds = getTargetViewBounds();
            ValueAnimator valueAnimator = this.mAnimator;
            Float valueOf = Float.valueOf(valueAnimator != null ? ((Float) valueAnimator.getAnimatedValue()).floatValue() : 1.0f);
            View view = (View) this.mTargetView.getParent();
            if (view == null) {
                AIFlickerTips.logi("mName = " + hashName() + "  " + targetViewBounds + ", parent is null");
                return;
            }
            String str = "mName = " + hashName() + "  " + targetViewBounds + " lt=" + this.mTargetView.getLeft() + "," + this.mTargetView.getTop() + " txy=" + view.getTranslationX() + "," + view.getTranslationY() + " sxy=" + view.getScrollX() + "," + view.getScrollY();
            if (!TextUtils.equals(this.mDrawLog, str) && System.currentTimeMillis() - this.updateDrawTime >= 250) {
                this.updateDrawTime = System.currentTimeMillis();
                this.mDrawLog = str;
                AIFlickerTips.logi(str);
            }
            Paint paint = AIFlickerTips.getInstance().mPaint;
            paint.setColor(15547195);
            paint.setAlpha((int) (valueOf.floatValue() * 51.0f));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(targetViewBounds, paint);
            paint.setColor(15547195);
            paint.setAlpha((int) (valueOf.floatValue() * 102.0f));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(targetViewBounds, paint);
        }

        public void release() {
            release(false);
        }

        public void release(boolean z) {
            if (z) {
                AIFlickerTips.logi("release mName = " + hashName() + " attach=" + this.mTargetView.isAttachedToWindow() + ":" + this.isAttachedToWindow);
            }
            this.mDrawLog = "";
            this.mAnimationLog = "";
            if (this.isAttachedToWindow) {
                return;
            }
            if (this.isAddAttachStateListener) {
                this.mTargetView.removeOnAttachStateChangeListener(this);
                this.isAddAttachStateListener = false;
            }
            this.mTargetView.getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
            List list = (List) AIFlickerTips.this.mFlickerMap.get(this.mName);
            if (list != null) {
                list.remove(this);
            }
            releaseAnimator();
        }

        private void releaseAnimator() {
            if (this.mAnimator == null) {
                return;
            }
            AIFlickerTips.logv("releaseAnimator, mName = " + hashName() + " mAnimator=" + Integer.toHexString(this.mAnimator.hashCode()));
            ValueAnimator valueAnimator = this.mAnimator;
            this.mAnimator = null;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
        }

        public void showAnimation() {
            boolean isAttachedToWindow = this.mTargetView.isAttachedToWindow();
            boolean z = this.isAttachedToWindow;
            View view = this.mFlickerView;
            boolean z2 = true;
            boolean z3 = view != null && view.isAttachedToWindow();
            ValueAnimator valueAnimator = this.mAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                z2 = false;
            }
            AIFlickerTips.logi("showAnimation " + hashName() + " a1=" + isAttachedToWindow + " a2=" + z + " a3=" + z3 + " animator=" + z2 + " view=" + this.mTargetView);
            if (isAttachedToWindow) {
                onViewAttachedToWindow(this.mTargetView);
            }
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewAttachedToWindow(View view) {
            if (AIFlickerTips.this.mPropNames.contains(this.mName)) {
                this.isAttachedToWindow = true;
                AIFlickerTips.this.getHandler().removeCallbacks(this.mDelayDetachedFromWindow);
                AIFlickerTips.logi("onViewAttachedToWindow " + hashName() + " view=" + this.mTargetView + " getRootView=" + this.mTargetView.getRootView());
                ViewGroup viewGroup = (ViewGroup) this.mTargetView.getRootView();
                if (this.mFlickerView != null && viewGroup != null && (viewGroup.getTag(AIFlickerTips.FLICKER_VIEW_ID) instanceof FlickerView)) {
                    FlickerView flickerView = (FlickerView) viewGroup.getTag(AIFlickerTips.FLICKER_VIEW_ID);
                    View view2 = this.mFlickerView;
                    if (flickerView != view2) {
                        ViewGroup viewGroup2 = (ViewGroup) view2.getParent();
                        if (viewGroup2 != null) {
                            viewGroup2.removeView(this.mFlickerView);
                        }
                        this.mFlickerView = null;
                    }
                }
                if (!AIFlickerTips.this.mFlickerViews.contains(this.mFlickerView)) {
                    this.mFlickerView = null;
                }
                if (view.getRootView() instanceof FlickerView) {
                    this.mFlickerView = view.getRootView();
                }
                if (this.mFlickerView == null) {
                    this.mFlickerView = AIFlickerTips.this.new FlickerView(this.mTargetView.getContext());
                    AIFlickerTips.this.mFlickerViews.add(this.mFlickerView);
                    viewGroup.addView(this.mFlickerView);
                    viewGroup.setTag(AIFlickerTips.FLICKER_VIEW_ID, this.mFlickerView);
                    this.mFlickerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.zte.gameassist.ai.AIFlickerTips.Flicker.2
                        @Override // android.view.View.OnLayoutChangeListener
                        public void onLayoutChange(View view3, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                            if ((Flicker.this.mAnimator == null || !Flicker.this.mAnimator.isStarted()) && Flicker.this.isAddAttachStateListener && !AIFlickerTips.this.getHandler().hasCallbacks(Flicker.this.mDelayDetachedFromWindow)) {
                                Flicker.this.postInvalidate();
                            }
                            Flicker.this.mFlickerView.removeOnLayoutChangeListener(this);
                        }
                    });
                    return;
                }
                ValueAnimator valueAnimator = this.mAnimator;
                if ((valueAnimator == null || !valueAnimator.isStarted()) && this.isAddAttachStateListener) {
                    postInvalidate();
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void postInvalidate() {
            if (TextUtils.equals(this.mName, getName())) {
                this.mTargetView.getViewTreeObserver().removeOnPreDrawListener(this.mOnPreDrawListener);
                this.mTargetView.getViewTreeObserver().addOnPreDrawListener(this.mOnPreDrawListener);
                this.mTargetView.postInvalidate();
            }
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewDetachedFromWindow(View view) {
            if (this.isAttachedToWindow) {
                AIFlickerTips.logi("onViewDetachedFromWindow " + hashName() + "  " + view);
                this.isAttachedToWindow = false;
                AIFlickerTips.this.getHandler().removeCallbacks(this.mDelayDetachedFromWindow);
                AIFlickerTips.this.getHandler().postDelayed(this.mDelayDetachedFromWindow, 200L);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void animatorEnd() {
            releaseAnimator();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Handler getHandler() {
        Handler handler = this.mHandlers.get();
        if (handler != null) {
            return handler;
        }
        Handler handler2 = new Handler(Looper.myLooper());
        this.mHandlers.set(handler2);
        return handler2;
    }

    private static class AnimatorListener implements Animator.AnimatorListener {
        private final Runnable mEndCallback;
        private final Runnable mStartCallback;

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        public AnimatorListener(Runnable runnable, Runnable runnable2) {
            this.mStartCallback = runnable;
            this.mEndCallback = runnable2;
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
            Runnable runnable = this.mStartCallback;
            if (runnable != null) {
                runnable.run();
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            Runnable runnable = this.mEndCallback;
            if (runnable != null) {
                runnable.run();
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
            Runnable runnable = this.mEndCallback;
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    public static View findVisibleViewById(View view, int i) {
        if (view != null && view.getVisibility() == 0) {
            if (view.getId() == i) {
                return view;
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i2 = 0; i2 < viewGroup.getChildCount(); i2++) {
                    View findVisibleViewById = findVisibleViewById(viewGroup.getChildAt(i2), i);
                    if (findVisibleViewById != null) {
                        return findVisibleViewById;
                    }
                }
            }
        }
        return null;
    }

    public static View findVisibleViewByName(View view, String str) {
        if (view != null && str != null && view.getVisibility() == 0) {
            if (str.equals(view.getTag(NAME_ID))) {
                return view;
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    View findVisibleViewByName = findVisibleViewByName(viewGroup.getChildAt(i), str);
                    if (findVisibleViewByName != null) {
                        return findVisibleViewByName;
                    }
                }
            }
        }
        return null;
    }

    public String toString() {
        return "AIFlickerTips : show=" + this.mPropNames + " all=" + ((String) this.mFlickerMap.keySet().stream().collect(Collectors.joining(",")));
    }
}

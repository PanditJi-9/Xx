package cn.nubia.tgk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class FakeFloatView extends TextView {
    private static final int MIN_SLIDE_DISTANCE = 10;
    private int VIEW_WIDTH_28;
    private int VIEW_WIDTH_32;
    private int canMoveHeight;
    private int canMoveWidth;
    private int canToBottom;
    private int canToLeft_l;
    private int canToRight_r;
    private int canToTop;
    private boolean isSecondaryScreen;
    private Context mContext;
    private int mDownPointRawX;
    private int mDownPointRawY;
    private int mDownRawX;
    private int mDownRawY;
    private long mEventMoveTime;
    private long mEventStartTime;
    private IFingerEventListener mFingerEventListener;
    private int mLeftOrRight;
    private boolean mMovable;
    private boolean mMove;
    private int mMovePointRawX;
    private int mMovePointRawY;
    private int mMoveRawX;
    private int mMoveRawY;
    private int mParentHeight;
    private int mParentWidth;
    private int mType;

    public interface IFingerEventListener {
        void fingerDown(int i);

        void fingerStartMove(int i);

        void fingerUp(int i);
    }

    public FakeFloatView(Context context) {
        super(context);
        this.mLeftOrRight = 0;
        this.isSecondaryScreen = false;
        this.mMovable = true;
        this.mType = 0;
        this.canMoveWidth = 1644;
        this.canMoveHeight = 778;
        this.canToTop = 150;
        this.canToBottom = 928;
        this.canToLeft_l = 318;
        this.canToRight_r = 1960;
        this.mContext = context;
    }

    public FakeFloatView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLeftOrRight = 0;
        this.isSecondaryScreen = false;
        this.mMovable = true;
        this.mType = 0;
        this.canMoveWidth = 1644;
        this.canMoveHeight = 778;
        this.canToTop = 150;
        this.canToBottom = 928;
        this.canToLeft_l = 318;
        this.canToRight_r = 1960;
        this.mContext = context;
        this.VIEW_WIDTH_32 = context.getResources().getDimensionPixelSize(R.dimen.tgk_ffv_width_32);
        this.VIEW_WIDTH_28 = this.mContext.getResources().getDimensionPixelSize(R.dimen.tgk_ffv_width_28);
    }

    public FakeFloatView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mLeftOrRight = 0;
        this.isSecondaryScreen = false;
        this.mMovable = true;
        this.mType = 0;
        this.canMoveWidth = 1644;
        this.canMoveHeight = 778;
        this.canToTop = 150;
        this.canToBottom = 928;
        this.canToLeft_l = 318;
        this.canToRight_r = 1960;
        this.mContext = context;
    }

    public FakeFloatView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mLeftOrRight = 0;
        this.isSecondaryScreen = false;
        this.mMovable = true;
        this.mType = 0;
        this.canMoveWidth = 1644;
        this.canMoveHeight = 778;
        this.canToTop = 150;
        this.canToBottom = 928;
        this.canToLeft_l = 318;
        this.canToRight_r = 1960;
        this.mContext = context;
    }

    @Override // android.widget.TextView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int i;
        int i2;
        int i3;
        View view = (View) getParent();
        this.mParentWidth = view.getWidth();
        this.mParentHeight = view.getHeight();
        float axisValue = motionEvent.getAxisValue(12, motionEvent.getActionIndex());
        int action = motionEvent.getAction();
        if (action == 0) {
            this.mEventStartTime = System.currentTimeMillis();
            this.mDownPointRawX = (int) motionEvent.getRawX();
            this.mDownPointRawY = (int) motionEvent.getRawY();
            this.mDownRawX = (int) motionEvent.getRawX();
            this.mDownRawY = (int) motionEvent.getRawY();
            IFingerEventListener iFingerEventListener = this.mFingerEventListener;
            if (iFingerEventListener != null) {
                iFingerEventListener.fingerDown(getId());
            }
        } else if (action == 1) {
            IFingerEventListener iFingerEventListener2 = this.mFingerEventListener;
            if (iFingerEventListener2 != null) {
                iFingerEventListener2.fingerUp(getId());
            }
            this.mMove = false;
            this.mDownRawY = 0;
            this.mDownRawX = 0;
            this.mMoveRawY = 0;
            this.mMoveRawX = 0;
        } else if (action == 2) {
            this.mEventMoveTime = System.currentTimeMillis();
            if (axisValue != 0.5f && axisValue != 0.6f && axisValue != 0.7f && axisValue != 0.8f) {
                this.mMovePointRawX = (int) motionEvent.getRawX();
                this.mMovePointRawY = (int) motionEvent.getRawY();
                this.mMoveRawX = (int) motionEvent.getRawX();
                this.mMoveRawY = (int) motionEvent.getRawY();
                if (this.mMovable && (i = this.mDownPointRawX) > 0 && i > 0 && (i2 = this.mMovePointRawY) > 0 && (i3 = this.mDownPointRawY) > 0 && this.mEventMoveTime - this.mEventStartTime > 100) {
                    updatePosition(this.mMovePointRawX - i, i2 - i3);
                    if ((Math.abs(this.mMoveRawX - this.mDownRawX) > 10 || Math.abs(this.mMoveRawY - this.mDownRawY) > 10) && !this.mMove) {
                        this.mFingerEventListener.fingerStartMove(getId());
                        this.mMove = true;
                    }
                }
            }
        } else if (action == 6) {
            IFingerEventListener iFingerEventListener3 = this.mFingerEventListener;
            if (iFingerEventListener3 != null) {
                iFingerEventListener3.fingerUp(getId());
            }
            this.mMove = false;
            this.mDownRawY = 0;
            this.mDownRawX = 0;
            this.mMoveRawY = 0;
            this.mMoveRawX = 0;
        }
        return true;
    }

    private void updatePosition(int i, int i2) {
        int left = getLeft() + i;
        int right = getRight() + i;
        int top = getTop() + i2;
        int bottom = getBottom() + i2;
        if (left < 0) {
            right = getWidth();
            left = 0;
        }
        if (top < 0) {
            bottom = getHeight();
            top = 0;
        }
        int i3 = this.mParentWidth;
        if (right > i3) {
            left = i3 - getWidth();
            right = i3;
        }
        int i4 = this.mParentHeight;
        if (bottom > i4) {
            top = i4 - getHeight();
            bottom = i4;
        }
        layout(left, top, right, bottom);
        this.mDownPointRawX = this.mMovePointRawX;
        this.mDownPointRawY = this.mMovePointRawY;
    }

    @Override // android.view.View
    public void layout(int i, int i2, int i3, int i4) {
        super.layout(i, i2, i3, i4);
    }

    public void setFingerEventListener(IFingerEventListener iFingerEventListener) {
        this.mFingerEventListener = iFingerEventListener;
    }

    public void setFingerLeftOrRight(int i, boolean z) {
        this.mLeftOrRight = i;
        this.isSecondaryScreen = z;
    }

    public void setMoveEnabled(boolean z) {
        this.mMovable = z;
    }

    public void setType(int i) {
        this.mType = i;
        int i2 = this.VIEW_WIDTH_28;
        if (2 == i) {
            i2 = this.VIEW_WIDTH_32;
        }
        changeFloatBallWidth(i2);
    }

    private void changeFloatBallWidth(int i) {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (i > 0) {
            layoutParams.width = i;
        }
        setLayoutParams(layoutParams);
    }
}

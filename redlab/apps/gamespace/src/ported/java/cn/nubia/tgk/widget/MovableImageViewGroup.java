package cn.nubia.tgk.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class MovableImageViewGroup extends ViewGroup {
    private static String TAG = "MovableImageViewGroup";
    private Button btn;
    private boolean first;
    private ImageView imageView;
    private View.OnClickListener listener;
    private Bitmap mBgBitmap;
    private int mDownPointRawX;
    private int mDownPointRawY;
    private int mDownRawX;
    private int mDownRawY;
    private onClosedListener mListener;
    private int mMovePointRawX;
    private int mMovePointRawY;
    private int mMoveRawX;
    private int mMoveRawY;
    private int mParentViewHeight;
    private int mParentViewWidth;

    public interface onClosedListener {
        void onClose();
    }

    public MovableImageViewGroup(Context context) {
        this(context, null);
    }

    public MovableImageViewGroup(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
        Log.d(TAG, "MovableImageViewGroup");
    }

    public MovableImageViewGroup(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mBgBitmap = null;
        this.first = true;
        this.listener = new View.OnClickListener() { // from class: cn.nubia.tgk.widget.MovableImageViewGroup.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (MovableImageViewGroup.this.mListener != null) {
                    Log.d("listener", "onClick lister!");
                    MovableImageViewGroup.this.mListener.onClose();
                }
            }
        };
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int mode = View.MeasureSpec.getMode(i);
        View.MeasureSpec.getSize(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        View.MeasureSpec.getSize(i2);
        Log.d(TAG, "onMeasure widthMode=" + mode + ", heightMode=" + mode2);
        Log.d(TAG, "onMeasure widthMeasureSpec=" + i + ", heightMeasureSpec=" + i2);
        measureChildren(i, i2);
        if (mode == Integer.MIN_VALUE && mode2 == Integer.MIN_VALUE) {
            Log.d(TAG, "count111=1");
            View childAt = getChildAt(0);
            setMeasuredDimension(childAt.getMeasuredWidth(), childAt.getMeasuredHeight());
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (this.imageView != null) {
            return;
        }
        int childCount = getChildCount();
        Log.d(TAG, "count=" + childCount);
        int i5 = 0;
        int i6 = 0;
        for (int i7 = 0; i7 < childCount; i7++) {
            View childAt = getChildAt(i7);
            if (childAt.getId() == R.id.tgk_preview_img) {
                this.imageView = (ImageView) childAt;
                i6 = childAt.getMeasuredWidth();
                int i8 = i5 + i6;
                childAt.layout(i5, 0, i8, childAt.getMeasuredHeight());
                this.imageView.setImageBitmap(this.mBgBitmap);
                Log.d(TAG, "move_img left=" + i8);
                i5 = i8;
            } else if (childAt.getId() == R.id.tgk_preview_close_btn) {
                this.btn = (Button) childAt;
                Log.d(TAG, "close_btn left1=" + i5 + ", childWidth=" + i6 + ", child.getMeasuredWidth()=" + childAt.getMeasuredWidth());
                i5 = (i6 - childAt.getMeasuredWidth()) - 24;
                Log.d(TAG, "close_btn left=" + i5);
                childAt.layout(i5, 18, childAt.getMeasuredWidth() + i5, childAt.getMeasuredHeight() + 18);
            }
        }
        Button button = this.btn;
        if (button != null) {
            button.setOnClickListener(this.listener);
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        View rootView = getRootView();
        this.mParentViewWidth = rootView.getWidth();
        this.mParentViewHeight = rootView.getHeight();
        Log.d(TAG, "event mParentViewWidth=" + this.mParentViewWidth + ", mParentViewHeight=" + this.mParentViewHeight);
        int action = motionEvent.getAction();
        if (action == 0) {
            int rawX = (int) motionEvent.getRawX();
            this.mDownRawX = rawX;
            this.mDownPointRawX = rawX;
            int rawY = (int) motionEvent.getRawY();
            this.mDownRawY = rawY;
            this.mDownPointRawY = rawY;
        } else if (action == 2) {
            int rawX2 = (int) motionEvent.getRawX();
            this.mMoveRawX = rawX2;
            this.mMovePointRawX = rawX2;
            int rawY2 = (int) motionEvent.getRawY();
            this.mMoveRawY = rawY2;
            this.mMovePointRawY = rawY2;
            updatePosition(this.mMovePointRawX - this.mDownPointRawX, rawY2 - this.mDownPointRawY);
        }
        return super.onTouchEvent(motionEvent);
    }

    private void updatePosition(int i, int i2) {
        Log.d(TAG, "offsetX=" + i + ", offsetY=" + i2);
        int left = getLeft() + i;
        int top = getTop() + i2;
        int right = getRight() + i;
        int bottom = getBottom() + i2;
        if (left < 0) {
            right = getWidth();
            left = 0;
        }
        if (top < 0) {
            bottom = getHeight();
            top = 0;
        }
        int i3 = this.mParentViewWidth;
        if (right > i3) {
            left = i3 - getWidth();
            right = i3;
        }
        int i4 = this.mParentViewHeight;
        if (bottom > i4) {
            top = i4 - getHeight();
            bottom = i4;
        }
        Log.d(TAG, "left=" + left + ",top=" + top + ",right=" + right + ",bottom=" + bottom);
        layout(left, top, right, bottom);
        this.mDownPointRawX = this.mMovePointRawX;
        this.mDownPointRawY = this.mMovePointRawY;
    }

    public void setListener(onClosedListener onclosedlistener) {
        this.mListener = onclosedlistener;
    }

    public void setImageBitmap(Bitmap bitmap) {
        Log.d(TAG, "in setImageBitmap");
        this.mBgBitmap = bitmap;
        if (this.imageView != null) {
            Log.d(TAG, "imageView is not null");
            this.imageView.setImageBitmap(this.mBgBitmap);
        }
    }
}

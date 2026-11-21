package dev.redlab.gamespace.gamecontrolpanel.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.core.view.ViewCompat;
import androidx.exifinterface.media.ExifInterface;
import dev.redlab.gamespace.R;
import java.util.List;

/* loaded from: classes.dex */
public class CustomSeekBar extends View {
    private static final String TAG = "CustomSeekBar";
    private ChangeListener mChangeListener;
    private List<String> mDataList;
    private boolean mIsAllowTouch;
    private float mLastX;
    private float mLastY;
    private int mLineColor;
    private int mLineHeight;
    private Paint mPaint;
    private int mSegmentColor;
    private float mSegmentWidth;
    private String mSelectedValue;
    private String mSelectedValue2;
    private int mSelectedValueIndex;
    private int mSelectedValueIndex2;
    private int mShowTickMarksType;
    private int mTextColor;
    private int mTextLineSpace;
    private int mTextSize;
    private Bitmap mThumbBitmap;
    private int mTouchIndex;
    private int mTouchSlop;

    public interface ChangeListener {
        void onChange(CustomSeekBar customSeekBar, String str);
    }

    public CustomSeekBar(Context context) {
        this(context, null);
    }

    public CustomSeekBar(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CustomSeekBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mIsAllowTouch = true;
        initAttributes(context, attributeSet);
        initPaint(context);
    }

    private void initAttributes(Context context, AttributeSet attributeSet) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.CustomSeekBar);
        this.mThumbBitmap = ((BitmapDrawable) obtainStyledAttributes.getDrawable(7)).getBitmap();
        this.mLineHeight = obtainStyledAttributes.getDimensionPixelSize(1, 4);
        this.mTextLineSpace = obtainStyledAttributes.getDimensionPixelSize(5, 4);
        this.mTextSize = obtainStyledAttributes.getDimensionPixelSize(6, 4);
        this.mSegmentColor = obtainStyledAttributes.getColor(2, ViewCompat.MEASURED_STATE_MASK);
        this.mLineColor = obtainStyledAttributes.getColor(0, ViewCompat.MEASURED_STATE_MASK);
        this.mTextColor = obtainStyledAttributes.getColor(4, ViewCompat.MEASURED_STATE_MASK);
        this.mShowTickMarksType = obtainStyledAttributes.getInt(3, 0);
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        obtainStyledAttributes.close();
    }

    private void initPaint(Context context) {
        this.mPaint = new Paint(1);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        int i = this.mLineHeight / 2;
        int width2 = this.mThumbBitmap.getWidth() / 2;
        int i2 = height / 2;
        this.mPaint.setColor(this.mLineColor);
        this.mPaint.setStrokeWidth(this.mLineHeight);
        this.mPaint.setStrokeCap(Paint.Cap.ROUND);
        float f = width2;
        float f2 = i2;
        canvas.drawLine(f, f2, width - width2, f2, this.mPaint);
        List<String> list = this.mDataList;
        if (list == null || list.size() < 2) {
            return;
        }
        this.mPaint.setColor(this.mSegmentColor);
        this.mSegmentWidth = (width - (width2 * 2)) / (this.mDataList.size() - 1);
        if (this.mShowTickMarksType != 0) {
            for (int i3 = 0; i3 < this.mDataList.size(); i3++) {
                int i4 = this.mShowTickMarksType;
                if (i4 == 1) {
                    canvas.drawCircle((this.mSegmentWidth * i3) + f, f2, i, this.mPaint);
                } else if (i4 == 2 && (i3 == 0 || i3 == this.mDataList.size() - 1)) {
                    canvas.drawCircle((this.mSegmentWidth * i3) + f, f2, i, this.mPaint);
                }
            }
        }
        this.mPaint.setColor(this.mTextColor);
        this.mPaint.setTextSize(this.mTextSize);
        float f3 = (i2 - i) - this.mTextLineSpace;
        int i5 = this.mSelectedValueIndex;
        if (i5 >= 0 && i5 < this.mDataList.size()) {
            String str = this.mDataList.get(this.mSelectedValueIndex) + ExifInterface.LATITUDE_SOUTH;
            float measureText = this.mPaint.measureText(str);
            int i6 = this.mSelectedValueIndex;
            if (i6 == 0) {
                canvas.drawText(str, f, f3, this.mPaint);
            } else if (i6 == this.mDataList.size() - 1) {
                canvas.drawText(str, (width - measureText) - f, f3, this.mPaint);
            } else {
                canvas.drawText(str, (f + (this.mSegmentWidth * this.mSelectedValueIndex)) - (measureText / 2.0f), f3, this.mPaint);
            }
        }
        canvas.drawBitmap(this.mThumbBitmap, this.mSelectedValueIndex * this.mSegmentWidth, i2 - width2, this.mPaint);
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.mIsAllowTouch) {
            return true;
        }
        int action = motionEvent.getAction();
        if (action == 0) {
            onTouchDown(motionEvent);
            return true;
        }
        if (action == 1) {
            onTouchUp(motionEvent);
        } else if (action == 2) {
            onTouchMove(motionEvent);
        }
        return super.onTouchEvent(motionEvent);
    }

    private void onTouchDown(MotionEvent motionEvent) {
        this.mLastX = motionEvent.getX();
        this.mLastY = motionEvent.getY();
        motionEvent.getX();
        this.mTouchIndex = 0;
    }

    private void onTouchMove(MotionEvent motionEvent) {
        float abs = Math.abs(motionEvent.getX() - this.mLastX);
        float abs2 = Math.abs(motionEvent.getY() - this.mLastY);
        int i = this.mTouchSlop;
        if ((abs >= i || abs2 >= i) && abs >= abs2) {
            getParent().requestDisallowInterceptTouchEvent(true);
            int round = Math.round((motionEvent.getX() - this.mThumbBitmap.getWidth()) / this.mSegmentWidth);
            if (round < 0) {
                round = 0;
            } else if (round > this.mDataList.size() - 1) {
                round = this.mDataList.size() - 1;
            }
            double x = motionEvent.getX() % this.mSegmentWidth;
            if (x > 0.6d || x < 0.4d) {
                if (this.mTouchIndex == 0) {
                    updateSelectValue(round, this.mSelectedValueIndex2);
                } else {
                    updateSelectValue(this.mSelectedValueIndex, round);
                }
            }
            this.mLastX = motionEvent.getX();
            this.mLastY = motionEvent.getY();
        }
    }

    private void onTouchUp(MotionEvent motionEvent) {
        int round = Math.round((motionEvent.getX() - this.mThumbBitmap.getWidth()) / this.mSegmentWidth);
        if (round < 0) {
            round = 0;
        } else if (round > this.mDataList.size() - 1) {
            round = this.mDataList.size() - 1;
        }
        if (this.mTouchIndex == 0) {
            updateSelectValue(round, this.mSelectedValueIndex2);
        } else {
            updateSelectValue(this.mSelectedValueIndex, round);
        }
        notifyDataChange();
    }

    private void updateSelectValue(int i, int i2) {
        if (i == this.mSelectedValueIndex && i2 == this.mSelectedValueIndex2) {
            return;
        }
        this.mSelectedValueIndex = i;
        this.mSelectedValue = this.mDataList.get(i);
        invalidate();
    }

    private void notifyDataChange() {
        ChangeListener changeListener = this.mChangeListener;
        if (changeListener != null) {
            changeListener.onChange(this, this.mSelectedValue);
        }
    }

    public void setDataList(List<String> list) {
        this.mDataList = list;
        updateSelectValueIndex();
        invalidate();
    }

    public void selectValue(String str) {
        this.mSelectedValue = str;
        updateSelectValueIndex();
        invalidate();
    }

    public void setIsAllowTouch(boolean z) {
        this.mIsAllowTouch = z;
    }

    private void updateSelectValueIndex() {
        List<String> list = this.mDataList;
        if (list == null || list.isEmpty()) {
            return;
        }
        int indexOf = this.mDataList.indexOf(this.mSelectedValue);
        this.mSelectedValueIndex = indexOf;
        if (indexOf < 0) {
            this.mSelectedValueIndex = 0;
        }
        int indexOf2 = this.mDataList.indexOf(this.mSelectedValue2);
        this.mSelectedValueIndex2 = indexOf2;
        if (indexOf2 < 0) {
            this.mSelectedValueIndex2 = Math.max(this.mDataList.size() - 1, 0);
        }
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.mChangeListener = changeListener;
    }
}

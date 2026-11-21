package dev.redlab.gamespace.gamecontrolpanel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.SeekBar;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public class GyroSenSeekBar extends SeekBar {
    private boolean isBeginCenter;
    private boolean isRtlDirection;
    private int mHeightPoint;
    private int mPaddingLeft;
    private int mPaddingRight;
    private Paint mPaintForRet;
    private int mProgressHeight;
    private int mThumbOffset;
    private int mWidth;

    public GyroSenSeekBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPaintForRet = null;
        this.isRtlDirection = context.getResources().getConfiguration().getLayoutDirection() == 1;
        init();
    }

    private void init() {
        Paint paint = new Paint();
        this.mPaintForRet = paint;
        paint.setColor(getContext().getResources().getColor(R.color.gamecontrol_seekbar_progress_color));
        this.mProgressHeight = getMinimumHeight();
        this.mPaddingLeft = getPaddingLeft();
        this.mPaddingRight = getPaddingRight();
        this.mThumbOffset = (getThumb().getIntrinsicWidth() / 2) - 5;
    }

    @Override // android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        redraw(canvas);
    }

    private void redraw(Canvas canvas) {
        int i;
        int i2;
        int progress = getProgress();
        this.mHeightPoint = (((getHeight() - getPaddingBottom()) - getPaddingTop()) - this.mProgressHeight) / 2;
        this.mWidth = getWidth();
        int max = getMax();
        Rect rect = new Rect();
        if (this.isBeginCenter) {
            if (progress < max / 2) {
                int i3 = this.mPaddingLeft;
                int i4 = (this.mThumbOffset / 2) + i3;
                int i5 = this.mWidth;
                i = i4 + ((((i5 - i3) - this.mPaddingRight) * progress) / max);
                i2 = i5 / 2;
            } else {
                int i6 = this.mWidth;
                i = i6 / 2;
                int i7 = this.mPaddingLeft;
                i2 = (i7 - (this.mThumbOffset / 2)) + ((((i6 - i7) - this.mPaddingRight) * progress) / max);
            }
        } else {
            i = this.mPaddingLeft;
            i2 = ((((this.mWidth - i) - this.mPaddingRight) * progress) / max) + (i - (this.mThumbOffset / 2));
        }
        if (i >= i2) {
            int i8 = this.mWidth;
            i = i8 / 2;
            i2 = i8 / 2;
        }
        if (this.isRtlDirection) {
            int i9 = this.mWidth;
            int i10 = this.mHeightPoint;
            rect.set(i9 - i2, i10, i9 - i, this.mProgressHeight + i10);
        } else {
            int i11 = this.mHeightPoint;
            rect.set(i, i11, i2, this.mProgressHeight + i11);
        }
        canvas.drawRect(rect, this.mPaintForRet);
    }

    public boolean isBeginCenter() {
        return this.isBeginCenter;
    }

    public void setBeginCenter(boolean z) {
        this.isBeginCenter = z;
        LayerDrawable layerDrawable = (LayerDrawable) getProgressDrawable();
        layerDrawable.setDrawableByLayerId(android.R.id.progress, new ColorDrawable(android.R.color.transparent));
        setProgressDrawable(layerDrawable);
        invalidate();
    }
}

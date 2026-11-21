package cn.nubia.common.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import cn.nubia.common.R;

/* loaded from: classes.dex */
public class ProgressView extends View {
    private int mAngle;
    private int mHeight;
    private boolean mPaused;
    private Bitmap mPausedBitmap;
    private int mProgress;
    private int mProgressColor;
    private float mProgressRadius;
    private float mRoundRadius;
    private int mStateColor;
    private int mStripeColor;
    private float mStripeRadius;
    private float mStripeWidth;
    private int mWidth;

    public ProgressView(Context context) {
        this(context, null);
    }

    public ProgressView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public ProgressView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPaused = false;
        this.mPausedBitmap = null;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.ProgressView, i, 0);
        int dimension = (int) obtainStyledAttributes.getDimension(R.styleable.ProgressView_progress_rect_width, 252.0f);
        this.mHeight = dimension;
        this.mWidth = dimension;
        this.mStripeWidth = obtainStyledAttributes.getDimension(R.styleable.ProgressView_progress_stripe_width, 6.0f);
        this.mProgress = obtainStyledAttributes.getInteger(R.styleable.ProgressView_progress, 0);
        this.mProgressColor = obtainStyledAttributes.getColor(R.styleable.ProgressView_progress_color, -1728053248);
        this.mStripeColor = obtainStyledAttributes.getColor(R.styleable.ProgressView_progress_stripe_color, 0);
        this.mStateColor = obtainStyledAttributes.getColor(R.styleable.ProgressView_progress_state_color, -1);
        this.mProgressRadius = obtainStyledAttributes.getDimensionPixelSize(R.styleable.ProgressView_progress_radius, 102);
        this.mRoundRadius = obtainStyledAttributes.getDimensionPixelSize(R.styleable.ProgressView_progress_round_radius, 34);
        obtainStyledAttributes.recycle();
        this.mStripeRadius = this.mProgressRadius + this.mStripeWidth;
        setLayerType(1, null);
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int mode = View.MeasureSpec.getMode(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size = View.MeasureSpec.getSize(i);
        int size2 = View.MeasureSpec.getSize(i2);
        if (mode == 1073741824 && mode2 == 1073741824) {
            this.mProgressRadius = (size / 2) - this.mStripeWidth;
            this.mWidth = size;
            this.mHeight = size2;
        }
        setMeasuredDimension(this.mWidth, this.mHeight);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float f = this.mWidth / 2;
        float f2 = this.mHeight / 2;
        this.mAngle = (int) (this.mProgress * 3.6d);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(this.mProgressColor);
        RectF rectF = new RectF(0.0f, 0.0f, this.mWidth, this.mHeight);
        float f3 = this.mRoundRadius;
        canvas.drawRoundRect(rectF, f3, f3, paint);
        paint.setAntiAlias(true);
        paint.setColor(this.mStripeColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(this.mStripeWidth);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawCircle(f, f2, this.mStripeRadius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(this.mProgressColor);
        paint.setAntiAlias(true);
        int i = this.mWidth;
        float f4 = this.mStripeRadius;
        int i2 = this.mHeight;
        canvas.drawArc(new RectF((i / 2) - f4, (i2 / 2) - f4, (i / 2) + f4, (i2 / 2) + f4), 270.0f, this.mAngle, true, paint);
        if (this.mPaused) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            paint.setColor(this.mStateColor);
            paint.setShadowLayer(4.0f, 0.0f, 2.0f, -1);
            int i3 = (int) f;
            int i4 = (int) f2;
            int i5 = i4 - 15;
            int i6 = i4 + 15;
            canvas.drawRect(new Rect(i3 - 12, i5, i3 - 4, i6), paint);
            canvas.drawRect(new Rect(i3 + 4, i5, i3 + 12, i6), paint);
        }
        paint.setXfermode(null);
    }

    public void setProgress(int i, boolean z) {
        if (i > 100) {
            throw new IllegalArgumentException("percent must less than 100!");
        }
        this.mProgress = i;
        this.mPaused = z;
        postInvalidate();
    }
}

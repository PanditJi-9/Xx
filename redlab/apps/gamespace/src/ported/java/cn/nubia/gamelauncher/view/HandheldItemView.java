package dev.redlab.gamespace.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class HandheldItemView extends FrameLayout {
    private final int BORDER_W_START;
    private Bitmap bg_rectangle;
    private Bitmap bg_square;
    private RectF blurRect;
    private float blurWidth;
    private float borderAlpha;
    private Paint borderPaint;
    private RectF borderRect;
    private int borderWidth;
    private int height;
    public boolean isSquare;
    public boolean mSelect;
    private int width;

    public HandheldItemView(Context context) {
        super(context);
        this.BORDER_W_START = 10;
        this.blurWidth = 48.0f;
        this.mSelect = false;
        init();
    }

    public HandheldItemView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.BORDER_W_START = 10;
        this.blurWidth = 48.0f;
        this.mSelect = false;
        init();
    }

    public HandheldItemView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.BORDER_W_START = 10;
        this.blurWidth = 48.0f;
        this.mSelect = false;
        init();
    }

    private void init() {
        this.borderWidth = 10;
        Paint paint = new Paint();
        this.borderPaint = paint;
        paint.setStyle(Paint.Style.STROKE);
        this.borderPaint.setColor(Color.parseColor("#FFFFFFFF"));
        this.bg_square = BitmapFactory.decodeResource(getResources(), R.drawable.bg_handheld_select_square);
        this.bg_rectangle = BitmapFactory.decodeResource(getResources(), R.drawable.bg_handheld_select_rectangle);
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.width = View.MeasureSpec.getSize(i);
        this.height = View.MeasureSpec.getSize(i2);
        this.blurRect = new RectF(0.0f, 0.0f, this.width, this.height);
        float f = this.blurWidth;
        int i5 = this.borderWidth;
        this.borderRect = new RectF((i5 / 2) + f, (i5 / 2) + f, (this.width - (i5 / 2)) - f, (this.height - (i5 / 2)) - f);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mSelect) {
            this.borderPaint.setStrokeWidth(this.borderWidth);
            this.borderPaint.setAlpha((int) (this.borderAlpha * 255.0f));
            if (this.isSquare) {
                canvas.drawBitmap(this.bg_square, (Rect) null, this.blurRect, this.borderPaint);
            } else {
                canvas.drawBitmap(this.bg_rectangle, (Rect) null, this.blurRect, this.borderPaint);
            }
            RectF rectF = this.borderRect;
            float f = this.blurWidth;
            int i = this.borderWidth;
            rectF.set((i / 2) + f, (i / 2) + f, (this.width - (i / 2)) - f, (this.height - (i / 2)) - f);
            canvas.drawRect(this.borderRect, this.borderPaint);
        }
    }

    public int getBorderWidth() {
        return this.borderWidth;
    }

    public void setBorderWidth(int i) {
        this.borderWidth = i;
    }

    public float getBorderAlpha() {
        return this.borderAlpha;
    }

    public void setBorderAlpha(float f) {
        this.borderAlpha = f;
    }

    public void setSelect(boolean z) {
        this.mSelect = z;
    }
}

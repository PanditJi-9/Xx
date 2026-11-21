package dev.redlab.gamespace.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import androidx.appcompat.widget.AppCompatImageView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class CustomRoundImageView extends AppCompatImageView {
    private final Paint mPaint;
    private float mRadius;
    private final RectF mRect;

    public CustomRoundImageView(Context context) {
        this(context, null);
    }

    public CustomRoundImageView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public CustomRoundImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mPaint = new Paint(1);
        this.mRect = new RectF();
        this.mRadius = 0.0f;
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.RoundImageView, i, 0);
        this.mRadius = obtainStyledAttributes.getDimension(0, 0.0f);
        obtainStyledAttributes.recycle();
    }

    @Override // android.widget.ImageView, android.view.View
    protected void onDraw(Canvas canvas) {
        Bitmap drawableToBitmap;
        if (getDrawable() == null || (drawableToBitmap = drawableToBitmap(getDrawable())) == null) {
            return;
        }
        BitmapShader bitmapShader = new BitmapShader(drawableToBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        this.mPaint.setShader(bitmapShader);
        updateShaderMatrix(drawableToBitmap, bitmapShader);
        this.mRect.set(0.0f, 0.0f, getWidth(), getHeight());
        RectF rectF = this.mRect;
        float f = this.mRadius;
        canvas.drawRoundRect(rectF, f, f, this.mPaint);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return null;
        }
        Bitmap createBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        return createBitmap;
    }

    private void updateShaderMatrix(Bitmap bitmap, BitmapShader bitmapShader) {
        float width;
        float height;
        Matrix matrix = new Matrix();
        int i = AnonymousClass1.$SwitchMap$android$widget$ImageView$ScaleType[getScaleType().ordinal()];
        if (i == 1) {
            float f = 0.0f;
            if (bitmap.getWidth() * getHeight() > getWidth() * bitmap.getHeight()) {
                width = getHeight() / bitmap.getHeight();
                f = (getWidth() - (bitmap.getWidth() * width)) * 0.5f;
                height = 0.0f;
            } else {
                width = getWidth() / bitmap.getWidth();
                height = (getHeight() - (bitmap.getHeight() * width)) * 0.5f;
            }
            matrix.setScale(width, width);
            matrix.postTranslate(Math.round(f), Math.round(height));
        } else if (i == 2) {
            matrix.setScale(getWidth() / bitmap.getWidth(), getHeight() / bitmap.getHeight());
        }
        bitmapShader.setLocalMatrix(matrix);
    }

    /* renamed from: dev.redlab.gamespace.view.CustomRoundImageView$1, reason: invalid class name */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$widget$ImageView$ScaleType;

        static {
            int[] iArr = new int[ImageView.ScaleType.values().length];
            $SwitchMap$android$widget$ImageView$ScaleType = iArr;
            try {
                iArr[ImageView.ScaleType.CENTER_CROP.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ImageView.ScaleType.FIT_XY.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
        }
    }
}

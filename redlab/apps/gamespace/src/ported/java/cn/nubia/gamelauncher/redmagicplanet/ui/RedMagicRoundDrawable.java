package dev.redlab.gamespace.redmagicplanet.ui;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

/* loaded from: classes.dex */
public class RedMagicRoundDrawable extends BitmapDrawable {
    private static final String TAG = "RedMagicRoundDrawable";
    private final float origin_radius;
    private final Paint paint;
    private float radius;
    private RectF rectF;

    public RedMagicRoundDrawable(Resources resources, Bitmap bitmap) {
        super(resources, bitmap);
        this.origin_radius = 75.0f;
        this.radius = 75.0f;
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Paint paint = new Paint();
        this.paint = paint;
        paint.setAntiAlias(true);
        paint.setShader(bitmapShader);
    }

    @Override // android.graphics.drawable.BitmapDrawable, android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        RectF rectF = this.rectF;
        float f = this.radius;
        canvas.drawRoundRect(rectF, f, f, this.paint);
    }

    public void setRadius(float f) {
        this.radius = f * 75.0f;
    }

    @Override // android.graphics.drawable.Drawable
    public void setBounds(int i, int i2, int i3, int i4) {
        this.rectF = new RectF(i, i2, i3, i4);
    }

    public void release() {
        this.paint.setShader(null);
    }
}

package cn.nubia.gamecenter.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.widget.ImageView;
import androidx.core.view.ViewCompat;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;

/* loaded from: classes.dex */
public class CircleClipImageView extends ImageView {
    public static final Property<CircleClipImageView, Float> CUST_MOVE_Y = new FloatProperty<CircleClipImageView>(AnimatorHelper.Item.CUST_MOVE_Y) { // from class: cn.nubia.gamecenter.settings.widget.CircleClipImageView.1
        @Override // android.util.FloatProperty
        public void setValue(CircleClipImageView circleClipImageView, float f) {
            circleClipImageView.setMoveY(f);
        }

        @Override // android.util.Property
        public Float get(CircleClipImageView circleClipImageView) {
            return Float.valueOf(circleClipImageView.getMoveY());
        }
    };
    private static final String TAG = "CircleClipImageView";
    Path m_clip;
    private int m_clipHeight;
    private int m_clipWidth;
    private LinearGradient m_gradient;
    private float m_moveY;
    private int m_oldHeight;
    private int m_oldWidth;
    Paint m_paint;

    public CircleClipImageView(Context context) {
        this(context, null);
    }

    public CircleClipImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public CircleClipImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    private Path getPath() {
        if (this.m_clip == null) {
            this.m_clip = new Path();
            int width = getWidth();
            this.m_clip.addCircle(width / 2, getHeight() / 2, width / 2.0f, Path.Direction.CCW);
        }
        return this.m_clip;
    }

    private Paint getPaint() {
        if (this.m_paint == null) {
            this.m_paint = new Paint();
        }
        return this.m_paint;
    }

    private LinearGradient getGradient() {
        if (this.m_gradient == null) {
            if (getHeight() == 0) {
                return null;
            }
            getWidth();
            this.m_gradient = new LinearGradient(0.0f, 0.0f, 0.0f, getHeight(), new int[]{ViewCompat.MEASURED_STATE_MASK, -4378359, ViewCompat.MEASURED_STATE_MASK}, (float[]) null, Shader.TileMode.CLAMP);
        }
        return this.m_gradient;
    }

    private void clearPath() {
        this.m_clip = null;
    }

    private void clearPaint() {
        this.m_paint = null;
    }

    private void clearGradient() {
        this.m_gradient = null;
    }

    @Override // android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.m_oldWidth == getWidth() && this.m_oldHeight == getHeight()) {
            return;
        }
        clearPath();
        clearPaint();
        clearGradient();
        this.m_oldWidth = getWidth();
        this.m_oldHeight = getHeight();
    }

    @Override // android.widget.ImageView, android.view.View
    protected void onDraw(Canvas canvas) {
        canvas.save();
        if (getPath() != null) {
            canvas.clipPath(getPath());
        }
        if (getGradient() != null) {
            getPaint().setShader(getGradient());
            getPaint().setStrokeWidth(getWidth());
            canvas.translate(0.0f, ((this.m_moveY * 2.0f) * getHeight()) - getHeight());
            canvas.drawLine(getWidth() / 2, 0.0f, getWidth() / 2, getHeight(), getPaint());
        }
        canvas.restore();
    }

    public void setMoveY(float f) {
        this.m_moveY = f;
        requestLayout();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getMoveY() {
        return this.m_moveY;
    }
}

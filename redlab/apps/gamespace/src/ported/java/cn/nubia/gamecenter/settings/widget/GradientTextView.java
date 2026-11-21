package cn.nubia.gamecenter.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Property;
import android.view.View;
import androidx.core.view.ViewCompat;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;

/* loaded from: classes.dex */
public class GradientTextView extends View {
    public static final Property<GradientTextView, Float> CUST_NUMBER_GROW = new FloatProperty<GradientTextView>(AnimatorHelper.Item.CUST_NUMBER) { // from class: cn.nubia.gamecenter.settings.widget.GradientTextView.1
        @Override // android.util.FloatProperty
        public void setValue(GradientTextView gradientTextView, float f) {
            gradientTextView.setNumber(f);
        }

        @Override // android.util.Property
        public Float get(GradientTextView gradientTextView) {
            return Float.valueOf(gradientTextView.getNumber());
        }
    };
    private static final String TAG = "GradientTextView";
    private float m_baseCenterX;
    private float m_baseCenterY;
    private String m_baseNumber;
    private int m_colorEnd;
    private int m_colorStart;
    private LinearGradient m_gradient;
    private int m_gradientHeight;
    private int m_gradientWidth;
    private String m_nextNumber;
    private float m_number;
    Paint m_paint;
    private int m_textColor;
    private float m_textHeight;
    private float m_textSize;

    public GradientTextView(Context context) {
        this(context, null);
    }

    public GradientTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.m_baseCenterX = 0.0f;
        this.m_baseCenterY = 0.0f;
        this.m_textHeight = 0.0f;
        this.m_textSize = 30.0f;
        this.m_baseNumber = "";
        this.m_nextNumber = "";
    }

    public GradientTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.m_baseCenterX = 0.0f;
        this.m_baseCenterY = 0.0f;
        this.m_textHeight = 0.0f;
        this.m_textSize = 30.0f;
        this.m_baseNumber = "";
        this.m_nextNumber = "";
    }

    @Override // android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (getGradient() != null) {
            getPaint().setShader(getGradient());
        }
    }

    private void adjustBasePosition(int i, int i2) {
        float f = i / 2.0f;
        this.m_baseCenterX = f;
        Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
        this.m_baseCenterY = (i2 / 2.0f) + (((fontMetrics.bottom - fontMetrics.top) / 2.0f) - fontMetrics.bottom);
        this.m_baseCenterX = f;
        this.m_textHeight = fontMetrics.descent - fontMetrics.ascent;
    }

    public void setTextSize(float f) {
        this.m_textSize = f;
        reset();
    }

    private Paint getPaint() {
        if (this.m_paint == null) {
            Paint paint = new Paint();
            this.m_paint = paint;
            paint.setAntiAlias(true);
            this.m_paint.setTextSize(this.m_textSize);
            this.m_paint.setTextAlign(Paint.Align.CENTER);
            this.m_paint.setStyle(Paint.Style.FILL);
            this.m_paint.setStrokeWidth(8.0f);
            this.m_paint.setColor(getTextColor());
        }
        return this.m_paint;
    }

    private LinearGradient getGradient() {
        if (this.m_gradient != null && (this.m_gradientWidth != getWidth() || this.m_gradientHeight != getHeight())) {
            this.m_gradient = null;
        }
        if (this.m_gradient == null) {
            if (getHeight() == 0) {
                return null;
            }
            this.m_gradientWidth = getWidth();
            int height = getHeight();
            this.m_gradientHeight = height;
            adjustBasePosition(this.m_gradientWidth, height);
            this.m_gradient = new LinearGradient(0.0f, 0.0f, 0.0f, this.m_gradientHeight, new int[]{this.m_colorStart, this.m_textColor, this.m_colorEnd}, (float[]) null, Shader.TileMode.CLAMP);
        }
        return this.m_gradient;
    }

    public void setTextColor(int i) {
        this.m_textColor = i;
        int i2 = i & ViewCompat.MEASURED_SIZE_MASK;
        this.m_colorStart = i2;
        this.m_colorEnd = i2;
        reset();
    }

    private void reset() {
        this.m_gradient = null;
        this.m_paint = null;
        requestLayout();
    }

    public int getTextColor() {
        return this.m_textColor;
    }

    private String getBaseText() {
        return this.m_baseNumber;
    }

    private String getNextText() {
        return this.m_nextNumber;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        float f = this.m_number;
        getHeight();
        float f2 = this.m_baseCenterY * (1.0f - (f - ((int) f)));
        canvas.drawText(getBaseText(), this.m_baseCenterX, f2, getPaint());
        canvas.drawText(getNextText(), this.m_baseCenterX, f2 + this.m_textHeight, getPaint());
        canvas.restore();
    }

    public void setNumber(float f) {
        if (f < 0.0f) {
            this.m_number = 0.0f;
        } else {
            this.m_number = f;
        }
        int i = (int) f;
        this.m_baseNumber = Integer.toString(i);
        this.m_nextNumber = Integer.toString(i + 1);
        invalidate();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getNumber() {
        return this.m_number;
    }
}

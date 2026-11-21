package cn.nubia.gamepanel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.List;

/* loaded from: classes2.dex */
public class PowerPanelView extends View {
    private static final String TAG = "PowerPanelView";
    private int cpsLineWidth;
    private List<Integer> cpsValues;
    private int intervalMpm;
    private int intervalX;
    private int lineHeight;
    int[] mColorsEnd;
    int[] mColorsEndMpm;
    int[] mColorsStart;
    int[] mColorsStartMpm;
    private int mHeight;
    private Shader mShaderEnd;
    private Shader mShaderStart;
    private int maxValueY;
    private List<Integer> mpmValues;
    private int number;
    private Paint paintCps;
    private Paint paintEnd;
    private Paint paintMpm;
    private Paint paintStart;
    private int yValuesCount;

    private int getRealData(int i, int i2) {
        if (i == 0) {
            return -1;
        }
        if (i > 0 && i < i2) {
            return 0;
        }
        if (i2 <= i && i < i2 * 2) {
            return 1;
        }
        if (i2 * 2 <= i && i < i2 * 3) {
            return 2;
        }
        if (i2 * 3 <= i && i < i2 * 4) {
            return 3;
        }
        if (i2 * 4 <= i && i < i2 * 5) {
            return 4;
        }
        if (i2 * 5 <= i && i < i2 * 6) {
            return 5;
        }
        if (i2 * 6 <= i && i < i2 * 7) {
            return 6;
        }
        if (i2 * 7 <= i && i < i2 * 8) {
            return 7;
        }
        if (i2 * 8 > i || i >= i2 * 9) {
            return (i2 * 9 > i || i >= i2 * 10) ? 10 : 9;
        }
        return 8;
    }

    public PowerPanelView(Context context) {
        this(context, null);
    }

    public PowerPanelView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public PowerPanelView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.intervalX = 18;
        this.intervalMpm = 84;
        this.lineHeight = 3;
        this.cpsLineWidth = 12;
        this.maxValueY = 10;
        this.yValuesCount = 10;
        this.number = 10;
        this.mColorsStart = new int[]{Color.parseColor("#222635"), Color.parseColor("#76EF74")};
        this.mColorsEnd = new int[]{Color.parseColor("#76EF74"), Color.parseColor("#222635")};
        this.mColorsStartMpm = new int[]{Color.parseColor("#222635"), Color.parseColor("#FFEF74EB")};
        this.mColorsEndMpm = new int[]{Color.parseColor("#FFEF74EB"), Color.parseColor("#222635")};
        initPaint();
    }

    private void initPaint() {
        Paint paint = new Paint(1);
        this.paintCps = paint;
        paint.setColor(Color.parseColor("#FF76EF74"));
        Paint paint2 = new Paint(1);
        this.paintMpm = paint2;
        paint2.setColor(Color.parseColor("#FFEF74EB"));
        this.paintEnd = new Paint(1);
        this.paintStart = new Paint(1);
    }

    @Override // android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        if (z) {
            this.mHeight = getHeight() - (this.lineHeight * 2);
        }
        super.onLayout(z, i, i2, i3, i4);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        drawMpm(canvas);
        drawCps(canvas);
    }

    private void drawCps(Canvas canvas) {
        List<Integer> list = this.cpsValues;
        if (list == null || list.size() <= 0) {
            return;
        }
        canvas.save();
        for (int i = 0; i < this.cpsValues.size(); i++) {
            if (this.cpsValues.get(i).intValue() > this.maxValueY) {
                this.maxValueY = this.cpsValues.get(i).intValue();
            }
        }
        for (int i2 = 0; i2 < this.cpsValues.size(); i2++) {
            int realData = getRealData(MathUtils.divData(this.cpsValues.get(i2).intValue(), this.maxValueY, 2), this.number);
            if (i2 == 0) {
                int i3 = this.intervalX;
                int i4 = this.mHeight;
                int i5 = this.yValuesCount;
                LinearGradient linearGradient = new LinearGradient(i2 * i3, (i4 - ((realData * i4) / i5)) + this.lineHeight, (i3 * i2) + this.cpsLineWidth, i4 - ((realData * i4) / i5), this.mColorsStart, (float[]) null, Shader.TileMode.MIRROR);
                this.mShaderStart = linearGradient;
                this.paintStart.setShader(linearGradient);
                int i6 = this.intervalX;
                int i7 = this.mHeight;
                int i8 = this.yValuesCount;
                canvas.drawRect(i2 * i6, (i7 - ((realData * i7) / i8)) + this.lineHeight, (i6 * i2) + this.cpsLineWidth, i7 - ((realData * i7) / i8), this.paintStart);
            } else if (i2 == this.cpsValues.size() - 1) {
                int i9 = this.intervalX;
                int i10 = this.mHeight;
                int i11 = this.yValuesCount;
                LinearGradient linearGradient2 = new LinearGradient(i2 * i9, (i10 - ((realData * i10) / i11)) + this.lineHeight, (i9 * i2) + this.cpsLineWidth, i10 - ((realData * i10) / i11), this.mColorsEnd, (float[]) null, Shader.TileMode.MIRROR);
                this.mShaderEnd = linearGradient2;
                this.paintEnd.setShader(linearGradient2);
                int i12 = this.intervalX;
                int i13 = this.mHeight;
                int i14 = this.yValuesCount;
                canvas.drawRect(i2 * i12, (i13 - ((realData * i13) / i14)) + this.lineHeight, (i12 * i2) + this.cpsLineWidth, i13 - ((realData * i13) / i14), this.paintEnd);
            } else {
                int i15 = this.intervalX;
                int i16 = this.mHeight;
                int i17 = this.yValuesCount;
                canvas.drawRect(i2 * i15, (i16 - ((realData * i16) / i17)) + this.lineHeight, (i15 * i2) + this.cpsLineWidth, i16 - ((realData * i16) / i17), this.paintCps);
            }
        }
        canvas.restore();
    }

    private void drawMpm(Canvas canvas) {
        List<Integer> list = this.mpmValues;
        if (list == null || list.size() <= 0) {
            return;
        }
        canvas.save();
        for (int i = 0; i < this.mpmValues.size(); i++) {
            int realData = getRealData(this.mpmValues.get(i).intValue(), this.number);
            if (i == 0) {
                int i2 = this.mHeight;
                int i3 = this.yValuesCount;
                int i4 = this.lineHeight;
                LinearGradient linearGradient = new LinearGradient(0.0f, (i2 - ((realData * i2) / i3)) + (i4 * 2), 45.0f, (i2 - ((realData * i2) / i3)) + i4, this.mColorsStartMpm, (float[]) null, Shader.TileMode.MIRROR);
                this.mShaderStart = linearGradient;
                this.paintStart.setShader(linearGradient);
                int i5 = this.mHeight;
                int i6 = this.yValuesCount;
                int i7 = this.lineHeight;
                canvas.drawRect(0.0f, (i5 - ((realData * i5) / i6)) + (i7 * 2), 45.0f, (i5 - ((realData * i5) / i6)) + i7, this.paintStart);
            } else if (i == this.mpmValues.size() - 1) {
                int i8 = this.mHeight;
                int i9 = this.yValuesCount;
                int i10 = this.lineHeight;
                LinearGradient linearGradient2 = new LinearGradient(141.0f, (i8 - ((realData * i8) / i9)) + (i10 * 2), 225.0f, (i8 - ((realData * i8) / i9)) + i10, this.mColorsEndMpm, (float[]) null, Shader.TileMode.MIRROR);
                this.mShaderEnd = linearGradient2;
                this.paintEnd.setShader(linearGradient2);
                int i11 = this.mHeight;
                int i12 = this.yValuesCount;
                int i13 = this.lineHeight;
                canvas.drawRect(141.0f, (i11 - ((realData * i11) / i12)) + (i13 * 2), 225.0f, (i11 - ((realData * i11) / i12)) + i13, this.paintEnd);
            } else {
                int i14 = this.mHeight;
                int i15 = this.yValuesCount;
                int i16 = this.lineHeight;
                canvas.drawRect(51.0f, (i14 - ((realData * i14) / i15)) + (i16 * 2), 135.0f, (i14 - ((realData * i14) / i15)) + i16, this.paintMpm);
            }
        }
        canvas.restore();
    }

    public void setMpmValues(List<Integer> list) {
        this.mpmValues = list;
    }

    public void setCpsValues(List<Integer> list) {
        this.cpsValues = list;
    }
}

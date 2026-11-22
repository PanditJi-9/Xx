package cn.nubia.gamecenter.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import cn.nubia.gamecenter.settings.widget.ClipImageView;

/* loaded from: classes.dex */
public class LinesView extends View implements ClipImageView.Callback {
    private static final int[] PERCENT = {25, 65};
    private static final String TAG = "LinesView";
    private Paint mLinePaint;
    private int[][] m_endsPoints;
    private int[][] m_lines;

    public LinesView(Context context) {
        this(context, null);
    }

    public LinesView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.m_endsPoints = new int[][]{null, null, null, null};
    }

    public LinesView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.m_endsPoints = new int[][]{null, null, null, null};
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        drawLines(canvas, getPaint());
        super.onDraw(canvas);
    }

    private Paint getPaint() {
        if (this.mLinePaint == null) {
            Paint paint = new Paint();
            this.mLinePaint = paint;
            paint.setStyle(Paint.Style.STROKE);
            this.mLinePaint.setStrokeWidth(2.0f);
            this.mLinePaint.setAntiAlias(true);
            this.mLinePaint.setColor(-4227972);
        }
        return this.mLinePaint;
    }

    @Override // cn.nubia.gamecenter.settings.widget.ClipImageView.Callback
    public void setEndsPoint(int[] iArr, int i) {
        if (i >= 0) {
            int[][] iArr2 = this.m_endsPoints;
            if (i >= iArr2.length) {
                return;
            }
            if (iArr == null || iArr.length == 2) {
                iArr2[i] = iArr;
                this.m_lines = null;
                invalidate();
            }
        }
    }

    private int[][] getLines() {
        if (this.m_lines == null) {
            int[][] iArr = this.m_endsPoints;
            if (iArr == null || iArr.length != 4) {
                return null;
            }
            int width = getWidth();
            int height = getHeight();
            int[] iArr2 = PERCENT;
            this.m_lines = new int[][]{getOneLine(0, (iArr2[0] * height) / 100, this.m_endsPoints[0]), getOneLine(0, (iArr2[1] * height) / 100, this.m_endsPoints[1]), getOneLine(this.m_endsPoints[2], width, (iArr2[0] * height) / 100), getOneLine(this.m_endsPoints[3], width, (height * iArr2[1]) / 100)};
        }
        return this.m_lines;
    }

    private int[] getOneLine(int i, int i2, int[] iArr) {
        if (iArr == null || iArr.length != 2) {
            return null;
        }
        return getOneLine(i, i2, iArr[0], iArr[1], false);
    }

    private int[] getOneLine(int[] iArr, int i, int i2) {
        if (iArr == null || iArr.length != 2) {
            return null;
        }
        return getOneLine(iArr[0], iArr[1], i, i2, true);
    }

    private int[] getOneLine(int i, int i2, int i3, int i4, boolean z) {
        int i5 = i4 - i2;
        if (i5 < 0) {
            i5 = -i5;
        }
        int i6 = i3 - i;
        if (i6 < 0) {
            i6 = -i6;
        }
        if (i6 <= i5) {
            return new int[]{i, i2, i3, i4, i3, i4};
        }
        if (z) {
            return new int[]{i, i2, i + i5, i4, i3, i4};
        }
        return new int[]{i, i2, i3 - i5, i2, i3, i4};
    }

    private void drawLines(Canvas canvas, Paint paint) {
        int[][] lines = getLines();
        if (lines == null) {
            return;
        }
        for (int[] iArr : lines) {
            drawLine(canvas, paint, iArr);
        }
    }

    private void drawLine(Canvas canvas, Paint paint, int[] iArr) {
        if (iArr == null || iArr.length != 6) {
            return;
        }
        canvas.drawLine(iArr[0], iArr[1], iArr[2], iArr[3], paint);
        canvas.drawLine(iArr[2], iArr[3], iArr[4], iArr[5], paint);
    }
}

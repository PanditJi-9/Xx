package cn.nubia.gamecenter.settings.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.IntProperty;
import android.widget.ImageView;
import cn.nubia.gamecenter.settings.helper.AnimatorHelper;

/* loaded from: classes.dex */
public class ClipImageView extends ImageView {
    public static final IntProperty<ClipImageView> CUST_PERCENT_END = new IntProperty<ClipImageView>(AnimatorHelper.Item.CUST_PERCENT_END) { // from class: cn.nubia.gamecenter.settings.widget.ClipImageView.1
        @Override // android.util.IntProperty
        public void setValue(ClipImageView clipImageView, int i) {
            clipImageView.setDegreeEnd(i);
        }

        @Override // android.util.Property
        public Integer get(ClipImageView clipImageView) {
            return Integer.valueOf(clipImageView.getDegreeEnd());
        }
    };
    private static final String TAG = "ClipImageView";
    private Callback m_callback;
    Path m_clip;
    private int m_degreeEnd;
    private int m_degreeStart;
    private int m_index;

    public interface Callback {
        void setEndsPoint(int[] iArr, int i);
    }

    public ClipImageView(Context context) {
        this(context, null);
    }

    public ClipImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.m_degreeStart = 0;
        this.m_degreeEnd = 0;
        this.m_index = -1;
    }

    public ClipImageView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.m_degreeStart = 0;
        this.m_degreeEnd = 0;
        this.m_index = -1;
    }

    public void setDegree(int i, int i2) {
        if (i > i2) {
            return;
        }
        this.m_degreeStart = i;
        this.m_degreeEnd = i2;
        requestLayout();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setDegreeEnd(int i) {
        setDegree(this.m_degreeStart, i);
    }

    public int getDegreeStart() {
        return this.m_degreeStart;
    }

    public int getDegreeEnd() {
        return this.m_degreeEnd;
    }

    private int[] getCenterPoint() {
        if (this.m_degreeStart >= this.m_degreeEnd) {
            return null;
        }
        int width = getWidth() / 2;
        int i = (width * 2) / 3;
        float f = width;
        double d = (((-((this.m_degreeStart + this.m_degreeEnd) / 2)) - 90.0d) * 3.141592653589793d) / 180.0d;
        double d2 = i;
        return new int[]{getLeft() + ((int) (f + ((float) (Math.cos(d) * d2)))), getTop() + ((int) (((float) (Math.sin(d) * d2)) + f))};
    }

    private Path getPath() {
        if (this.m_degreeEnd - this.m_degreeStart > 359) {
            return null;
        }
        if (this.m_clip == null) {
            int width = getWidth() / 2;
            Path path = new Path();
            this.m_clip = path;
            float f = 0;
            path.moveTo(f, f);
            double d = width;
            this.m_clip.lineTo(((float) (Math.cos(((-this.m_degreeStart) * 3.141592653589793d) / 180.0d) * d)) + f, ((float) (Math.sin(((-this.m_degreeStart) * 3.141592653589793d) / 180.0d) * d)) + f);
            int[] edgePointForDegree = getEdgePointForDegree(this.m_degreeStart, this.m_degreeEnd);
            if (edgePointForDegree != null && edgePointForDegree.length >= 6) {
                this.m_clip.lineTo(edgePointForDegree[0] * width, 0 - (edgePointForDegree[1] * width));
                this.m_clip.lineTo(edgePointForDegree[2] * width, 0 - (edgePointForDegree[3] * width));
                this.m_clip.lineTo(edgePointForDegree[4] * width, 0 - (edgePointForDegree[5] * width));
                if (edgePointForDegree.length >= 8) {
                    this.m_clip.lineTo(edgePointForDegree[6] * width, 0 - (edgePointForDegree[7] * width));
                }
            }
            this.m_clip.lineTo(((float) (Math.cos(((-this.m_degreeEnd) * 3.141592653589793d) / 180.0d) * d)) + f, f + ((float) (Math.sin(((-this.m_degreeEnd) * 3.141592653589793d) / 180.0d) * d)));
            this.m_clip.close();
        }
        return this.m_clip;
    }

    private int[] getEdgePointForDegree(int i, int i2) {
        if (this.m_degreeEnd - this.m_degreeStart <= 180) {
            return getEdgePointForAcuteDegree(i, i2);
        }
        return getEdgePointForObtuseDegree(i, i2);
    }

    private int[] getEdgePointForObtuseDegree(int i, int i2) {
        int i3;
        int i4;
        int i5 = (i < 0 || i > 90) ? -1 : 1;
        if (i2 >= 180 && i2 <= 270) {
            i3 = 1;
            i4 = -1;
        } else {
            if (i < 90) {
                return new int[]{1, i5, -1, 1, -1, -1, 1, -1};
            }
            i3 = -1;
            i4 = 1;
        }
        return new int[]{1, i5, -1, i3, i4, -1};
    }

    private int[] getEdgePointForAcuteDegree(int i, int i2) {
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        if (i < 0 || i > 90) {
            if (i > 90 && i <= 180) {
                i4 = -1;
                i3 = 1;
            } else if (i <= 180 || i > 270) {
                i3 = -1;
                i4 = 1;
            } else {
                i3 = -1;
                i4 = -1;
            }
            if (i2 < 0 && i2 <= 90) {
                i5 = 1;
                i6 = 1;
                i8 = 1;
                i7 = 1;
            } else if (i2 <= 90 && i2 <= 180) {
                i5 = -1;
                i8 = -1;
                i6 = 1;
                i7 = 1;
            } else if (i2 > 180 || i2 > 270) {
                i5 = (i >= 90 || i > 180) ? 1 : -1;
                i6 = -1;
                i7 = -1;
                i8 = 1;
            } else {
                i5 = -1;
                i8 = -1;
                i7 = -1;
                i6 = (i < 0 || i > 90) ? -1 : 1;
            }
            return new int[]{i4, i3, i5, i6, i8, i7};
        }
        i4 = 1;
        i3 = i4;
        if (i2 < 0) {
        }
        if (i2 <= 90) {
        }
        if (i2 > 180) {
        }
        i5 = (i >= 90 || i > 180) ? 1 : -1;
        i6 = -1;
        i7 = -1;
        i8 = 1;
        return new int[]{i4, i3, i5, i6, i8, i7};
    }

    private void clearPath() {
        this.m_clip = null;
    }

    @Override // android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        clearPath();
        notifyUpdate();
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        canvas.save();
        if (getPath() != null) {
            int width = getWidth() / 2;
            float f = width;
            canvas.translate(f, f);
            canvas.rotate(-90.0f);
            canvas.clipPath(getPath());
            canvas.rotate(90.0f);
            float f2 = -width;
            canvas.translate(f2, f2);
        }
        super.draw(canvas);
        canvas.restore();
    }

    public void setCallback(Callback callback, int i) {
        this.m_index = i;
        this.m_callback = callback;
    }

    private void notifyUpdate() {
        if (this.m_callback == null || this.m_index == -1 || getWidth() == 0) {
            return;
        }
        this.m_callback.setEndsPoint(getCenterPoint(), this.m_index);
    }
}

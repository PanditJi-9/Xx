package cn.nubia.plug.view.plugRatingBarView;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/* loaded from: classes2.dex */
public class PlugScoreDrawable extends Drawable {
    private ColorFilter mColorFilter;
    private Drawable mDrawable;
    private PorterDuffColorFilter mTintFilter;
    private ColorStateList mTintList;
    int mAlpha = 255;
    private PorterDuff.Mode mTintMode = PorterDuff.Mode.SRC_IN;
    private int mTileCount = -1;
    private CustomConstantState mConstantState = new CustomConstantState();

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -3;
    }

    public PlugScoreDrawable(Drawable drawable) {
        this.mDrawable = drawable;
    }

    public Drawable getDrawable() {
        return this.mDrawable;
    }

    public void setTileCount(int i) {
        this.mTileCount = i;
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public int getAlpha() {
        return this.mAlpha;
    }

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int i) {
        if (this.mAlpha != i) {
            this.mAlpha = i;
            invalidateSelf();
        }
    }

    @Override // android.graphics.drawable.Drawable
    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
        this.mColorFilter = colorFilter;
        invalidateSelf();
    }

    @Override // android.graphics.drawable.Drawable
    public void setTint(int i) {
        setTintList(ColorStateList.valueOf(i));
    }

    @Override // android.graphics.drawable.Drawable
    public void setTintList(ColorStateList colorStateList) {
        this.mTintList = colorStateList;
        if (updateTintFilter()) {
            invalidateSelf();
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void setTintMode(PorterDuff.Mode mode) {
        this.mTintMode = mode;
        if (updateTintFilter()) {
            invalidateSelf();
        }
    }

    @Override // android.graphics.drawable.Drawable
    public boolean isStateful() {
        ColorStateList colorStateList = this.mTintList;
        return colorStateList != null && colorStateList.isStateful();
    }

    @Override // android.graphics.drawable.Drawable
    protected boolean onStateChange(int[] iArr) {
        return updateTintFilter();
    }

    private boolean updateTintFilter() {
        ColorStateList colorStateList = this.mTintList;
        if (colorStateList == null || this.mTintMode == null) {
            boolean z = this.mTintFilter != null;
            this.mTintFilter = null;
            return z;
        }
        this.mTintFilter = new PorterDuffColorFilter(colorStateList.getColorForState(getState(), 0), this.mTintMode);
        return true;
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.width() == 0 || bounds.height() == 0) {
            return;
        }
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        onDraw(canvas, bounds.width(), bounds.height());
        canvas.restoreToCount(save);
    }

    @Override // android.graphics.drawable.Drawable
    public Drawable mutate() {
        this.mDrawable = this.mDrawable.mutate();
        return this;
    }

    @Override // android.graphics.drawable.Drawable
    public Drawable.ConstantState getConstantState() {
        return this.mConstantState;
    }

    private class CustomConstantState extends Drawable.ConstantState {
        @Override // android.graphics.drawable.Drawable.ConstantState
        public int getChangingConfigurations() {
            return 0;
        }

        private CustomConstantState() {
        }

        @Override // android.graphics.drawable.Drawable.ConstantState
        public Drawable newDrawable() {
            return PlugScoreDrawable.this;
        }
    }

    protected ColorFilter getColorFilterForDrawing() {
        ColorFilter colorFilter = this.mColorFilter;
        return colorFilter != null ? colorFilter : this.mTintFilter;
    }

    protected void onDraw(Canvas canvas, int i, int i2) {
        this.mDrawable.setAlpha(this.mAlpha);
        ColorFilter colorFilterForDrawing = getColorFilterForDrawing();
        if (colorFilterForDrawing != null) {
            this.mDrawable.setColorFilter(colorFilterForDrawing);
        }
        int intrinsicHeight = this.mDrawable.getIntrinsicHeight();
        float f = i2 / intrinsicHeight;
        canvas.scale(f, f);
        float f2 = i / f;
        int i3 = this.mTileCount;
        if (i3 < 0) {
            int intrinsicWidth = this.mDrawable.getIntrinsicWidth();
            int i4 = 0;
            while (i4 < f2) {
                int i5 = i4 + intrinsicWidth;
                this.mDrawable.setBounds(i4, 0, i5, intrinsicHeight);
                this.mDrawable.draw(canvas);
                i4 = i5;
            }
            return;
        }
        float f3 = f2 / i3;
        for (int i6 = 0; i6 < this.mTileCount; i6++) {
            float f4 = (i6 + 0.5f) * f3;
            float intrinsicWidth2 = this.mDrawable.getIntrinsicWidth() / 2.0f;
            this.mDrawable.setBounds(Math.round(f4 - intrinsicWidth2), 0, Math.round(f4 + intrinsicWidth2), intrinsicHeight);
            this.mDrawable.draw(canvas);
        }
    }
}

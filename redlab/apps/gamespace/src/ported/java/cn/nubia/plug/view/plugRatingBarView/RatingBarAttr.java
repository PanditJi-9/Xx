package cn.nubia.plug.view.plugRatingBarView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class RatingBarAttr {
    private ColorStateList mBarColor;
    private int mBarCount;
    private int mBarDrawable;
    private Context mContext;
    private ColorStateList mDefaultColor;
    private int mDefaultDrawable;

    public RatingBarAttr(Context context, int i, int i2, int i3, ColorStateList colorStateList, ColorStateList colorStateList2) {
        this.mContext = context;
        this.mBarCount = i;
        this.mDefaultDrawable = i2;
        this.mBarDrawable = i3;
        this.mDefaultColor = colorStateList;
        this.mBarColor = colorStateList2;
    }

    public Drawable[] getLayerList() {
        return new Drawable[]{createLayerDrawableWithTintAttrRes(this.mDefaultDrawable, R.attr.colorControlHighlight), createClippedLayerDrawableWithTintColor(this.mBarDrawable, 0), createClippedLayerDrawableWithTintAttrRes(this.mBarDrawable, R.attr.colorControlActivated)};
    }

    private Drawable createLayerDrawableWithTintAttrRes(int i, int i2) {
        return createLayerDrawableWithTintAttrColor(i, getColorFromAttrRes(i2));
    }

    private Drawable createClippedLayerDrawableWithTintColor(int i, int i2) {
        return new ClipDrawable(createLayerDrawableWithTintAttrColor(i, i2), 3, 1);
    }

    private Drawable createLayerDrawableWithTintAttrColor(int i, int i2) {
        PlugScoreDrawable plugScoreDrawable = new PlugScoreDrawable(ContextCompat.getDrawable(this.mContext, i));
        plugScoreDrawable.mutate();
        if (i2 != -1) {
            plugScoreDrawable.setTint(i2);
        }
        return plugScoreDrawable;
    }

    private Drawable createClippedLayerDrawableWithTintAttrRes(int i, int i2) {
        return new ClipDrawable(createLayerDrawableWithTintAttrRes(i, i2), 3, 1);
    }

    private int getColorFromAttrRes(int i) {
        TypedArray obtainStyledAttributes = this.mContext.obtainStyledAttributes(new int[]{i});
        try {
            return obtainStyledAttributes.getColor(0, 0);
        } finally {
            obtainStyledAttributes.recycle();
        }
    }

    public int getBarCount() {
        return this.mBarCount;
    }

    public ColorStateList getDefaultColor() {
        return this.mDefaultColor;
    }

    public ColorStateList getBarColor() {
        return this.mBarColor;
    }
}

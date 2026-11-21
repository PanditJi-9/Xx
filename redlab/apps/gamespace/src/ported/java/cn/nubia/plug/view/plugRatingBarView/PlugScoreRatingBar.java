package cn.nubia.plug.view.plugRatingBarView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RatingBar;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class PlugScoreRatingBar extends RatingBar implements RatingBar.OnRatingBarChangeListener {
    private ColorStateList mBarColor;
    private float mBarDistance;
    private int mBarDrawable;
    private ColorStateList mDefaultColor;
    private int mDefaultDrawable;
    private RatingBarDrawable mDrawable;
    private OnRatingChangeListener mOnRatingChangeListener;
    private float mTempRating;

    public interface OnRatingChangeListener {
        void onRatingChanged(PlugScoreRatingBar plugScoreRatingBar, float f, boolean z);
    }

    public PlugScoreRatingBar(Context context) {
        this(context, null);
    }

    public PlugScoreRatingBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet, 0);
    }

    public PlugScoreRatingBar(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context, attributeSet, i);
    }

    @Override // android.widget.RatingBar.OnRatingBarChangeListener
    public void onRatingChanged(RatingBar ratingBar, float f, boolean z) {
        OnRatingChangeListener onRatingChangeListener = this.mOnRatingChangeListener;
        if (onRatingChangeListener != null && f != this.mTempRating) {
            onRatingChangeListener.onRatingChanged(this, f, z);
        }
        this.mTempRating = f;
    }

    @Override // android.widget.RatingBar
    public void setNumStars(int i) {
        super.setNumStars(i);
        RatingBarDrawable ratingBarDrawable = this.mDrawable;
        if (ratingBarDrawable != null) {
            ratingBarDrawable.setBarCount(i);
        }
    }

    @Override // android.widget.RatingBar, android.widget.AbsSeekBar, android.widget.ProgressBar, android.view.View
    protected synchronized void onMeasure(int i, int i2) {
        super.onMeasure(i, i2);
        int measuredHeight = getMeasuredHeight();
        setMeasuredDimension(resolveSizeAndState(Math.round(measuredHeight * this.mDrawable.getTileRatio() * getNumStars()) + ((int) ((getNumStars() - 1) * this.mBarDistance)), i, 0), measuredHeight);
    }

    private void init(Context context, AttributeSet attributeSet, int i) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.PlugScoreRatingBar, i, 0);
        if (obtainStyledAttributes.hasValue(0)) {
            this.mBarColor = obtainStyledAttributes.getColorStateList(0);
        }
        if (obtainStyledAttributes.hasValue(3)) {
            this.mDefaultColor = obtainStyledAttributes.getColorStateList(3);
        }
        this.mBarDistance = obtainStyledAttributes.getDimension(1, 0.0f);
        this.mBarDrawable = obtainStyledAttributes.getResourceId(2, R.drawable.plug_rating_bar_bg);
        if (obtainStyledAttributes.hasValue(4)) {
            this.mDefaultDrawable = obtainStyledAttributes.getResourceId(4, R.drawable.plug_rating_bar_bg);
        } else {
            this.mDefaultDrawable = this.mBarDrawable;
        }
        obtainStyledAttributes.recycle();
        RatingBarDrawable ratingBarDrawable = new RatingBarDrawable(new RatingBarAttr(context, getNumStars(), this.mDefaultDrawable, this.mBarDrawable, this.mDefaultColor, this.mBarColor));
        this.mDrawable = ratingBarDrawable;
        setProgressDrawable(ratingBarDrawable);
    }
}

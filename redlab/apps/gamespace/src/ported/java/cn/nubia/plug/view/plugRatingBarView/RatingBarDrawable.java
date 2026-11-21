package cn.nubia.plug.view.plugRatingBarView;

import android.R;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

/* loaded from: classes2.dex */
public class RatingBarDrawable extends LayerDrawable {
    private static final String FIELD_DRAWABLE = "mDrawable";
    private static final String FIELD_STATE = "mState";
    private static final String FIELD_STATE_CLIP = "mClipState";

    public RatingBarDrawable(RatingBarAttr ratingBarAttr) {
        super(ratingBarAttr.getLayerList());
        setId(0, R.id.background);
        setId(1, R.id.secondaryProgress);
        setId(2, R.id.progress);
        initStyle(ratingBarAttr);
    }

    private void initStyle(RatingBarAttr ratingBarAttr) {
        PlugScoreDrawable tileDrawableByLayerId = getTileDrawableByLayerId(R.id.background);
        PlugScoreDrawable tileDrawableByLayerId2 = getTileDrawableByLayerId(R.id.secondaryProgress);
        PlugScoreDrawable tileDrawableByLayerId3 = getTileDrawableByLayerId(R.id.progress);
        tileDrawableByLayerId.setTileCount(ratingBarAttr.getBarCount());
        tileDrawableByLayerId2.setTileCount(ratingBarAttr.getBarCount());
        tileDrawableByLayerId3.setTileCount(ratingBarAttr.getBarCount());
        if (ratingBarAttr.getDefaultColor() != null) {
            tileDrawableByLayerId.setTintList(ratingBarAttr.getDefaultColor());
        }
        if (ratingBarAttr.getBarColor() != null) {
            tileDrawableByLayerId3.setTintList(ratingBarAttr.getBarColor());
        }
    }

    public float getTileRatio() {
        Drawable drawable = getTileDrawableByLayerId(R.id.progress).getDrawable();
        return drawable.getIntrinsicWidth() / drawable.getIntrinsicHeight();
    }

    public void setBarCount(int i) {
        getTileDrawableByLayerId(R.id.background).setTileCount(i);
        getTileDrawableByLayerId(R.id.secondaryProgress).setTileCount(i);
        getTileDrawableByLayerId(R.id.progress).setTileCount(i);
    }

    private PlugScoreDrawable getTileDrawableByLayerId(int i) {
        Drawable findDrawableByLayerId = findDrawableByLayerId(i);
        if (i == 16908288) {
            return (PlugScoreDrawable) findDrawableByLayerId;
        }
        if (i == 16908301 || i == 16908303) {
            return (PlugScoreDrawable) ((ClipDrawable) findDrawableByLayerId).getDrawable();
        }
        throw new RuntimeException();
    }
}

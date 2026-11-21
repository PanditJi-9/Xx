package dev.redlab.gamespace.layoutmanager;

import android.content.Context;
import android.util.Log;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public class AutoAdjustColumnGridLayoutManager extends GridLayoutManager {
    private static final String TAG = "HostModeGrid";
    int mHorizontalMargin;
    int mItemWidth;

    public AutoAdjustColumnGridLayoutManager(Context context, int i) {
        super(context, 1);
        this.mItemWidth = i;
        this.mHorizontalMargin = context.getResources().getDimensionPixelOffset(R.dimen.host_item_card_margin) * 2;
    }

    @Override // androidx.recyclerview.widget.GridLayoutManager, androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        updateSpanCount();
        super.onLayoutChildren(recycler, state);
    }

    private void updateSpanCount() {
        Log.d(TAG, "updateSpanCount() getWidth() : " + getWidth() + ", mHorizontalMargin : " + this.mHorizontalMargin + ", itemWidth : " + this.mItemWidth);
        int width = (getWidth() - this.mHorizontalMargin) / this.mItemWidth;
        if (width < 1) {
            width = 1;
        }
        setSpanCount(width);
    }
}

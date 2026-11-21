package dev.redlab.gamespace.layoutmanager;

import android.content.Context;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/* loaded from: classes.dex */
public class HandheldLayoutManager extends GridLayoutManager {
    public static final int MAX_COUNT = 10;
    private static final String TAG = "HandheldLayoutManager";
    private boolean isExpand;
    int mHorizontalMargin;
    int mItemWidth;

    public HandheldLayoutManager(Context context, int i) {
        super(context, i);
        this.isExpand = false;
    }

    @Override // androidx.recyclerview.widget.GridLayoutManager, androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        updateSpanCount();
        super.onLayoutChildren(recycler, state);
    }

    private void updateSpanCount() {
        setSpanCount(getCount());
    }

    private int getCount() {
        return isExpand() ? 6 : 1;
    }

    public void setExpand(boolean z) {
        if (z == this.isExpand) {
            return;
        }
        this.isExpand = z;
        setOrientation(z ? 1 : 0);
        updateSpanCount();
    }

    public boolean isExpand() {
        return this.isExpand;
    }
}

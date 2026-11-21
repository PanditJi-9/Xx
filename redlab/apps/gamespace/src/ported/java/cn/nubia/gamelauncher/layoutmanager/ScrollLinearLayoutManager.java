package dev.redlab.gamespace.layoutmanager;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;

/* loaded from: classes.dex */
public class ScrollLinearLayoutManager extends LinearLayoutManager {
    private boolean mScrollEnable;

    public ScrollLinearLayoutManager(Context context) {
        super(context);
        this.mScrollEnable = true;
    }

    @Override // androidx.recyclerview.widget.LinearLayoutManager, androidx.recyclerview.widget.RecyclerView.LayoutManager
    public boolean canScrollVertically() {
        if (this.mScrollEnable) {
            return super.canScrollVertically();
        }
        return false;
    }

    public void setScrollEnable(boolean z) {
        this.mScrollEnable = z;
    }
}

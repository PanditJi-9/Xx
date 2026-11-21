package dev.redlab.gamespace.recycler;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import dev.redlab.gamespace.util.LogUtil;

/* loaded from: classes.dex */
public class BannerRecyclerView extends RecyclerView {
    int mBannerWidth;
    Runnable mIdleRunnable;
    ScrollHelper mScrollHelper;
    ScrollListener mScrollListener;
    int mScrolledX;
    Runnable mScrollingRunnable;

    public BannerRecyclerView(Context context) {
        super(context);
        this.mScrolledX = 0;
        init(context);
    }

    public BannerRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mScrolledX = 0;
        init(context);
    }

    public BannerRecyclerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mScrolledX = 0;
        init(context);
    }

    private void init(Context context) {
        if (this.mScrollHelper == null) {
            this.mScrollHelper = new ScrollHelper(context);
        }
        if (this.mScrollListener == null) {
            this.mScrollListener = new ScrollListener();
        }
    }

    @Override // androidx.recyclerview.widget.RecyclerView, android.view.ViewGroup
    public boolean drawChild(Canvas canvas, View view, long j) {
        try {
            return super.drawChild(canvas, view, j);
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.w("BannerRecyclerView", "drawChild Exception e = " + e);
            return false;
        }
    }

    class ScrollListener extends RecyclerView.OnScrollListener {
        ScrollListener() {
        }

        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
        public void onScrollStateChanged(RecyclerView recyclerView, int i) {
            super.onScrollStateChanged(recyclerView, i);
            if (i == 0) {
                BannerRecyclerView.this.mScrolledX = 0;
                if (BannerRecyclerView.this.mIdleRunnable != null) {
                    BannerRecyclerView.this.mIdleRunnable.run();
                    return;
                }
                return;
            }
            if (BannerRecyclerView.this.mScrollingRunnable != null) {
                BannerRecyclerView.this.mScrollingRunnable.run();
            }
        }

        @Override // androidx.recyclerview.widget.RecyclerView.OnScrollListener
        public void onScrolled(RecyclerView recyclerView, int i, int i2) {
            super.onScrolled(recyclerView, i, i2);
            BannerRecyclerView.this.mScrolledX += i;
        }
    }
}

package cn.nubia.gamecenter.settings.preference;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/* loaded from: classes.dex */
public class GameCenterDividerGridItemDecoration extends RecyclerView.ItemDecoration {
    private static final int[] ATTRS = {R.attr.listDivider};
    private Context mContext;
    private Drawable mDivider;

    public GameCenterDividerGridItemDecoration(Context context) {
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(ATTRS);
        this.mContext = context;
        this.mDivider = ContextCompat.getDrawable(context, cn.nubia.gamecenter.settings.R.drawable.gamespace_divider);
        obtainStyledAttributes.recycle();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.ItemDecoration
    public void onDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.State state) {
        drawHorizontal(canvas, recyclerView);
    }

    private int getSpanCount(RecyclerView recyclerView) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount();
        }
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            return ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
        }
        return -1;
    }

    public void drawHorizontal(Canvas canvas, RecyclerView recyclerView) {
        int childCount = recyclerView.getChildCount();
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(cn.nubia.gamecenter.settings.R.dimen.gcs_divider_padding_left1);
        int dimensionPixelSize2 = this.mContext.getResources().getDimensionPixelSize(cn.nubia.gamecenter.settings.R.dimen.gcs_divider_padding_right1);
        for (int i = 0; i < childCount; i++) {
            View childAt = recyclerView.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) childAt.getLayoutParams();
            int left = childAt.getLeft() - layoutParams.leftMargin;
            int right = childAt.getRight() + layoutParams.rightMargin + this.mDivider.getIntrinsicWidth();
            int bottom = childAt.getBottom() + layoutParams.bottomMargin;
            this.mDivider.setBounds(left + dimensionPixelSize, bottom, right - dimensionPixelSize2, this.mDivider.getIntrinsicHeight() + bottom);
            this.mDivider.draw(canvas);
        }
    }

    public void drawVertical(Canvas canvas, RecyclerView recyclerView) {
        int childCount = recyclerView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = recyclerView.getChildAt(i);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) childAt.getLayoutParams();
            int top = childAt.getTop() - layoutParams.topMargin;
            int bottom = childAt.getBottom() + layoutParams.bottomMargin;
            int right = childAt.getRight() + layoutParams.rightMargin;
            this.mDivider.setBounds(right, top, this.mDivider.getIntrinsicWidth() + right, bottom);
            this.mDivider.draw(canvas);
        }
    }

    private boolean isLastColum(RecyclerView recyclerView, int i, int i2, int i3) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        return layoutManager instanceof GridLayoutManager ? (i + 1) % i2 == 0 : layoutManager instanceof StaggeredGridLayoutManager ? ((StaggeredGridLayoutManager) layoutManager).getOrientation() == 1 && (i + 1) % i2 == 0 : i >= i3 - (i3 % i2);
    }

    private boolean isLastRaw(RecyclerView recyclerView, int i, int i2, int i3) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager instanceof GridLayoutManager) {
            return i >= i3 - (i3 % i2);
        }
        if (layoutManager instanceof StaggeredGridLayoutManager) {
            return ((StaggeredGridLayoutManager) layoutManager).getOrientation() == 1 ? i >= i3 - (i3 % i2) : (i + 1) % i2 == 0;
        }
        return false;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.ItemDecoration
    public void getItemOffsets(Rect rect, int i, RecyclerView recyclerView) {
        int spanCount = getSpanCount(recyclerView);
        int itemCount = recyclerView.getAdapter().getItemCount();
        if (isLastRaw(recyclerView, i, spanCount, itemCount)) {
            rect.set(0, 0, this.mDivider.getIntrinsicWidth(), 0);
        } else if (isLastColum(recyclerView, i, spanCount, itemCount)) {
            rect.set(0, 0, 0, this.mDivider.getIntrinsicHeight());
        } else {
            rect.set(0, 0, this.mDivider.getIntrinsicWidth(), this.mDivider.getIntrinsicHeight());
        }
    }
}

package dev.redlab.gamespace.gamelist;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

/* loaded from: classes.dex */
public class GameItemDecoration extends RecyclerView.ItemDecoration {
    private int leftRight;
    private int topBottom;

    public GameItemDecoration(int i, int i2) {
        this.leftRight = i;
        this.topBottom = i2;
    }

    @Override // androidx.recyclerview.widget.RecyclerView.ItemDecoration
    public void getItemOffsets(Rect rect, View view, RecyclerView recyclerView, RecyclerView.State state) {
        super.getItemOffsets(rect, view, recyclerView, state);
        rect.left = this.leftRight;
        rect.right = this.leftRight;
        rect.bottom = this.topBottom;
        rect.top = this.topBottom;
    }
}

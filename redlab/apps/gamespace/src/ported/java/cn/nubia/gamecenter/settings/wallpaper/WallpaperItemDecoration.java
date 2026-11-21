package cn.nubia.gamecenter.settings.wallpaper;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import cn.nubia.gamecenter.settings.R;

/* loaded from: classes.dex */
public class WallpaperItemDecoration extends RecyclerView.ItemDecoration {
    @Override // androidx.recyclerview.widget.RecyclerView.ItemDecoration
    public void getItemOffsets(Rect rect, View view, RecyclerView recyclerView, RecyclerView.State state) {
        super.getItemOffsets(rect, view, recyclerView, state);
        if (recyclerView.getChildAdapterPosition(view) <= 1) {
            rect.top = view.getResources().getDimensionPixelSize(R.dimen.gcs_wallpaper_list_top);
        }
    }
}

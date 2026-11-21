package dev.redlab.gamespace.gamelist;

import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import java.util.List;

/* loaded from: classes.dex */
public class GameListPagerAdapter extends PagerAdapter {
    private List<RecyclerView> mViewList;

    @Override // androidx.viewpager.widget.PagerAdapter
    public boolean isViewFromObject(View view, Object obj) {
        return view == obj;
    }

    public GameListPagerAdapter(List<RecyclerView> list) {
        this.mViewList = list;
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        viewGroup.removeView(this.mViewList.get(i));
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public Object instantiateItem(ViewGroup viewGroup, int i) {
        viewGroup.addView(this.mViewList.get(i));
        return this.mViewList.get(i);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public int getCount() {
        List<RecyclerView> list = this.mViewList;
        if (list == null) {
            return 0;
        }
        return list.size();
    }
}

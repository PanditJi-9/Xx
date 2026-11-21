package dev.redlab.gamespace.redmagicplanet.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.viewpager.widget.PagerAdapter;
import dev.redlab.gamespace.R;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class GameAgentViewPagerAdapter extends PagerAdapter {
    private Context context;
    private List<Integer> placeholderImages;

    @Override // androidx.viewpager.widget.PagerAdapter
    public boolean isViewFromObject(View view, Object obj) {
        return view == obj;
    }

    public GameAgentViewPagerAdapter(Context context) {
        ArrayList arrayList = new ArrayList();
        this.placeholderImages = arrayList;
        this.context = context;
        arrayList.add(Integer.valueOf(R.drawable.game_agent_left_image));
        this.placeholderImages.add(Integer.valueOf(R.drawable.behavior_learned_left_image));
    }

    public void removeLowSugar() {
        this.placeholderImages.remove(0);
    }

    public void removeBehavior() {
        this.placeholderImages.remove(1);
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public int getCount() {
        return this.placeholderImages.size();
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public Object instantiateItem(ViewGroup viewGroup, int i) {
        View inflate = LayoutInflater.from(this.context).inflate(R.layout.item_image, viewGroup, false);
        ((ImageView) inflate.findViewById(R.id.imageView)).setImageResource(this.placeholderImages.get(i).intValue());
        viewGroup.addView(inflate);
        return inflate;
    }

    @Override // androidx.viewpager.widget.PagerAdapter
    public void destroyItem(ViewGroup viewGroup, int i, Object obj) {
        viewGroup.removeView((View) obj);
    }
}

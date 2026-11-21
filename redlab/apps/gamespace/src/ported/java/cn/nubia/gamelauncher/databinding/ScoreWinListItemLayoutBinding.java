package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ScoreWinListItemLayoutBinding implements ViewBinding {
    public final ImageView ivWinLost;
    public final ImageView ivWinRate;
    private final LinearLayout rootView;
    public final TextView tvAssist;
    public final TextView tvDate;
    public final TextView tvDeath;
    public final TextView tvKill;

    private ScoreWinListItemLayoutBinding(LinearLayout linearLayout, ImageView imageView, ImageView imageView2, TextView textView, TextView textView2, TextView textView3, TextView textView4) {
        this.rootView = linearLayout;
        this.ivWinLost = imageView;
        this.ivWinRate = imageView2;
        this.tvAssist = textView;
        this.tvDate = textView2;
        this.tvDeath = textView3;
        this.tvKill = textView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ScoreWinListItemLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ScoreWinListItemLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.score_win_list_item_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ScoreWinListItemLayoutBinding bind(View view) {
        int i = R.id.iv_win_lost;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_win_lost);
        if (imageView != null) {
            i = R.id.iv_win_rate;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_win_rate);
            if (imageView2 != null) {
                i = R.id.tv_assist;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tv_assist);
                if (textView != null) {
                    i = R.id.tv_date;
                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_date);
                    if (textView2 != null) {
                        i = R.id.tv_death;
                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_death);
                        if (textView3 != null) {
                            i = R.id.tv_kill;
                            TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_kill);
                            if (textView4 != null) {
                                return new ScoreWinListItemLayoutBinding((LinearLayout) view, imageView, imageView2, textView, textView2, textView3, textView4);
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

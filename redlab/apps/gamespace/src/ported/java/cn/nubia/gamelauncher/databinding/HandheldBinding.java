package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class HandheldBinding implements ViewBinding {
    public final TextView gameTitle;
    public final RecyclerView handheldGameList;
    public final RecyclerView handheldGameRecommend;
    public final RecyclerView handheldStreamList;
    public final TextView noRecommend;
    public final TextView recommendTitle;
    private final ConstraintLayout rootView;
    public final TextView title3a;

    private HandheldBinding(ConstraintLayout constraintLayout, TextView textView, RecyclerView recyclerView, RecyclerView recyclerView2, RecyclerView recyclerView3, TextView textView2, TextView textView3, TextView textView4) {
        this.rootView = constraintLayout;
        this.gameTitle = textView;
        this.handheldGameList = recyclerView;
        this.handheldGameRecommend = recyclerView2;
        this.handheldStreamList = recyclerView3;
        this.noRecommend = textView2;
        this.recommendTitle = textView3;
        this.title3a = textView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static HandheldBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HandheldBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.handheld, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HandheldBinding bind(View view) {
        int i = R.id.game_title;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.game_title);
        if (textView != null) {
            i = R.id.handheld_game_list;
            RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, R.id.handheld_game_list);
            if (recyclerView != null) {
                i = R.id.handheld_game_recommend;
                RecyclerView recyclerView2 = (RecyclerView) ViewBindings.findChildViewById(view, R.id.handheld_game_recommend);
                if (recyclerView2 != null) {
                    i = R.id.handheld_stream_list;
                    RecyclerView recyclerView3 = (RecyclerView) ViewBindings.findChildViewById(view, R.id.handheld_stream_list);
                    if (recyclerView3 != null) {
                        i = R.id.no_recommend;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.no_recommend);
                        if (textView2 != null) {
                            i = R.id.recommend_title;
                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.recommend_title);
                            if (textView3 != null) {
                                i = R.id.title_3a;
                                TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.title_3a);
                                if (textView4 != null) {
                                    return new HandheldBinding((ConstraintLayout) view, textView, recyclerView, recyclerView2, recyclerView3, textView2, textView3, textView4);
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

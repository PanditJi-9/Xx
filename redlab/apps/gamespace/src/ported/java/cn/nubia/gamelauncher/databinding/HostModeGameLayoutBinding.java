package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import cn.nubia.common.view.NubiaFontTextView;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class HostModeGameLayoutBinding implements ViewBinding {
    public final RecyclerView hostGameList;
    public final ImageView hostGameListTitle;
    public final NubiaFontTextView hostGameListTitleText;
    public final NubiaFontTextView hostGameStream;
    public final CardView hostItemPc;
    public final TextView hostItemPcTitle;
    public final CardView hostItemStream;
    public final TextView hostItemStreamTitle;
    public final ImageView hostNullArrow;
    public final TextView hostNullGamesText;
    public final View hostTitleAllSelect;
    public final View hostTitleStreamSelect;
    private final ConstraintLayout rootView;

    private HostModeGameLayoutBinding(ConstraintLayout constraintLayout, RecyclerView recyclerView, ImageView imageView, NubiaFontTextView nubiaFontTextView, NubiaFontTextView nubiaFontTextView2, CardView cardView, TextView textView, CardView cardView2, TextView textView2, ImageView imageView2, TextView textView3, View view, View view2) {
        this.rootView = constraintLayout;
        this.hostGameList = recyclerView;
        this.hostGameListTitle = imageView;
        this.hostGameListTitleText = nubiaFontTextView;
        this.hostGameStream = nubiaFontTextView2;
        this.hostItemPc = cardView;
        this.hostItemPcTitle = textView;
        this.hostItemStream = cardView2;
        this.hostItemStreamTitle = textView2;
        this.hostNullArrow = imageView2;
        this.hostNullGamesText = textView3;
        this.hostTitleAllSelect = view;
        this.hostTitleStreamSelect = view2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static HostModeGameLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HostModeGameLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.host_mode_game_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HostModeGameLayoutBinding bind(View view) {
        int i = R.id.host_game_list;
        RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, R.id.host_game_list);
        if (recyclerView != null) {
            i = R.id.host_game_list_title;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.host_game_list_title);
            if (imageView != null) {
                i = R.id.host_game_list_title_text;
                NubiaFontTextView nubiaFontTextView = (NubiaFontTextView) ViewBindings.findChildViewById(view, R.id.host_game_list_title_text);
                if (nubiaFontTextView != null) {
                    i = R.id.host_game_stream;
                    NubiaFontTextView nubiaFontTextView2 = (NubiaFontTextView) ViewBindings.findChildViewById(view, R.id.host_game_stream);
                    if (nubiaFontTextView2 != null) {
                        i = R.id.host_item_pc;
                        CardView cardView = (CardView) ViewBindings.findChildViewById(view, R.id.host_item_pc);
                        if (cardView != null) {
                            i = R.id.host_item_pc_title;
                            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.host_item_pc_title);
                            if (textView != null) {
                                i = R.id.host_item_stream;
                                CardView cardView2 = (CardView) ViewBindings.findChildViewById(view, R.id.host_item_stream);
                                if (cardView2 != null) {
                                    i = R.id.host_item_stream_title;
                                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.host_item_stream_title);
                                    if (textView2 != null) {
                                        i = R.id.host_null_arrow;
                                        ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.host_null_arrow);
                                        if (imageView2 != null) {
                                            i = R.id.host_null_games_text;
                                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.host_null_games_text);
                                            if (textView3 != null) {
                                                i = R.id.host_title_all_select;
                                                View findChildViewById = ViewBindings.findChildViewById(view, R.id.host_title_all_select);
                                                if (findChildViewById != null) {
                                                    i = R.id.host_title_stream_select;
                                                    View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.host_title_stream_select);
                                                    if (findChildViewById2 != null) {
                                                        return new HostModeGameLayoutBinding((ConstraintLayout) view, recyclerView, imageView, nubiaFontTextView, nubiaFontTextView2, cardView, textView, cardView2, textView2, imageView2, textView3, findChildViewById, findChildViewById2);
                                                    }
                                                }
                                            }
                                        }
                                    }
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

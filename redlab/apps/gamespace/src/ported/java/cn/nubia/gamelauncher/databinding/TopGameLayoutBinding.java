package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.neostore.gameplace.GameListView;

/* loaded from: classes.dex */
public final class TopGameLayoutBinding implements ViewBinding {
    public final GameListView gameListView;
    private final LinearLayout rootView;
    public final LinearLayout topGameView;

    private TopGameLayoutBinding(LinearLayout linearLayout, GameListView gameListView, LinearLayout linearLayout2) {
        this.rootView = linearLayout;
        this.gameListView = gameListView;
        this.topGameView = linearLayout2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static TopGameLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TopGameLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.top_game_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TopGameLayoutBinding bind(View view) {
        GameListView gameListView = (GameListView) ViewBindings.findChildViewById(view, R.id.game_list_view);
        if (gameListView != null) {
            LinearLayout linearLayout = (LinearLayout) view;
            return new TopGameLayoutBinding(linearLayout, gameListView, linearLayout);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.game_list_view)));
    }
}

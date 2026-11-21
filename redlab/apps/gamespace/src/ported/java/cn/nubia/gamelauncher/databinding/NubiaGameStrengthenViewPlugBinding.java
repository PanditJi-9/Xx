package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class NubiaGameStrengthenViewPlugBinding implements ViewBinding {
    public final GridView gridView;
    private final View rootView;

    private NubiaGameStrengthenViewPlugBinding(View view, GridView gridView) {
        this.rootView = view;
        this.gridView = gridView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static NubiaGameStrengthenViewPlugBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.nubia_game_strengthen_view_plug, viewGroup);
        return bind(viewGroup);
    }

    public static NubiaGameStrengthenViewPlugBinding bind(View view) {
        GridView gridView = (GridView) ViewBindings.findChildViewById(view, R.id.grid_view);
        if (gridView != null) {
            return new NubiaGameStrengthenViewPlugBinding(view, gridView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.grid_view)));
    }
}

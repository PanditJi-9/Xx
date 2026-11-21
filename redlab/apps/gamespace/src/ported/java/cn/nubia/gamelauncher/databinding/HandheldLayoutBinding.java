package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.core.widget.NestedScrollView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class HandheldLayoutBinding implements ViewBinding {
    public final NestedScrollView handheldScrollView;
    private final RelativeLayout rootView;

    private HandheldLayoutBinding(RelativeLayout relativeLayout, NestedScrollView nestedScrollView) {
        this.rootView = relativeLayout;
        this.handheldScrollView = nestedScrollView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static HandheldLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HandheldLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.handheld_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HandheldLayoutBinding bind(View view) {
        NestedScrollView nestedScrollView = (NestedScrollView) ViewBindings.findChildViewById(view, R.id.handheld_scrollView);
        if (nestedScrollView != null) {
            return new HandheldLayoutBinding((RelativeLayout) view, nestedScrollView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.handheld_scrollView)));
    }
}

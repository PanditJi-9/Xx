package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.HandheldItemLayout;

/* loaded from: classes.dex */
public final class HandheldItemRectangleBinding implements ViewBinding {
    public final HandheldItemLayout handheldBanner;
    public final TextView handheldGameName;
    private final ConstraintLayout rootView;

    private HandheldItemRectangleBinding(ConstraintLayout constraintLayout, HandheldItemLayout handheldItemLayout, TextView textView) {
        this.rootView = constraintLayout;
        this.handheldBanner = handheldItemLayout;
        this.handheldGameName = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static HandheldItemRectangleBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HandheldItemRectangleBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.handheld_item_rectangle, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HandheldItemRectangleBinding bind(View view) {
        int i = R.id.handheld_banner;
        HandheldItemLayout handheldItemLayout = (HandheldItemLayout) ViewBindings.findChildViewById(view, R.id.handheld_banner);
        if (handheldItemLayout != null) {
            i = R.id.handheld_game_name;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.handheld_game_name);
            if (textView != null) {
                return new HandheldItemRectangleBinding((ConstraintLayout) view, handheldItemLayout, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

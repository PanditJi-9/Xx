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
public final class GameActionbarTitlebarLayoutBinding implements ViewBinding {
    public final ImageView leftIcon;
    public final TextView leftName;
    private final LinearLayout rootView;

    private GameActionbarTitlebarLayoutBinding(LinearLayout linearLayout, ImageView imageView, TextView textView) {
        this.rootView = linearLayout;
        this.leftIcon = imageView;
        this.leftName = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static GameActionbarTitlebarLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GameActionbarTitlebarLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.game_actionbar_titlebar_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GameActionbarTitlebarLayoutBinding bind(View view) {
        int i = R.id.left_icon;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.left_icon);
        if (imageView != null) {
            i = R.id.left_name;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.left_name);
            if (textView != null) {
                return new GameActionbarTitlebarLayoutBinding((LinearLayout) view, imageView, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class TgkMenuViewLayoutBinding implements ViewBinding {
    private final RelativeLayout rootView;
    public final View tgkIcon;
    public final LinearLayout tgkMenu;
    public final TextView tgkMenuTitle;

    private TgkMenuViewLayoutBinding(RelativeLayout relativeLayout, View view, LinearLayout linearLayout, TextView textView) {
        this.rootView = relativeLayout;
        this.tgkIcon = view;
        this.tgkMenu = linearLayout;
        this.tgkMenuTitle = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static TgkMenuViewLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkMenuViewLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_menu_view_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkMenuViewLayoutBinding bind(View view) {
        int i = R.id.tgk_icon;
        View findChildViewById = ViewBindings.findChildViewById(view, R.id.tgk_icon);
        if (findChildViewById != null) {
            i = R.id.tgk_menu;
            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.tgk_menu);
            if (linearLayout != null) {
                i = R.id.tgk_menu_title;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tgk_menu_title);
                if (textView != null) {
                    return new TgkMenuViewLayoutBinding((RelativeLayout) view, findChildViewById, linearLayout, textView);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class SuperResolutionSettingsItemLayoutBinding implements ViewBinding {
    public final TextView descriptionView;
    private final RelativeLayout rootView;
    public final CheckBox selectBox;

    private SuperResolutionSettingsItemLayoutBinding(RelativeLayout relativeLayout, TextView textView, CheckBox checkBox) {
        this.rootView = relativeLayout;
        this.descriptionView = textView;
        this.selectBox = checkBox;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static SuperResolutionSettingsItemLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static SuperResolutionSettingsItemLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.super_resolution_settings_item_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static SuperResolutionSettingsItemLayoutBinding bind(View view) {
        int i = R.id.description_view;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.description_view);
        if (textView != null) {
            i = R.id.select_box;
            CheckBox checkBox = (CheckBox) ViewBindings.findChildViewById(view, R.id.select_box);
            if (checkBox != null) {
                return new SuperResolutionSettingsItemLayoutBinding((RelativeLayout) view, textView, checkBox);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

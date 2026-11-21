package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class AppAddTitlebarLayoutBinding implements ViewBinding {
    public final TextView appSelectBtn;
    public final ImageView leftIcon;
    public final TextView leftName;
    public final TextView middleName;
    private final RelativeLayout rootView;

    private AppAddTitlebarLayoutBinding(RelativeLayout relativeLayout, TextView textView, ImageView imageView, TextView textView2, TextView textView3) {
        this.rootView = relativeLayout;
        this.appSelectBtn = textView;
        this.leftIcon = imageView;
        this.leftName = textView2;
        this.middleName = textView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static AppAddTitlebarLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static AppAddTitlebarLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.app_add_titlebar_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static AppAddTitlebarLayoutBinding bind(View view) {
        int i = R.id.app_select_btn;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.app_select_btn);
        if (textView != null) {
            i = R.id.left_icon;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.left_icon);
            if (imageView != null) {
                i = R.id.left_name;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.left_name);
                if (textView2 != null) {
                    i = R.id.middle_name;
                    TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.middle_name);
                    if (textView3 != null) {
                        return new AppAddTitlebarLayoutBinding((RelativeLayout) view, textView, imageView, textView2, textView3);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

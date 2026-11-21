package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class TgkRadiobuttonLayoutBinding implements ViewBinding {
    public final ImageView nubiaRbRightImg;
    public final TextView nubiaRbText;
    public final LinearLayout nubiaSelectBtn;
    private final RelativeLayout rootView;

    private TgkRadiobuttonLayoutBinding(RelativeLayout relativeLayout, ImageView imageView, TextView textView, LinearLayout linearLayout) {
        this.rootView = relativeLayout;
        this.nubiaRbRightImg = imageView;
        this.nubiaRbText = textView;
        this.nubiaSelectBtn = linearLayout;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static TgkRadiobuttonLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkRadiobuttonLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_radiobutton_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkRadiobuttonLayoutBinding bind(View view) {
        int i = R.id.nubia_rb_right_img;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.nubia_rb_right_img);
        if (imageView != null) {
            i = R.id.nubia_rb_text;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_rb_text);
            if (textView != null) {
                i = R.id.nubia_select_btn;
                LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.nubia_select_btn);
                if (linearLayout != null) {
                    return new TgkRadiobuttonLayoutBinding((RelativeLayout) view, imageView, textView, linearLayout);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

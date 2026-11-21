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
public final class PreventAccidentialTouchLayoutBinding implements ViewBinding {
    public final TextView bigRangeText;
    public final TextView middleRangeText;
    public final LinearLayout preventAccidentialTouchLayout;
    public final ImageView preventTouchSwitch;
    public final ImageView rangeDisplayImage;
    private final LinearLayout rootView;
    public final TextView smallRangeText;

    private PreventAccidentialTouchLayoutBinding(LinearLayout linearLayout, TextView textView, TextView textView2, LinearLayout linearLayout2, ImageView imageView, ImageView imageView2, TextView textView3) {
        this.rootView = linearLayout;
        this.bigRangeText = textView;
        this.middleRangeText = textView2;
        this.preventAccidentialTouchLayout = linearLayout2;
        this.preventTouchSwitch = imageView;
        this.rangeDisplayImage = imageView2;
        this.smallRangeText = textView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static PreventAccidentialTouchLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PreventAccidentialTouchLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.prevent_accidential_touch_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PreventAccidentialTouchLayoutBinding bind(View view) {
        int i = R.id.big_range_text;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.big_range_text);
        if (textView != null) {
            i = R.id.middle_range_text;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.middle_range_text);
            if (textView2 != null) {
                LinearLayout linearLayout = (LinearLayout) view;
                i = R.id.prevent_touch_switch;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.prevent_touch_switch);
                if (imageView != null) {
                    i = R.id.range_display_image;
                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.range_display_image);
                    if (imageView2 != null) {
                        i = R.id.small_range_text;
                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.small_range_text);
                        if (textView3 != null) {
                            return new PreventAccidentialTouchLayoutBinding(linearLayout, textView, textView2, linearLayout, imageView, imageView2, textView3);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

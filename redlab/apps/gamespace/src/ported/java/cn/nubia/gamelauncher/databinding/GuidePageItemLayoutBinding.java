package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class GuidePageItemLayoutBinding implements ViewBinding {
    public final Button confirmButton;
    public final FrameLayout confirmButtonArea;
    public final ImageView image;
    public final ImageView indicator1;
    public final ImageView indicator2;
    public final LinearLayout indicators;
    private final RelativeLayout rootView;
    public final TextView text;

    private GuidePageItemLayoutBinding(RelativeLayout relativeLayout, Button button, FrameLayout frameLayout, ImageView imageView, ImageView imageView2, ImageView imageView3, LinearLayout linearLayout, TextView textView) {
        this.rootView = relativeLayout;
        this.confirmButton = button;
        this.confirmButtonArea = frameLayout;
        this.image = imageView;
        this.indicator1 = imageView2;
        this.indicator2 = imageView3;
        this.indicators = linearLayout;
        this.text = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static GuidePageItemLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GuidePageItemLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.guide_page_item_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GuidePageItemLayoutBinding bind(View view) {
        int i = R.id.confirm_button;
        Button button = (Button) ViewBindings.findChildViewById(view, R.id.confirm_button);
        if (button != null) {
            i = R.id.confirm_button_area;
            FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, R.id.confirm_button_area);
            if (frameLayout != null) {
                i = R.id.image;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.image);
                if (imageView != null) {
                    i = R.id.indicator_1;
                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.indicator_1);
                    if (imageView2 != null) {
                        i = R.id.indicator_2;
                        ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.indicator_2);
                        if (imageView3 != null) {
                            i = R.id.indicators;
                            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.indicators);
                            if (linearLayout != null) {
                                i = R.id.text;
                                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.text);
                                if (textView != null) {
                                    return new GuidePageItemLayoutBinding((RelativeLayout) view, button, frameLayout, imageView, imageView2, imageView3, linearLayout, textView);
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

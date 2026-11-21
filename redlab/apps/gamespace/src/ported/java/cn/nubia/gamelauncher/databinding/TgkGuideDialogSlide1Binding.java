package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MarqueeTextView;

/* loaded from: classes.dex */
public final class TgkGuideDialogSlide1Binding implements ViewBinding {
    private final LinearLayout rootView;
    public final MarqueeTextView tailText;
    public final View tailText0;
    public final ImageView tgkSlide1;

    private TgkGuideDialogSlide1Binding(LinearLayout linearLayout, MarqueeTextView marqueeTextView, View view, ImageView imageView) {
        this.rootView = linearLayout;
        this.tailText = marqueeTextView;
        this.tailText0 = view;
        this.tgkSlide1 = imageView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static TgkGuideDialogSlide1Binding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkGuideDialogSlide1Binding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_guide_dialog_slide_1, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkGuideDialogSlide1Binding bind(View view) {
        int i = R.id.tail_text;
        MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tail_text);
        if (marqueeTextView != null) {
            i = R.id.tail_text0;
            View findChildViewById = ViewBindings.findChildViewById(view, R.id.tail_text0);
            if (findChildViewById != null) {
                i = R.id.tgk_slide1;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.tgk_slide1);
                if (imageView != null) {
                    return new TgkGuideDialogSlide1Binding((LinearLayout) view, marqueeTextView, findChildViewById, imageView);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

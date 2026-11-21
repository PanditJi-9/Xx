package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MarqueeTextView;

/* loaded from: classes.dex */
public final class TgkGuideDialogSlide3Binding implements ViewBinding {
    private final RelativeLayout rootView;
    public final MarqueeTextView startSetting;
    public final View tailText;
    public final MarqueeTextView tailText0;
    public final ImageView tgkSlide3;

    private TgkGuideDialogSlide3Binding(RelativeLayout relativeLayout, MarqueeTextView marqueeTextView, View view, MarqueeTextView marqueeTextView2, ImageView imageView) {
        this.rootView = relativeLayout;
        this.startSetting = marqueeTextView;
        this.tailText = view;
        this.tailText0 = marqueeTextView2;
        this.tgkSlide3 = imageView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static TgkGuideDialogSlide3Binding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkGuideDialogSlide3Binding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_guide_dialog_slide_3, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkGuideDialogSlide3Binding bind(View view) {
        int i = R.id.start_setting;
        MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.start_setting);
        if (marqueeTextView != null) {
            i = R.id.tail_text;
            View findChildViewById = ViewBindings.findChildViewById(view, R.id.tail_text);
            if (findChildViewById != null) {
                i = R.id.tail_text0;
                MarqueeTextView marqueeTextView2 = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tail_text0);
                if (marqueeTextView2 != null) {
                    i = R.id.tgk_slide3;
                    ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.tgk_slide3);
                    if (imageView != null) {
                        return new TgkGuideDialogSlide3Binding((RelativeLayout) view, marqueeTextView, findChildViewById, marqueeTextView2, imageView);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

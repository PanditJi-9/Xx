package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MarqueeTextView;

/* loaded from: classes.dex */
public final class TgkGuideDialogSlide3OldBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final Button startSetting;
    public final MarqueeTextView tailText;
    public final MarqueeTextView tailTextSubRisk;
    public final ImageView tgkSlide3;

    private TgkGuideDialogSlide3OldBinding(LinearLayout linearLayout, Button button, MarqueeTextView marqueeTextView, MarqueeTextView marqueeTextView2, ImageView imageView) {
        this.rootView = linearLayout;
        this.startSetting = button;
        this.tailText = marqueeTextView;
        this.tailTextSubRisk = marqueeTextView2;
        this.tgkSlide3 = imageView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static TgkGuideDialogSlide3OldBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkGuideDialogSlide3OldBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_guide_dialog_slide_3_old, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkGuideDialogSlide3OldBinding bind(View view) {
        int i = R.id.start_setting;
        Button button = (Button) ViewBindings.findChildViewById(view, R.id.start_setting);
        if (button != null) {
            i = R.id.tail_text;
            MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tail_text);
            if (marqueeTextView != null) {
                i = R.id.tail_text_sub_risk;
                MarqueeTextView marqueeTextView2 = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tail_text_sub_risk);
                if (marqueeTextView2 != null) {
                    i = R.id.tgk_slide3;
                    ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.tgk_slide3);
                    if (imageView != null) {
                        return new TgkGuideDialogSlide3OldBinding((LinearLayout) view, button, marqueeTextView, marqueeTextView2, imageView);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

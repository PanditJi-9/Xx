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
public final class TgkGuideDialogSlide1OldBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final TextView tailText;
    public final TextView tailText0;
    public final TextView tailTextSub;
    public final ImageView tgkSlide1;

    private TgkGuideDialogSlide1OldBinding(LinearLayout linearLayout, TextView textView, TextView textView2, TextView textView3, ImageView imageView) {
        this.rootView = linearLayout;
        this.tailText = textView;
        this.tailText0 = textView2;
        this.tailTextSub = textView3;
        this.tgkSlide1 = imageView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static TgkGuideDialogSlide1OldBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkGuideDialogSlide1OldBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_guide_dialog_slide_1_old, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkGuideDialogSlide1OldBinding bind(View view) {
        int i = R.id.tail_text;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tail_text);
        if (textView != null) {
            i = R.id.tail_text0;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.tail_text0);
            if (textView2 != null) {
                i = R.id.tail_text_sub;
                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.tail_text_sub);
                if (textView3 != null) {
                    i = R.id.tgk_slide1;
                    ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.tgk_slide1);
                    if (imageView != null) {
                        return new TgkGuideDialogSlide1OldBinding((LinearLayout) view, textView, textView2, textView3, imageView);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

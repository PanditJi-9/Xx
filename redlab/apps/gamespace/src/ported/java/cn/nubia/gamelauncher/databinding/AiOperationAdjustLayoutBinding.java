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
public final class AiOperationAdjustLayoutBinding implements ViewBinding {
    public final ImageView aiAdjustCheckbox;
    public final RelativeLayout aiAdjustLayout;
    public final TextView aiAdjustText;
    private final RelativeLayout rootView;

    private AiOperationAdjustLayoutBinding(RelativeLayout relativeLayout, ImageView imageView, RelativeLayout relativeLayout2, TextView textView) {
        this.rootView = relativeLayout;
        this.aiAdjustCheckbox = imageView;
        this.aiAdjustLayout = relativeLayout2;
        this.aiAdjustText = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static AiOperationAdjustLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static AiOperationAdjustLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.ai_operation_adjust_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static AiOperationAdjustLayoutBinding bind(View view) {
        int i = R.id.ai_adjust_checkbox;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.ai_adjust_checkbox);
        if (imageView != null) {
            RelativeLayout relativeLayout = (RelativeLayout) view;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.ai_adjust_text);
            if (textView != null) {
                return new AiOperationAdjustLayoutBinding(relativeLayout, imageView, relativeLayout, textView);
            }
            i = R.id.ai_adjust_text;
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

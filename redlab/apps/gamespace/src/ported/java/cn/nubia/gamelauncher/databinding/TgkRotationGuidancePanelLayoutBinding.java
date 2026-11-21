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

/* loaded from: classes.dex */
public final class TgkRotationGuidancePanelLayoutBinding implements ViewBinding {
    public final Button knowButton;
    public final ImageView noremindimageview;
    public final LinearLayout noremindpanel;
    private final LinearLayout rootView;

    private TgkRotationGuidancePanelLayoutBinding(LinearLayout linearLayout, Button button, ImageView imageView, LinearLayout linearLayout2) {
        this.rootView = linearLayout;
        this.knowButton = button;
        this.noremindimageview = imageView;
        this.noremindpanel = linearLayout2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static TgkRotationGuidancePanelLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkRotationGuidancePanelLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_rotation_guidance_panel_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkRotationGuidancePanelLayoutBinding bind(View view) {
        int i = R.id.knowButton;
        Button button = (Button) ViewBindings.findChildViewById(view, R.id.knowButton);
        if (button != null) {
            i = R.id.noremindimageview;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.noremindimageview);
            if (imageView != null) {
                i = R.id.noremindpanel;
                LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.noremindpanel);
                if (linearLayout != null) {
                    return new TgkRotationGuidancePanelLayoutBinding((LinearLayout) view, button, imageView, linearLayout);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

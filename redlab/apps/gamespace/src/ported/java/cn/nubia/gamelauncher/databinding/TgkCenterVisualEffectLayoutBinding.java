package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MarqueeTextView;

/* loaded from: classes.dex */
public final class TgkCenterVisualEffectLayoutBinding implements ViewBinding {
    private final RelativeLayout rootView;
    public final View summary;
    public final RelativeLayout texts;
    public final ToggleButton tgkCenterVisualEffectBtn;
    public final MarqueeTextView title;

    private TgkCenterVisualEffectLayoutBinding(RelativeLayout relativeLayout, View view, RelativeLayout relativeLayout2, ToggleButton toggleButton, MarqueeTextView marqueeTextView) {
        this.rootView = relativeLayout;
        this.summary = view;
        this.texts = relativeLayout2;
        this.tgkCenterVisualEffectBtn = toggleButton;
        this.title = marqueeTextView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static TgkCenterVisualEffectLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkCenterVisualEffectLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_center_visual_effect_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkCenterVisualEffectLayoutBinding bind(View view) {
        int i = R.id.summary;
        View findChildViewById = ViewBindings.findChildViewById(view, R.id.summary);
        if (findChildViewById != null) {
            i = R.id.texts;
            RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.texts);
            if (relativeLayout != null) {
                i = R.id.tgk_center_visual_effect_btn;
                ToggleButton toggleButton = (ToggleButton) ViewBindings.findChildViewById(view, R.id.tgk_center_visual_effect_btn);
                if (toggleButton != null) {
                    i = R.id.title;
                    MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.title);
                    if (marqueeTextView != null) {
                        return new TgkCenterVisualEffectLayoutBinding((RelativeLayout) view, findChildViewById, relativeLayout, toggleButton, marqueeTextView);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

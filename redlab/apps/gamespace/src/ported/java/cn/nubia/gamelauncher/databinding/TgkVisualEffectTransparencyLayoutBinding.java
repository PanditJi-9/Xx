package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MarqueeTextView;

/* loaded from: classes.dex */
public final class TgkVisualEffectTransparencyLayoutBinding implements ViewBinding {
    public final RelativeLayout rootView;
    private final RelativeLayout rootView_;
    public final SeekBar tgkCenterVisualEffectSeekbar;
    public final MarqueeTextView title;

    private TgkVisualEffectTransparencyLayoutBinding(RelativeLayout relativeLayout, RelativeLayout relativeLayout2, SeekBar seekBar, MarqueeTextView marqueeTextView) {
        this.rootView_ = relativeLayout;
        this.rootView = relativeLayout2;
        this.tgkCenterVisualEffectSeekbar = seekBar;
        this.title = marqueeTextView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView_;
    }

    public static TgkVisualEffectTransparencyLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkVisualEffectTransparencyLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_visual_effect_transparency_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkVisualEffectTransparencyLayoutBinding bind(View view) {
        RelativeLayout relativeLayout = (RelativeLayout) view;
        int i = R.id.tgk_center_visual_effect_seekbar;
        SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.tgk_center_visual_effect_seekbar);
        if (seekBar != null) {
            i = R.id.title;
            MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.title);
            if (marqueeTextView != null) {
                return new TgkVisualEffectTransparencyLayoutBinding(relativeLayout, relativeLayout, seekBar, marqueeTextView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

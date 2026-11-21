package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.TgkMultSeekBarView;

/* loaded from: classes.dex */
public final class TgkMultclicksViewLayoutBinding implements ViewBinding {
    private final RelativeLayout rootView;
    public final TgkMultSeekBarView tgkMultSeekbarView;
    public final ImageButton tgkMultViewClose;

    private TgkMultclicksViewLayoutBinding(RelativeLayout relativeLayout, TgkMultSeekBarView tgkMultSeekBarView, ImageButton imageButton) {
        this.rootView = relativeLayout;
        this.tgkMultSeekbarView = tgkMultSeekBarView;
        this.tgkMultViewClose = imageButton;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static TgkMultclicksViewLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkMultclicksViewLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_multclicks_view_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkMultclicksViewLayoutBinding bind(View view) {
        int i = R.id.tgk_mult_seekbar_view;
        TgkMultSeekBarView tgkMultSeekBarView = (TgkMultSeekBarView) ViewBindings.findChildViewById(view, R.id.tgk_mult_seekbar_view);
        if (tgkMultSeekBarView != null) {
            i = R.id.tgk_mult_view_close;
            ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, R.id.tgk_mult_view_close);
            if (imageButton != null) {
                return new TgkMultclicksViewLayoutBinding((RelativeLayout) view, tgkMultSeekBarView, imageButton);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

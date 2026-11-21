package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MarqueeTextView;

/* loaded from: classes.dex */
public final class TgkSensitivityViewLayoutBinding implements ViewBinding {
    private final FrameLayout rootView;
    public final SeekBar sensitivitySeekBar;
    public final TextView sensitivityTvH;
    public final TextView sensitivityTvL;
    public final TextView sensitivityTvN;
    public final RelativeLayout sensitivityViewL;
    public final MarqueeTextView tgkSensitivityTitle;

    private TgkSensitivityViewLayoutBinding(FrameLayout frameLayout, SeekBar seekBar, TextView textView, TextView textView2, TextView textView3, RelativeLayout relativeLayout, MarqueeTextView marqueeTextView) {
        this.rootView = frameLayout;
        this.sensitivitySeekBar = seekBar;
        this.sensitivityTvH = textView;
        this.sensitivityTvL = textView2;
        this.sensitivityTvN = textView3;
        this.sensitivityViewL = relativeLayout;
        this.tgkSensitivityTitle = marqueeTextView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static TgkSensitivityViewLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkSensitivityViewLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_sensitivity_view_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkSensitivityViewLayoutBinding bind(View view) {
        int i = R.id.sensitivity_seek_bar;
        SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.sensitivity_seek_bar);
        if (seekBar != null) {
            i = R.id.sensitivity_tv_h;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.sensitivity_tv_h);
            if (textView != null) {
                i = R.id.sensitivity_tv_l;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.sensitivity_tv_l);
                if (textView2 != null) {
                    i = R.id.sensitivity_tv_n;
                    TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.sensitivity_tv_n);
                    if (textView3 != null) {
                        i = R.id.sensitivity_view_l;
                        RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.sensitivity_view_l);
                        if (relativeLayout != null) {
                            i = R.id.tgk_sensitivity_title;
                            MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_sensitivity_title);
                            if (marqueeTextView != null) {
                                return new TgkSensitivityViewLayoutBinding((FrameLayout) view, seekBar, textView, textView2, textView3, relativeLayout, marqueeTextView);
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

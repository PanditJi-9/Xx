package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class BehaviorLearnedWidgetSwitchBinding implements ViewBinding {
    public final ToggleButton gamespaceSwitch;
    public final TextView gamespaceSwitchOffId;
    public final TextView gamespaceSwitchOnId;
    private final FrameLayout rootView;
    public final ImageView switchButtonBg;
    public final FrameLayout switchButtonLayout;
    public final ImageView switchButtonThumbBlack;
    public final ImageView switchButtonThumbRed;
    public final ImageView switchButtonTrackBlack;
    public final ImageView switchButtonTrackGray;

    private BehaviorLearnedWidgetSwitchBinding(FrameLayout frameLayout, ToggleButton toggleButton, TextView textView, TextView textView2, ImageView imageView, FrameLayout frameLayout2, ImageView imageView2, ImageView imageView3, ImageView imageView4, ImageView imageView5) {
        this.rootView = frameLayout;
        this.gamespaceSwitch = toggleButton;
        this.gamespaceSwitchOffId = textView;
        this.gamespaceSwitchOnId = textView2;
        this.switchButtonBg = imageView;
        this.switchButtonLayout = frameLayout2;
        this.switchButtonThumbBlack = imageView2;
        this.switchButtonThumbRed = imageView3;
        this.switchButtonTrackBlack = imageView4;
        this.switchButtonTrackGray = imageView5;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static BehaviorLearnedWidgetSwitchBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static BehaviorLearnedWidgetSwitchBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.behavior_learned_widget_switch, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static BehaviorLearnedWidgetSwitchBinding bind(View view) {
        int i = R.id.gamespace_switch;
        ToggleButton toggleButton = (ToggleButton) ViewBindings.findChildViewById(view, R.id.gamespace_switch);
        if (toggleButton != null) {
            i = R.id.gamespace_switch_off_id;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.gamespace_switch_off_id);
            if (textView != null) {
                i = R.id.gamespace_switch_on_id;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.gamespace_switch_on_id);
                if (textView2 != null) {
                    i = R.id.switch_button_bg;
                    ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.switch_button_bg);
                    if (imageView != null) {
                        i = R.id.switch_button_layout;
                        FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, R.id.switch_button_layout);
                        if (frameLayout != null) {
                            i = R.id.switch_button_thumb_black;
                            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.switch_button_thumb_black);
                            if (imageView2 != null) {
                                i = R.id.switch_button_thumb_red;
                                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.switch_button_thumb_red);
                                if (imageView3 != null) {
                                    i = R.id.switch_button_track_black;
                                    ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.switch_button_track_black);
                                    if (imageView4 != null) {
                                        i = R.id.switch_button_track_gray;
                                        ImageView imageView5 = (ImageView) ViewBindings.findChildViewById(view, R.id.switch_button_track_gray);
                                        if (imageView5 != null) {
                                            return new BehaviorLearnedWidgetSwitchBinding((FrameLayout) view, toggleButton, textView, textView2, imageView, frameLayout, imageView2, imageView3, imageView4, imageView5);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

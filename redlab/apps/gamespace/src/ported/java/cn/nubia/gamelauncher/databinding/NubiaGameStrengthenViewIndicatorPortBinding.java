package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class NubiaGameStrengthenViewIndicatorPortBinding implements ViewBinding {
    public final ScrollView indicatorScrollView;
    public final FrameLayout optionsItemGameFunctionText;
    public final FrameLayout optionsItemGameGyroscopeText;
    public final FrameLayout optionsItemGamePlugText;
    public final FrameLayout optionsItemGameScreenText;
    public final FrameLayout optionsItemGameVoiceText;
    public final FrameLayout optionsItemGpuSettingsText;
    public final FrameLayout optionsItemNetSettingsText;
    public final FrameLayout optionsItemResourceLibSettings;
    public final FrameLayout optionsItemStrengthperformanceText;
    private final View rootView;
    public final LinearLayout viewLayout;

    private NubiaGameStrengthenViewIndicatorPortBinding(View view, ScrollView scrollView, FrameLayout frameLayout, FrameLayout frameLayout2, FrameLayout frameLayout3, FrameLayout frameLayout4, FrameLayout frameLayout5, FrameLayout frameLayout6, FrameLayout frameLayout7, FrameLayout frameLayout8, FrameLayout frameLayout9, LinearLayout linearLayout) {
        this.rootView = view;
        this.indicatorScrollView = scrollView;
        this.optionsItemGameFunctionText = frameLayout;
        this.optionsItemGameGyroscopeText = frameLayout2;
        this.optionsItemGamePlugText = frameLayout3;
        this.optionsItemGameScreenText = frameLayout4;
        this.optionsItemGameVoiceText = frameLayout5;
        this.optionsItemGpuSettingsText = frameLayout6;
        this.optionsItemNetSettingsText = frameLayout7;
        this.optionsItemResourceLibSettings = frameLayout8;
        this.optionsItemStrengthperformanceText = frameLayout9;
        this.viewLayout = linearLayout;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static NubiaGameStrengthenViewIndicatorPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.nubia_game_strengthen_view_indicator_port, viewGroup);
        return bind(viewGroup);
    }

    public static NubiaGameStrengthenViewIndicatorPortBinding bind(View view) {
        int i = R.id.indicator_scrollView;
        ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, R.id.indicator_scrollView);
        if (scrollView != null) {
            i = R.id.options_item_game_function_text;
            FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_game_function_text);
            if (frameLayout != null) {
                i = R.id.options_item_game_gyroscope_text;
                FrameLayout frameLayout2 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_game_gyroscope_text);
                if (frameLayout2 != null) {
                    i = R.id.options_item_game_plug_text;
                    FrameLayout frameLayout3 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_game_plug_text);
                    if (frameLayout3 != null) {
                        i = R.id.options_item_game_screen_text;
                        FrameLayout frameLayout4 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_game_screen_text);
                        if (frameLayout4 != null) {
                            i = R.id.options_item_game_voice_text;
                            FrameLayout frameLayout5 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_game_voice_text);
                            if (frameLayout5 != null) {
                                i = R.id.options_item_gpu_settings_text;
                                FrameLayout frameLayout6 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_gpu_settings_text);
                                if (frameLayout6 != null) {
                                    i = R.id.options_item_net_settings_text;
                                    FrameLayout frameLayout7 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_net_settings_text);
                                    if (frameLayout7 != null) {
                                        i = R.id.options_item_resource_lib_settings;
                                        FrameLayout frameLayout8 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_resource_lib_settings);
                                        if (frameLayout8 != null) {
                                            i = R.id.options_item_strengthperformance_text;
                                            FrameLayout frameLayout9 = (FrameLayout) ViewBindings.findChildViewById(view, R.id.options_item_strengthperformance_text);
                                            if (frameLayout9 != null) {
                                                i = R.id.view_layout;
                                                LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.view_layout);
                                                if (linearLayout != null) {
                                                    return new NubiaGameStrengthenViewIndicatorPortBinding(view, scrollView, frameLayout, frameLayout2, frameLayout3, frameLayout4, frameLayout5, frameLayout6, frameLayout7, frameLayout8, frameLayout9, linearLayout);
                                                }
                                            }
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

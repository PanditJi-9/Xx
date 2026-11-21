package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class NubiaGameStrengthenViewFunctionBinding implements ViewBinding {
    public final ImageView autoOpenFanCheckbox;
    public final TextView autoOpenFanText;
    public final ImageView autoOpenLiquidCoolingCheckbox;
    public final TextView autoOpenLiquidCoolingText;
    public final RelativeLayout functionRedmagicTime;
    public final ImageView functionRedmagicTimeCheckbox;
    public final ImageView functionRedmagicTimeLampCheckbox;
    public final RelativeLayout functionRedmagicTimeLampLayout;
    public final TextView functionRedmagicTimeLampText;
    public final LinearLayout functionRedmagicTimeLayout;
    public final TextView functionRedmagicTimeText;
    public final RelativeLayout nubiaAutoOpenFanLayout;
    public final RelativeLayout nubiaAutoOpenLiquidCoolingLayout;
    public final LinearLayout nubiaGameStrengthFunctionLayout;
    private final View rootView;
    public final ScrollView scrollView;

    private NubiaGameStrengthenViewFunctionBinding(View view, ImageView imageView, TextView textView, ImageView imageView2, TextView textView2, RelativeLayout relativeLayout, ImageView imageView3, ImageView imageView4, RelativeLayout relativeLayout2, TextView textView3, LinearLayout linearLayout, TextView textView4, RelativeLayout relativeLayout3, RelativeLayout relativeLayout4, LinearLayout linearLayout2, ScrollView scrollView) {
        this.rootView = view;
        this.autoOpenFanCheckbox = imageView;
        this.autoOpenFanText = textView;
        this.autoOpenLiquidCoolingCheckbox = imageView2;
        this.autoOpenLiquidCoolingText = textView2;
        this.functionRedmagicTime = relativeLayout;
        this.functionRedmagicTimeCheckbox = imageView3;
        this.functionRedmagicTimeLampCheckbox = imageView4;
        this.functionRedmagicTimeLampLayout = relativeLayout2;
        this.functionRedmagicTimeLampText = textView3;
        this.functionRedmagicTimeLayout = linearLayout;
        this.functionRedmagicTimeText = textView4;
        this.nubiaAutoOpenFanLayout = relativeLayout3;
        this.nubiaAutoOpenLiquidCoolingLayout = relativeLayout4;
        this.nubiaGameStrengthFunctionLayout = linearLayout2;
        this.scrollView = scrollView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static NubiaGameStrengthenViewFunctionBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.nubia_game_strengthen_view_function, viewGroup);
        return bind(viewGroup);
    }

    public static NubiaGameStrengthenViewFunctionBinding bind(View view) {
        int i = R.id.auto_open_fan_checkbox;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.auto_open_fan_checkbox);
        if (imageView != null) {
            i = R.id.auto_open_fan_text;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.auto_open_fan_text);
            if (textView != null) {
                i = R.id.auto_open_liquid_cooling_checkbox;
                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.auto_open_liquid_cooling_checkbox);
                if (imageView2 != null) {
                    i = R.id.auto_open_liquid_cooling_text;
                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.auto_open_liquid_cooling_text);
                    if (textView2 != null) {
                        i = R.id.function_redmagic_time;
                        RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_redmagic_time);
                        if (relativeLayout != null) {
                            i = R.id.function_redmagic_time_checkbox;
                            ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_checkbox);
                            if (imageView3 != null) {
                                i = R.id.function_redmagic_time_lamp_checkbox;
                                ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_lamp_checkbox);
                                if (imageView4 != null) {
                                    i = R.id.function_redmagic_time_lamp_layout;
                                    RelativeLayout relativeLayout2 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_lamp_layout);
                                    if (relativeLayout2 != null) {
                                        i = R.id.function_redmagic_time_lamp_text;
                                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_lamp_text);
                                        if (textView3 != null) {
                                            i = R.id.function_redmagic_time_layout;
                                            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_layout);
                                            if (linearLayout != null) {
                                                i = R.id.function_redmagic_time_text;
                                                TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_text);
                                                if (textView4 != null) {
                                                    i = R.id.nubia_auto_open_fan_layout;
                                                    RelativeLayout relativeLayout3 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.nubia_auto_open_fan_layout);
                                                    if (relativeLayout3 != null) {
                                                        i = R.id.nubia_auto_open_liquid_cooling_layout;
                                                        RelativeLayout relativeLayout4 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.nubia_auto_open_liquid_cooling_layout);
                                                        if (relativeLayout4 != null) {
                                                            i = R.id.nubia_game_strength_function_layout;
                                                            LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.nubia_game_strength_function_layout);
                                                            if (linearLayout2 != null) {
                                                                i = R.id.scroll_view;
                                                                ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, R.id.scroll_view);
                                                                if (scrollView != null) {
                                                                    return new NubiaGameStrengthenViewFunctionBinding(view, imageView, textView, imageView2, textView2, relativeLayout, imageView3, imageView4, relativeLayout2, textView3, linearLayout, textView4, relativeLayout3, relativeLayout4, linearLayout2, scrollView);
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
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

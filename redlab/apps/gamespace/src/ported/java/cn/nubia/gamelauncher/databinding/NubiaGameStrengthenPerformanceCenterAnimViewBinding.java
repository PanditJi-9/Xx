package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.gamecontrolpanel.GameStrengthenPerformanceRectangle;

/* loaded from: classes.dex */
public final class NubiaGameStrengthenPerformanceCenterAnimViewBinding implements ViewBinding {
    public final View nubiaGameStrengthenPerformanceAnimationAutoLogo;
    public final GameStrengthenPerformanceRectangle nubiaGameStrengthenPerformanceAnimationCpu;
    public final View nubiaGameStrengthenPerformanceAnimationFogBlue;
    public final View nubiaGameStrengthenPerformanceAnimationFogBlue1;
    public final View nubiaGameStrengthenPerformanceAnimationFogRed;
    public final View nubiaGameStrengthenPerformanceAnimationFogRed1;
    public final GameStrengthenPerformanceRectangle nubiaGameStrengthenPerformanceAnimationGpu;
    public final View nubiaGameStrengthenPerformanceAnimationLightLogo1;
    public final View nubiaGameStrengthenPerformanceAnimationLightLogo2;
    public final ImageView nubiaGameStrengthenPerformanceSuperModeFan;
    public final FrameLayout nubiaGameStrengthenPerformanceSuperModeFanBg;
    private final View rootView;

    private NubiaGameStrengthenPerformanceCenterAnimViewBinding(View view, View view2, GameStrengthenPerformanceRectangle gameStrengthenPerformanceRectangle, View view3, View view4, View view5, View view6, GameStrengthenPerformanceRectangle gameStrengthenPerformanceRectangle2, View view7, View view8, ImageView imageView, FrameLayout frameLayout) {
        this.rootView = view;
        this.nubiaGameStrengthenPerformanceAnimationAutoLogo = view2;
        this.nubiaGameStrengthenPerformanceAnimationCpu = gameStrengthenPerformanceRectangle;
        this.nubiaGameStrengthenPerformanceAnimationFogBlue = view3;
        this.nubiaGameStrengthenPerformanceAnimationFogBlue1 = view4;
        this.nubiaGameStrengthenPerformanceAnimationFogRed = view5;
        this.nubiaGameStrengthenPerformanceAnimationFogRed1 = view6;
        this.nubiaGameStrengthenPerformanceAnimationGpu = gameStrengthenPerformanceRectangle2;
        this.nubiaGameStrengthenPerformanceAnimationLightLogo1 = view7;
        this.nubiaGameStrengthenPerformanceAnimationLightLogo2 = view8;
        this.nubiaGameStrengthenPerformanceSuperModeFan = imageView;
        this.nubiaGameStrengthenPerformanceSuperModeFanBg = frameLayout;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static NubiaGameStrengthenPerformanceCenterAnimViewBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.nubia_game_strengthen_performance_center_anim_view, viewGroup);
        return bind(viewGroup);
    }

    public static NubiaGameStrengthenPerformanceCenterAnimViewBinding bind(View view) {
        int i = R.id.nubia_game_strengthen_performance_animation_auto_logo;
        View findChildViewById = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_auto_logo);
        if (findChildViewById != null) {
            i = R.id.nubia_game_strengthen_performance_animation_cpu;
            GameStrengthenPerformanceRectangle gameStrengthenPerformanceRectangle = (GameStrengthenPerformanceRectangle) ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_cpu);
            if (gameStrengthenPerformanceRectangle != null) {
                i = R.id.nubia_game_strengthen_performance_animation_fog_blue;
                View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_fog_blue);
                if (findChildViewById2 != null) {
                    i = R.id.nubia_game_strengthen_performance_animation_fog_blue1;
                    View findChildViewById3 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_fog_blue1);
                    if (findChildViewById3 != null) {
                        i = R.id.nubia_game_strengthen_performance_animation_fog_red;
                        View findChildViewById4 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_fog_red);
                        if (findChildViewById4 != null) {
                            i = R.id.nubia_game_strengthen_performance_animation_fog_red1;
                            View findChildViewById5 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_fog_red1);
                            if (findChildViewById5 != null) {
                                i = R.id.nubia_game_strengthen_performance_animation_gpu;
                                GameStrengthenPerformanceRectangle gameStrengthenPerformanceRectangle2 = (GameStrengthenPerformanceRectangle) ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_gpu);
                                if (gameStrengthenPerformanceRectangle2 != null) {
                                    i = R.id.nubia_game_strengthen_performance_animation_light_logo1;
                                    View findChildViewById6 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_light_logo1);
                                    if (findChildViewById6 != null) {
                                        i = R.id.nubia_game_strengthen_performance_animation_light_logo2;
                                        View findChildViewById7 = ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_animation_light_logo2);
                                        if (findChildViewById7 != null) {
                                            i = R.id.nubia_game_strengthen_performance_super_mode_fan;
                                            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_super_mode_fan);
                                            if (imageView != null) {
                                                i = R.id.nubia_game_strengthen_performance_super_mode_fan_bg;
                                                FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, R.id.nubia_game_strengthen_performance_super_mode_fan_bg);
                                                if (frameLayout != null) {
                                                    return new NubiaGameStrengthenPerformanceCenterAnimViewBinding(view, findChildViewById, gameStrengthenPerformanceRectangle, findChildViewById2, findChildViewById3, findChildViewById4, findChildViewById5, gameStrengthenPerformanceRectangle2, findChildViewById6, findChildViewById7, imageView, frameLayout);
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

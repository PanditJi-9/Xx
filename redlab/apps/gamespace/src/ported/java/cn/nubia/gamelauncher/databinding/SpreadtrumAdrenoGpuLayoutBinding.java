package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class SpreadtrumAdrenoGpuLayoutBinding implements ViewBinding {
    public final ScrollView gameGpuAllLayout;
    public final ConstraintLayout gpuSettingsModeSwitchLayout;
    public final View nubiaGameAdrenoGpuDivider;
    public final TextView nubiaGameGpuAdrenoCustomer;
    public final TextView nubiaGameGpuAdrenoHighqual;
    public final TextView nubiaGameGpuAdrenoPowersav;
    public final TextView nubiaGameGpuAdrenoStandard;
    public final TextView nubiaGameVoiceStrengthenClosePrompt;
    private final LinearLayout rootView;
    public final LinearLayout snapdragonAdrenoGpuCustomLayout;
    public final View snapdragonAdrenoGpuDialogLayout;

    private SpreadtrumAdrenoGpuLayoutBinding(LinearLayout linearLayout, ScrollView scrollView, ConstraintLayout constraintLayout, View view, TextView textView, TextView textView2, TextView textView3, TextView textView4, TextView textView5, LinearLayout linearLayout2, View view2) {
        this.rootView = linearLayout;
        this.gameGpuAllLayout = scrollView;
        this.gpuSettingsModeSwitchLayout = constraintLayout;
        this.nubiaGameAdrenoGpuDivider = view;
        this.nubiaGameGpuAdrenoCustomer = textView;
        this.nubiaGameGpuAdrenoHighqual = textView2;
        this.nubiaGameGpuAdrenoPowersav = textView3;
        this.nubiaGameGpuAdrenoStandard = textView4;
        this.nubiaGameVoiceStrengthenClosePrompt = textView5;
        this.snapdragonAdrenoGpuCustomLayout = linearLayout2;
        this.snapdragonAdrenoGpuDialogLayout = view2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static SpreadtrumAdrenoGpuLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static SpreadtrumAdrenoGpuLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.spreadtrum_adreno_gpu_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static SpreadtrumAdrenoGpuLayoutBinding bind(View view) {
        int i = R.id.game_gpu_all_layout;
        ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, R.id.game_gpu_all_layout);
        if (scrollView != null) {
            i = R.id.gpu_settings_mode_switch_layout;
            ConstraintLayout constraintLayout = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.gpu_settings_mode_switch_layout);
            if (constraintLayout != null) {
                i = R.id.nubia_game_adreno_gpu_divider;
                View findChildViewById = ViewBindings.findChildViewById(view, R.id.nubia_game_adreno_gpu_divider);
                if (findChildViewById != null) {
                    i = R.id.nubia_game_gpu_adreno_customer;
                    TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_gpu_adreno_customer);
                    if (textView != null) {
                        i = R.id.nubia_game_gpu_adreno_highqual;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_gpu_adreno_highqual);
                        if (textView2 != null) {
                            i = R.id.nubia_game_gpu_adreno_powersav;
                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_gpu_adreno_powersav);
                            if (textView3 != null) {
                                i = R.id.nubia_game_gpu_adreno_standard;
                                TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_gpu_adreno_standard);
                                if (textView4 != null) {
                                    i = R.id.nubia_game_voice_strengthen_close_prompt;
                                    TextView textView5 = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_voice_strengthen_close_prompt);
                                    if (textView5 != null) {
                                        i = R.id.snapdragon_adreno_gpu_custom_layout;
                                        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.snapdragon_adreno_gpu_custom_layout);
                                        if (linearLayout != null) {
                                            i = R.id.snapdragon_adreno_gpu_dialog_layout;
                                            View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.snapdragon_adreno_gpu_dialog_layout);
                                            if (findChildViewById2 != null) {
                                                return new SpreadtrumAdrenoGpuLayoutBinding((LinearLayout) view, scrollView, constraintLayout, findChildViewById, textView, textView2, textView3, textView4, textView5, linearLayout, findChildViewById2);
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

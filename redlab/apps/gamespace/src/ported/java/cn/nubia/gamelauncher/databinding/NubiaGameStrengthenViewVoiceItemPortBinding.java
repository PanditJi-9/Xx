package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class NubiaGameStrengthenViewVoiceItemPortBinding implements ViewBinding {
    public final TextView nubiaGameStrengthVoiceDesc;
    public final LinearLayout nubiaGameStrengthVoiceLayout;
    public final TextView nubiaGameStrengthVoiceType;
    private final View rootView;

    private NubiaGameStrengthenViewVoiceItemPortBinding(View view, TextView textView, LinearLayout linearLayout, TextView textView2) {
        this.rootView = view;
        this.nubiaGameStrengthVoiceDesc = textView;
        this.nubiaGameStrengthVoiceLayout = linearLayout;
        this.nubiaGameStrengthVoiceType = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static NubiaGameStrengthenViewVoiceItemPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.nubia_game_strengthen_view_voice_item_port, viewGroup);
        return bind(viewGroup);
    }

    public static NubiaGameStrengthenViewVoiceItemPortBinding bind(View view) {
        int i = R.id.nubia_game_strength_voice_desc;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_strength_voice_desc);
        if (textView != null) {
            i = R.id.nubia_game_strength_voice_layout;
            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.nubia_game_strength_voice_layout);
            if (linearLayout != null) {
                i = R.id.nubia_game_strength_voice_type;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.nubia_game_strength_voice_type);
                if (textView2 != null) {
                    return new NubiaGameStrengthenViewVoiceItemPortBinding(view, textView, linearLayout, textView2);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

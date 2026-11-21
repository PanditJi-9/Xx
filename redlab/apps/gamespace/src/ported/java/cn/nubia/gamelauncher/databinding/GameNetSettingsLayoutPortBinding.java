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
public final class GameNetSettingsLayoutPortBinding implements ViewBinding {
    public final ImageView dataLowLatencyCheckbox;
    public final RelativeLayout dataLowLatencyLayout;
    public final TextView dataLowLatencyText;
    public final ScrollView nubiaWifiLowLatencyLayout;
    private final LinearLayout rootView;
    public final TextView strengthenPageTitle;
    public final ImageView wifiLowLatencyCheckbox;
    public final RelativeLayout wifiLowLatencyLayout;
    public final TextView wifiLowLatencyText;

    private GameNetSettingsLayoutPortBinding(LinearLayout linearLayout, ImageView imageView, RelativeLayout relativeLayout, TextView textView, ScrollView scrollView, TextView textView2, ImageView imageView2, RelativeLayout relativeLayout2, TextView textView3) {
        this.rootView = linearLayout;
        this.dataLowLatencyCheckbox = imageView;
        this.dataLowLatencyLayout = relativeLayout;
        this.dataLowLatencyText = textView;
        this.nubiaWifiLowLatencyLayout = scrollView;
        this.strengthenPageTitle = textView2;
        this.wifiLowLatencyCheckbox = imageView2;
        this.wifiLowLatencyLayout = relativeLayout2;
        this.wifiLowLatencyText = textView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static GameNetSettingsLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GameNetSettingsLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.game_net_settings_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GameNetSettingsLayoutPortBinding bind(View view) {
        int i = R.id.data_low_latency_checkbox;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.data_low_latency_checkbox);
        if (imageView != null) {
            i = R.id.data_low_latency_layout;
            RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.data_low_latency_layout);
            if (relativeLayout != null) {
                i = R.id.data_low_latency_text;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.data_low_latency_text);
                if (textView != null) {
                    i = R.id.nubia_wifi_low_latency_layout;
                    ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, R.id.nubia_wifi_low_latency_layout);
                    if (scrollView != null) {
                        i = R.id.strengthen_page_title;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.strengthen_page_title);
                        if (textView2 != null) {
                            i = R.id.wifi_low_latency_checkbox;
                            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.wifi_low_latency_checkbox);
                            if (imageView2 != null) {
                                i = R.id.wifi_low_latency_layout;
                                RelativeLayout relativeLayout2 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.wifi_low_latency_layout);
                                if (relativeLayout2 != null) {
                                    i = R.id.wifi_low_latency_text;
                                    TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.wifi_low_latency_text);
                                    if (textView3 != null) {
                                        return new GameNetSettingsLayoutPortBinding((LinearLayout) view, imageView, relativeLayout, textView, scrollView, textView2, imageView2, relativeLayout2, textView3);
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

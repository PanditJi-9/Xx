package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class FragmentPreviewBinding implements ViewBinding {
    public final TextView help;
    public final LinearLayout operation;
    private final LinearLayout rootView;
    public final Button settingFind;
    public final Button studioHDMI;
    public final Button studioMiliWave;
    public final Button studioMiliWaveMirror;
    public final Button studioScan;
    public final Button studioUseTell;
    public final Button toupingSettings;

    private FragmentPreviewBinding(LinearLayout linearLayout, TextView textView, LinearLayout linearLayout2, Button button, Button button2, Button button3, Button button4, Button button5, Button button6, Button button7) {
        this.rootView = linearLayout;
        this.help = textView;
        this.operation = linearLayout2;
        this.settingFind = button;
        this.studioHDMI = button2;
        this.studioMiliWave = button3;
        this.studioMiliWaveMirror = button4;
        this.studioScan = button5;
        this.studioUseTell = button6;
        this.toupingSettings = button7;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static FragmentPreviewBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static FragmentPreviewBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.fragment_preview, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static FragmentPreviewBinding bind(View view) {
        int i = R.id.help;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.help);
        if (textView != null) {
            i = R.id.operation;
            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.operation);
            if (linearLayout != null) {
                i = R.id.setting_find;
                Button button = (Button) ViewBindings.findChildViewById(view, R.id.setting_find);
                if (button != null) {
                    i = R.id.studio_HDMI;
                    Button button2 = (Button) ViewBindings.findChildViewById(view, R.id.studio_HDMI);
                    if (button2 != null) {
                        i = R.id.studio_mili_wave;
                        Button button3 = (Button) ViewBindings.findChildViewById(view, R.id.studio_mili_wave);
                        if (button3 != null) {
                            i = R.id.studio_mili_wave_mirror;
                            Button button4 = (Button) ViewBindings.findChildViewById(view, R.id.studio_mili_wave_mirror);
                            if (button4 != null) {
                                i = R.id.studio_scan;
                                Button button5 = (Button) ViewBindings.findChildViewById(view, R.id.studio_scan);
                                if (button5 != null) {
                                    i = R.id.studio_use_tell;
                                    Button button6 = (Button) ViewBindings.findChildViewById(view, R.id.studio_use_tell);
                                    if (button6 != null) {
                                        i = R.id.touping_settings;
                                        Button button7 = (Button) ViewBindings.findChildViewById(view, R.id.touping_settings);
                                        if (button7 != null) {
                                            return new FragmentPreviewBinding((LinearLayout) view, textView, linearLayout, button, button2, button3, button4, button5, button6, button7);
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

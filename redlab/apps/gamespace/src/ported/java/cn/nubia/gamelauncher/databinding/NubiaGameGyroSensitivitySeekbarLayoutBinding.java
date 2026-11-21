package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.gamecontrolpanel.GyroSenSeekBar;

/* loaded from: classes.dex */
public final class NubiaGameGyroSensitivitySeekbarLayoutBinding implements ViewBinding {
    public final RelativeLayout gyroSenPanel;
    public final TextView gyroSenTitle;
    public final ImageView gyroSenXDown;
    public final RelativeLayout gyroSenXId;
    public final TextView gyroSenXTx;
    public final ImageView gyroSenXUp;
    public final TextView gyroSenXValue;
    public final ImageView gyroSenYDown;
    public final RelativeLayout gyroSenYId;
    public final TextView gyroSenYTx;
    public final ImageView gyroSenYUp;
    public final TextView gyroSenYValue;
    private final RelativeLayout rootView;
    public final GyroSenSeekBar seekBarGyroX;
    public final GyroSenSeekBar seekBarGyroY;
    public final TextView unsupportedMsg;

    private NubiaGameGyroSensitivitySeekbarLayoutBinding(RelativeLayout relativeLayout, RelativeLayout relativeLayout2, TextView textView, ImageView imageView, RelativeLayout relativeLayout3, TextView textView2, ImageView imageView2, TextView textView3, ImageView imageView3, RelativeLayout relativeLayout4, TextView textView4, ImageView imageView4, TextView textView5, GyroSenSeekBar gyroSenSeekBar, GyroSenSeekBar gyroSenSeekBar2, TextView textView6) {
        this.rootView = relativeLayout;
        this.gyroSenPanel = relativeLayout2;
        this.gyroSenTitle = textView;
        this.gyroSenXDown = imageView;
        this.gyroSenXId = relativeLayout3;
        this.gyroSenXTx = textView2;
        this.gyroSenXUp = imageView2;
        this.gyroSenXValue = textView3;
        this.gyroSenYDown = imageView3;
        this.gyroSenYId = relativeLayout4;
        this.gyroSenYTx = textView4;
        this.gyroSenYUp = imageView4;
        this.gyroSenYValue = textView5;
        this.seekBarGyroX = gyroSenSeekBar;
        this.seekBarGyroY = gyroSenSeekBar2;
        this.unsupportedMsg = textView6;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static NubiaGameGyroSensitivitySeekbarLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static NubiaGameGyroSensitivitySeekbarLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.nubia_game_gyro_sensitivity_seekbar_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static NubiaGameGyroSensitivitySeekbarLayoutBinding bind(View view) {
        int i = R.id.gyro_sen_panel;
        RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.gyro_sen_panel);
        if (relativeLayout != null) {
            i = R.id.gyro_sen_title;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.gyro_sen_title);
            if (textView != null) {
                i = R.id.gyro_sen_x_down;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.gyro_sen_x_down);
                if (imageView != null) {
                    i = R.id.gyro_sen_x_id;
                    RelativeLayout relativeLayout2 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.gyro_sen_x_id);
                    if (relativeLayout2 != null) {
                        i = R.id.gyro_sen_x_tx;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.gyro_sen_x_tx);
                        if (textView2 != null) {
                            i = R.id.gyro_sen_x_up;
                            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.gyro_sen_x_up);
                            if (imageView2 != null) {
                                i = R.id.gyro_sen_x_value;
                                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.gyro_sen_x_value);
                                if (textView3 != null) {
                                    i = R.id.gyro_sen_y_down;
                                    ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.gyro_sen_y_down);
                                    if (imageView3 != null) {
                                        i = R.id.gyro_sen_y_id;
                                        RelativeLayout relativeLayout3 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.gyro_sen_y_id);
                                        if (relativeLayout3 != null) {
                                            i = R.id.gyro_sen_y_tx;
                                            TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.gyro_sen_y_tx);
                                            if (textView4 != null) {
                                                i = R.id.gyro_sen_y_up;
                                                ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.gyro_sen_y_up);
                                                if (imageView4 != null) {
                                                    i = R.id.gyro_sen_y_value;
                                                    TextView textView5 = (TextView) ViewBindings.findChildViewById(view, R.id.gyro_sen_y_value);
                                                    if (textView5 != null) {
                                                        i = R.id.seekBar_gyro_x;
                                                        GyroSenSeekBar gyroSenSeekBar = (GyroSenSeekBar) ViewBindings.findChildViewById(view, R.id.seekBar_gyro_x);
                                                        if (gyroSenSeekBar != null) {
                                                            i = R.id.seekBar_gyro_y;
                                                            GyroSenSeekBar gyroSenSeekBar2 = (GyroSenSeekBar) ViewBindings.findChildViewById(view, R.id.seekBar_gyro_y);
                                                            if (gyroSenSeekBar2 != null) {
                                                                i = R.id.unsupported_msg;
                                                                TextView textView6 = (TextView) ViewBindings.findChildViewById(view, R.id.unsupported_msg);
                                                                if (textView6 != null) {
                                                                    return new NubiaGameGyroSensitivitySeekbarLayoutBinding((RelativeLayout) view, relativeLayout, textView, imageView, relativeLayout2, textView2, imageView2, textView3, imageView3, relativeLayout3, textView4, imageView4, textView5, gyroSenSeekBar, gyroSenSeekBar2, textView6);
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

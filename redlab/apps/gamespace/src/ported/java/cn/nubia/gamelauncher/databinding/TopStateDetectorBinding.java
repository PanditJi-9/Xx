package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class TopStateDetectorBinding implements ViewBinding {
    public final TextView detectorCheckTime;
    public final Guideline detectorGuidelineH;
    public final Guideline detectorGuidelineV;
    public final TextView detectorJitter;
    public final TextView detectorJitterValue;
    public final TextView detectorLatency;
    public final TextView detectorLatencyValue;
    public final TextView detectorStatus;
    public final ImageView detectorStatusBg;
    public final TextView detectorText;
    public final Button indicatorDetectorButton;
    private final ConstraintLayout rootView;
    public final TextView status;

    private TopStateDetectorBinding(ConstraintLayout constraintLayout, TextView textView, Guideline guideline, Guideline guideline2, TextView textView2, TextView textView3, TextView textView4, TextView textView5, TextView textView6, ImageView imageView, TextView textView7, Button button, TextView textView8) {
        this.rootView = constraintLayout;
        this.detectorCheckTime = textView;
        this.detectorGuidelineH = guideline;
        this.detectorGuidelineV = guideline2;
        this.detectorJitter = textView2;
        this.detectorJitterValue = textView3;
        this.detectorLatency = textView4;
        this.detectorLatencyValue = textView5;
        this.detectorStatus = textView6;
        this.detectorStatusBg = imageView;
        this.detectorText = textView7;
        this.indicatorDetectorButton = button;
        this.status = textView8;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static TopStateDetectorBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TopStateDetectorBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.top_state_detector, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TopStateDetectorBinding bind(View view) {
        int i = R.id.detector_check_time;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.detector_check_time);
        if (textView != null) {
            i = R.id.detector_guideline_h;
            Guideline guideline = (Guideline) ViewBindings.findChildViewById(view, R.id.detector_guideline_h);
            if (guideline != null) {
                i = R.id.detector_guideline_v;
                Guideline guideline2 = (Guideline) ViewBindings.findChildViewById(view, R.id.detector_guideline_v);
                if (guideline2 != null) {
                    i = R.id.detector_jitter;
                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.detector_jitter);
                    if (textView2 != null) {
                        i = R.id.detector_jitter_value;
                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.detector_jitter_value);
                        if (textView3 != null) {
                            i = R.id.detector_latency;
                            TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.detector_latency);
                            if (textView4 != null) {
                                i = R.id.detector_latency_value;
                                TextView textView5 = (TextView) ViewBindings.findChildViewById(view, R.id.detector_latency_value);
                                if (textView5 != null) {
                                    i = R.id.detector_status;
                                    TextView textView6 = (TextView) ViewBindings.findChildViewById(view, R.id.detector_status);
                                    if (textView6 != null) {
                                        i = R.id.detector_status_bg;
                                        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.detector_status_bg);
                                        if (imageView != null) {
                                            i = R.id.detector_text;
                                            TextView textView7 = (TextView) ViewBindings.findChildViewById(view, R.id.detector_text);
                                            if (textView7 != null) {
                                                i = R.id.indicator_detector_button;
                                                Button button = (Button) ViewBindings.findChildViewById(view, R.id.indicator_detector_button);
                                                if (button != null) {
                                                    i = R.id.status;
                                                    TextView textView8 = (TextView) ViewBindings.findChildViewById(view, R.id.status);
                                                    if (textView8 != null) {
                                                        return new TopStateDetectorBinding((ConstraintLayout) view, textView, guideline, guideline2, textView2, textView3, textView4, textView5, textView6, imageView, textView7, button, textView8);
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

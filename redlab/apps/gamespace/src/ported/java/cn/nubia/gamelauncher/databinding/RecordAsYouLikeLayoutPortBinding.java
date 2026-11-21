package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.gamecontrolpanel.widget.CustomSeekBar;

/* loaded from: classes.dex */
public final class RecordAsYouLikeLayoutPortBinding implements ViewBinding {
    public final TextView TitleView;
    public final View driver;
    public final LinearLayout postRecordingDurationLayout;
    public final CustomSeekBar postRecordingSeekbar;
    public final TextView randomRecordQualityHigh;
    public final TextView randomRecordQualityStandard;
    public final TextView randomRecordQualityText;
    public final ConstraintLayout recordAsYouLikeLayout;
    public final LinearLayout recordAsYouLikeQualityEndLayout;
    public final RelativeLayout recordAsYouLikeQualityLayout;
    public final CustomSeekBar recordTimeSeekbar;
    public final RelativeLayout recordingTimeLayout;
    private final ConstraintLayout rootView;

    private RecordAsYouLikeLayoutPortBinding(ConstraintLayout constraintLayout, TextView textView, View view, LinearLayout linearLayout, CustomSeekBar customSeekBar, TextView textView2, TextView textView3, TextView textView4, ConstraintLayout constraintLayout2, LinearLayout linearLayout2, RelativeLayout relativeLayout, CustomSeekBar customSeekBar2, RelativeLayout relativeLayout2) {
        this.rootView = constraintLayout;
        this.TitleView = textView;
        this.driver = view;
        this.postRecordingDurationLayout = linearLayout;
        this.postRecordingSeekbar = customSeekBar;
        this.randomRecordQualityHigh = textView2;
        this.randomRecordQualityStandard = textView3;
        this.randomRecordQualityText = textView4;
        this.recordAsYouLikeLayout = constraintLayout2;
        this.recordAsYouLikeQualityEndLayout = linearLayout2;
        this.recordAsYouLikeQualityLayout = relativeLayout;
        this.recordTimeSeekbar = customSeekBar2;
        this.recordingTimeLayout = relativeLayout2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static RecordAsYouLikeLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static RecordAsYouLikeLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.record_as_you_like_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static RecordAsYouLikeLayoutPortBinding bind(View view) {
        int i = R.id.TitleView;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.TitleView);
        if (textView != null) {
            i = R.id.driver;
            View findChildViewById = ViewBindings.findChildViewById(view, R.id.driver);
            if (findChildViewById != null) {
                i = R.id.post_recording_duration_layout;
                LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.post_recording_duration_layout);
                if (linearLayout != null) {
                    i = R.id.post_recording_seekbar;
                    CustomSeekBar customSeekBar = (CustomSeekBar) ViewBindings.findChildViewById(view, R.id.post_recording_seekbar);
                    if (customSeekBar != null) {
                        i = R.id.random_record_quality_high;
                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.random_record_quality_high);
                        if (textView2 != null) {
                            i = R.id.random_record__quality_standard;
                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.random_record__quality_standard);
                            if (textView3 != null) {
                                i = R.id.random_record_quality_text;
                                TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.random_record_quality_text);
                                if (textView4 != null) {
                                    ConstraintLayout constraintLayout = (ConstraintLayout) view;
                                    i = R.id.record_as_you_like_quality_end_layout;
                                    LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.record_as_you_like_quality_end_layout);
                                    if (linearLayout2 != null) {
                                        i = R.id.record_as_you_like_quality_layout;
                                        RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.record_as_you_like_quality_layout);
                                        if (relativeLayout != null) {
                                            i = R.id.record_time_seekbar;
                                            CustomSeekBar customSeekBar2 = (CustomSeekBar) ViewBindings.findChildViewById(view, R.id.record_time_seekbar);
                                            if (customSeekBar2 != null) {
                                                i = R.id.recording_time_layout;
                                                RelativeLayout relativeLayout2 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.recording_time_layout);
                                                if (relativeLayout2 != null) {
                                                    return new RecordAsYouLikeLayoutPortBinding(constraintLayout, textView, findChildViewById, linearLayout, customSeekBar, textView2, textView3, textView4, constraintLayout, linearLayout2, relativeLayout, customSeekBar2, relativeLayout2);
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

package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class FunctionRedmagicTimeLayoutBinding implements ViewBinding {
    public final ImageView functionRedmagicTimeDieVideoCheckbox;
    public final RelativeLayout functionRedmagicTimeDieVideoLayout;
    public final TextView functionRedmagicTimeDieVideoText;
    public final TextView functionRedmagicTimeQualityHigh;
    public final RelativeLayout functionRedmagicTimeQualityLayout;
    public final TextView functionRedmagicTimeQualityStandard;
    public final TextView functionRedmagicTimeQualityText;
    public final TextView functionRedmagicTimeRecord15s;
    public final TextView functionRedmagicTimeRecord30s;
    public final TextView functionRedmagicTimeRecord45s;
    public final TextView functionRedmagicTimeRecord60s;
    public final ImageView functionRedmagicTimeRecordAllCheckbox;
    public final RelativeLayout functionRedmagicTimeRecordAllLayout;
    public final TextView functionRedmagicTimeRecordAllText;
    public final ImageView functionRedmagicTimeRecordDieCheckbox;
    public final RelativeLayout functionRedmagicTimeRecordDieLayout;
    public final TextView functionRedmagicTimeRecordDieText;
    public final RelativeLayout functionRedmagicTimeRecordLayout;
    public final TextView functionRedmagicTimeRecordText;
    private final LinearLayout rootView;

    private FunctionRedmagicTimeLayoutBinding(LinearLayout linearLayout, ImageView imageView, RelativeLayout relativeLayout, TextView textView, TextView textView2, RelativeLayout relativeLayout2, TextView textView3, TextView textView4, TextView textView5, TextView textView6, TextView textView7, TextView textView8, ImageView imageView2, RelativeLayout relativeLayout3, TextView textView9, ImageView imageView3, RelativeLayout relativeLayout4, TextView textView10, RelativeLayout relativeLayout5, TextView textView11) {
        this.rootView = linearLayout;
        this.functionRedmagicTimeDieVideoCheckbox = imageView;
        this.functionRedmagicTimeDieVideoLayout = relativeLayout;
        this.functionRedmagicTimeDieVideoText = textView;
        this.functionRedmagicTimeQualityHigh = textView2;
        this.functionRedmagicTimeQualityLayout = relativeLayout2;
        this.functionRedmagicTimeQualityStandard = textView3;
        this.functionRedmagicTimeQualityText = textView4;
        this.functionRedmagicTimeRecord15s = textView5;
        this.functionRedmagicTimeRecord30s = textView6;
        this.functionRedmagicTimeRecord45s = textView7;
        this.functionRedmagicTimeRecord60s = textView8;
        this.functionRedmagicTimeRecordAllCheckbox = imageView2;
        this.functionRedmagicTimeRecordAllLayout = relativeLayout3;
        this.functionRedmagicTimeRecordAllText = textView9;
        this.functionRedmagicTimeRecordDieCheckbox = imageView3;
        this.functionRedmagicTimeRecordDieLayout = relativeLayout4;
        this.functionRedmagicTimeRecordDieText = textView10;
        this.functionRedmagicTimeRecordLayout = relativeLayout5;
        this.functionRedmagicTimeRecordText = textView11;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static FunctionRedmagicTimeLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static FunctionRedmagicTimeLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.function_redmagic_time_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static FunctionRedmagicTimeLayoutBinding bind(View view) {
        int i = R.id.function_redmagic_time_die_video_checkbox;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_die_video_checkbox);
        if (imageView != null) {
            i = R.id.function_redmagic_time_die_video_layout;
            RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_die_video_layout);
            if (relativeLayout != null) {
                i = R.id.function_redmagic_time_die_video_text;
                TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_die_video_text);
                if (textView != null) {
                    i = R.id.function_redmagic_time_quality_high;
                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_quality_high);
                    if (textView2 != null) {
                        i = R.id.function_redmagic_time_quality_layout;
                        RelativeLayout relativeLayout2 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_quality_layout);
                        if (relativeLayout2 != null) {
                            i = R.id.function_redmagic_time_quality_standard;
                            TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_quality_standard);
                            if (textView3 != null) {
                                i = R.id.function_redmagic_time_quality_text;
                                TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_quality_text);
                                if (textView4 != null) {
                                    i = R.id.function_redmagic_time_record_15s;
                                    TextView textView5 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_15s);
                                    if (textView5 != null) {
                                        i = R.id.function_redmagic_time_record_30s;
                                        TextView textView6 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_30s);
                                        if (textView6 != null) {
                                            i = R.id.function_redmagic_time_record_45s;
                                            TextView textView7 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_45s);
                                            if (textView7 != null) {
                                                i = R.id.function_redmagic_time_record_60s;
                                                TextView textView8 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_60s);
                                                if (textView8 != null) {
                                                    i = R.id.function_redmagic_time_record_all_checkbox;
                                                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_all_checkbox);
                                                    if (imageView2 != null) {
                                                        i = R.id.function_redmagic_time_record_all_layout;
                                                        RelativeLayout relativeLayout3 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_all_layout);
                                                        if (relativeLayout3 != null) {
                                                            i = R.id.function_redmagic_time_record_all_text;
                                                            TextView textView9 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_all_text);
                                                            if (textView9 != null) {
                                                                i = R.id.function_redmagic_time_record_die_checkbox;
                                                                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_die_checkbox);
                                                                if (imageView3 != null) {
                                                                    i = R.id.function_redmagic_time_record_die_layout;
                                                                    RelativeLayout relativeLayout4 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_die_layout);
                                                                    if (relativeLayout4 != null) {
                                                                        i = R.id.function_redmagic_time_record_die_text;
                                                                        TextView textView10 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_die_text);
                                                                        if (textView10 != null) {
                                                                            i = R.id.function_redmagic_time_record_layout;
                                                                            RelativeLayout relativeLayout5 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_layout);
                                                                            if (relativeLayout5 != null) {
                                                                                i = R.id.function_redmagic_time_record_text;
                                                                                TextView textView11 = (TextView) ViewBindings.findChildViewById(view, R.id.function_redmagic_time_record_text);
                                                                                if (textView11 != null) {
                                                                                    return new FunctionRedmagicTimeLayoutBinding((LinearLayout) view, imageView, relativeLayout, textView, textView2, relativeLayout2, textView3, textView4, textView5, textView6, textView7, textView8, imageView2, relativeLayout3, textView9, imageView3, relativeLayout4, textView10, relativeLayout5, textView11);
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
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

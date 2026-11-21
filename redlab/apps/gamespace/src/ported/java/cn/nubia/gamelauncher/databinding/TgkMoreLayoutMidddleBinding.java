package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MarqueeTextView;
import cn.nubia.tgk.widget.TgkCenterVisualEffectView;
import cn.nubia.tgk.widget.TgkSensitivityView;
import cn.nubia.tgk.widget.TgkTopVisualEffectView;
import cn.nubia.tgk.widget.TgkVisualEffectTransparencyView;

/* loaded from: classes.dex */
public final class TgkMoreLayoutMidddleBinding implements ViewBinding {
    private final RelativeLayout rootView;
    public final LinearLayout tgkCaseListView;
    public final View tgkCaseListViewDelete;
    public final View tgkCaseListViewDeleteLine;
    public final View tgkCaseListViewModify;
    public final View tgkCaseListViewPreview;
    public final View tgkCaseListViewPreviewLine;
    public final MarqueeTextView tgkCaseShow;
    public final ToggleButton tgkCaseSw;
    public final TextView tgkCaseTitle;
    public final TgkCenterVisualEffectView tgkCenterVisualEffectView;
    public final MarqueeTextView tgkHapticFeedback;
    public final Button tgkMoreCloseBtn;
    public final ScrollView tgkMoreVsScrollview;
    public final TgkSensitivityView tgkSensitivityViewL;
    public final TgkSensitivityView tgkSensitivityViewR;
    public final TgkTopVisualEffectView tgkTopVisualEffectView;
    public final TgkVisualEffectTransparencyView tgkVisualEffectTransparencyView;
    public final ToggleButton touchHapticFeedbackSw;

    private TgkMoreLayoutMidddleBinding(RelativeLayout relativeLayout, LinearLayout linearLayout, View view, View view2, View view3, View view4, View view5, MarqueeTextView marqueeTextView, ToggleButton toggleButton, TextView textView, TgkCenterVisualEffectView tgkCenterVisualEffectView, MarqueeTextView marqueeTextView2, Button button, ScrollView scrollView, TgkSensitivityView tgkSensitivityView, TgkSensitivityView tgkSensitivityView2, TgkTopVisualEffectView tgkTopVisualEffectView, TgkVisualEffectTransparencyView tgkVisualEffectTransparencyView, ToggleButton toggleButton2) {
        this.rootView = relativeLayout;
        this.tgkCaseListView = linearLayout;
        this.tgkCaseListViewDelete = view;
        this.tgkCaseListViewDeleteLine = view2;
        this.tgkCaseListViewModify = view3;
        this.tgkCaseListViewPreview = view4;
        this.tgkCaseListViewPreviewLine = view5;
        this.tgkCaseShow = marqueeTextView;
        this.tgkCaseSw = toggleButton;
        this.tgkCaseTitle = textView;
        this.tgkCenterVisualEffectView = tgkCenterVisualEffectView;
        this.tgkHapticFeedback = marqueeTextView2;
        this.tgkMoreCloseBtn = button;
        this.tgkMoreVsScrollview = scrollView;
        this.tgkSensitivityViewL = tgkSensitivityView;
        this.tgkSensitivityViewR = tgkSensitivityView2;
        this.tgkTopVisualEffectView = tgkTopVisualEffectView;
        this.tgkVisualEffectTransparencyView = tgkVisualEffectTransparencyView;
        this.touchHapticFeedbackSw = toggleButton2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static TgkMoreLayoutMidddleBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkMoreLayoutMidddleBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_more_layout_midddle, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkMoreLayoutMidddleBinding bind(View view) {
        int i = R.id.tgk_case_list_view;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.tgk_case_list_view);
        if (linearLayout != null) {
            i = R.id.tgk_case_list_view_delete;
            View findChildViewById = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_delete);
            if (findChildViewById != null) {
                i = R.id.tgk_case_list_view_delete_line;
                View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_delete_line);
                if (findChildViewById2 != null) {
                    i = R.id.tgk_case_list_view_modify;
                    View findChildViewById3 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_modify);
                    if (findChildViewById3 != null) {
                        i = R.id.tgk_case_list_view_preview;
                        View findChildViewById4 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_preview);
                        if (findChildViewById4 != null) {
                            i = R.id.tgk_case_list_view_preview_line;
                            View findChildViewById5 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_preview_line);
                            if (findChildViewById5 != null) {
                                i = R.id.tgk_case_show;
                                MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_case_show);
                                if (marqueeTextView != null) {
                                    i = R.id.tgk_case_sw;
                                    ToggleButton toggleButton = (ToggleButton) ViewBindings.findChildViewById(view, R.id.tgk_case_sw);
                                    if (toggleButton != null) {
                                        i = R.id.tgk_case_title;
                                        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tgk_case_title);
                                        if (textView != null) {
                                            i = R.id.tgk_center_visual_effect_view;
                                            TgkCenterVisualEffectView tgkCenterVisualEffectView = (TgkCenterVisualEffectView) ViewBindings.findChildViewById(view, R.id.tgk_center_visual_effect_view);
                                            if (tgkCenterVisualEffectView != null) {
                                                i = R.id.tgk_haptic_feedback;
                                                MarqueeTextView marqueeTextView2 = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_haptic_feedback);
                                                if (marqueeTextView2 != null) {
                                                    i = R.id.tgk_more_close_btn;
                                                    Button button = (Button) ViewBindings.findChildViewById(view, R.id.tgk_more_close_btn);
                                                    if (button != null) {
                                                        i = R.id.tgk_more_vs_scrollview;
                                                        ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, R.id.tgk_more_vs_scrollview);
                                                        if (scrollView != null) {
                                                            i = R.id.tgk_sensitivity_view_l;
                                                            TgkSensitivityView tgkSensitivityView = (TgkSensitivityView) ViewBindings.findChildViewById(view, R.id.tgk_sensitivity_view_l);
                                                            if (tgkSensitivityView != null) {
                                                                i = R.id.tgk_sensitivity_view_r;
                                                                TgkSensitivityView tgkSensitivityView2 = (TgkSensitivityView) ViewBindings.findChildViewById(view, R.id.tgk_sensitivity_view_r);
                                                                if (tgkSensitivityView2 != null) {
                                                                    i = R.id.tgk_top_visual_effect_view;
                                                                    TgkTopVisualEffectView tgkTopVisualEffectView = (TgkTopVisualEffectView) ViewBindings.findChildViewById(view, R.id.tgk_top_visual_effect_view);
                                                                    if (tgkTopVisualEffectView != null) {
                                                                        i = R.id.tgk_visual_effect_transparency_view;
                                                                        TgkVisualEffectTransparencyView tgkVisualEffectTransparencyView = (TgkVisualEffectTransparencyView) ViewBindings.findChildViewById(view, R.id.tgk_visual_effect_transparency_view);
                                                                        if (tgkVisualEffectTransparencyView != null) {
                                                                            i = R.id.touch_haptic_feedback_sw;
                                                                            ToggleButton toggleButton2 = (ToggleButton) ViewBindings.findChildViewById(view, R.id.touch_haptic_feedback_sw);
                                                                            if (toggleButton2 != null) {
                                                                                return new TgkMoreLayoutMidddleBinding((RelativeLayout) view, linearLayout, findChildViewById, findChildViewById2, findChildViewById3, findChildViewById4, findChildViewById5, marqueeTextView, toggleButton, textView, tgkCenterVisualEffectView, marqueeTextView2, button, scrollView, tgkSensitivityView, tgkSensitivityView2, tgkTopVisualEffectView, tgkVisualEffectTransparencyView, toggleButton2);
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

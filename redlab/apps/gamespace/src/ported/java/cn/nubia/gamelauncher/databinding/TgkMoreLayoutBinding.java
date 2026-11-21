package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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
public final class TgkMoreLayoutBinding implements ViewBinding {
    private final RelativeLayout rootView;
    public final MarqueeTextView tgkCaseListDeleteTextview;
    public final MarqueeTextView tgkCaseListModifyTextview;
    public final MarqueeTextView tgkCaseListPreviewTextview;
    public final LinearLayout tgkCaseListView;
    public final View tgkCaseListViewDelete;
    public final View tgkCaseListViewDeleteLine;
    public final View tgkCaseListViewModify;
    public final View tgkCaseListViewMore;
    public final View tgkCaseListViewPreview;
    public final View tgkCaseListViewPreviewLine;
    public final MarqueeTextView tgkCaseShow;
    public final ToggleButton tgkCaseShowSw;
    public final View tgkCaseTitle;
    public final TgkCenterVisualEffectView tgkCenterVisualEffectView;
    public final MarqueeTextView tgkHapticFeedback;
    public final LinearLayout tgkMoreBtnsView;
    public final Button tgkMoreCloseBtn;
    public final RelativeLayout tgkMoreView;
    public final ScrollView tgkMoreVsScrollview;
    public final TgkSensitivityView tgkSensitivityViewL;
    public final TgkSensitivityView tgkSensitivityViewR;
    public final TgkTopVisualEffectView tgkTopVisualEffectView;
    public final TgkVisualEffectTransparencyView tgkVisualEffectTransparencyView;
    public final ToggleButton touchHapticFeedbackSw;

    private TgkMoreLayoutBinding(RelativeLayout relativeLayout, MarqueeTextView marqueeTextView, MarqueeTextView marqueeTextView2, MarqueeTextView marqueeTextView3, LinearLayout linearLayout, View view, View view2, View view3, View view4, View view5, View view6, MarqueeTextView marqueeTextView4, ToggleButton toggleButton, View view7, TgkCenterVisualEffectView tgkCenterVisualEffectView, MarqueeTextView marqueeTextView5, LinearLayout linearLayout2, Button button, RelativeLayout relativeLayout2, ScrollView scrollView, TgkSensitivityView tgkSensitivityView, TgkSensitivityView tgkSensitivityView2, TgkTopVisualEffectView tgkTopVisualEffectView, TgkVisualEffectTransparencyView tgkVisualEffectTransparencyView, ToggleButton toggleButton2) {
        this.rootView = relativeLayout;
        this.tgkCaseListDeleteTextview = marqueeTextView;
        this.tgkCaseListModifyTextview = marqueeTextView2;
        this.tgkCaseListPreviewTextview = marqueeTextView3;
        this.tgkCaseListView = linearLayout;
        this.tgkCaseListViewDelete = view;
        this.tgkCaseListViewDeleteLine = view2;
        this.tgkCaseListViewModify = view3;
        this.tgkCaseListViewMore = view4;
        this.tgkCaseListViewPreview = view5;
        this.tgkCaseListViewPreviewLine = view6;
        this.tgkCaseShow = marqueeTextView4;
        this.tgkCaseShowSw = toggleButton;
        this.tgkCaseTitle = view7;
        this.tgkCenterVisualEffectView = tgkCenterVisualEffectView;
        this.tgkHapticFeedback = marqueeTextView5;
        this.tgkMoreBtnsView = linearLayout2;
        this.tgkMoreCloseBtn = button;
        this.tgkMoreView = relativeLayout2;
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

    public static TgkMoreLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkMoreLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_more_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkMoreLayoutBinding bind(View view) {
        MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_case_list_delete_textview);
        MarqueeTextView marqueeTextView2 = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_case_list_modify_textview);
        MarqueeTextView marqueeTextView3 = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_case_list_preview_textview);
        int i = R.id.tgk_case_list_view;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.tgk_case_list_view);
        if (linearLayout != null) {
            View findChildViewById = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_delete);
            View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_delete_line);
            View findChildViewById3 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_modify);
            View findChildViewById4 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_more);
            View findChildViewById5 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_preview);
            View findChildViewById6 = ViewBindings.findChildViewById(view, R.id.tgk_case_list_view_preview_line);
            i = R.id.tgk_case_show;
            MarqueeTextView marqueeTextView4 = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_case_show);
            if (marqueeTextView4 != null) {
                i = R.id.tgk_case_show_sw;
                ToggleButton toggleButton = (ToggleButton) ViewBindings.findChildViewById(view, R.id.tgk_case_show_sw);
                if (toggleButton != null) {
                    i = R.id.tgk_case_title;
                    View findChildViewById7 = ViewBindings.findChildViewById(view, R.id.tgk_case_title);
                    if (findChildViewById7 != null) {
                        i = R.id.tgk_center_visual_effect_view;
                        TgkCenterVisualEffectView tgkCenterVisualEffectView = (TgkCenterVisualEffectView) ViewBindings.findChildViewById(view, R.id.tgk_center_visual_effect_view);
                        if (tgkCenterVisualEffectView != null) {
                            i = R.id.tgk_haptic_feedback;
                            MarqueeTextView marqueeTextView5 = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_haptic_feedback);
                            if (marqueeTextView5 != null) {
                                LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.tgk_more_btns_view);
                                i = R.id.tgk_more_close_btn;
                                Button button = (Button) ViewBindings.findChildViewById(view, R.id.tgk_more_close_btn);
                                if (button != null) {
                                    RelativeLayout relativeLayout = (RelativeLayout) view;
                                    ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, R.id.tgk_more_vs_scrollview);
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
                                                        return new TgkMoreLayoutBinding(relativeLayout, marqueeTextView, marqueeTextView2, marqueeTextView3, linearLayout, findChildViewById, findChildViewById2, findChildViewById3, findChildViewById4, findChildViewById5, findChildViewById6, marqueeTextView4, toggleButton, findChildViewById7, tgkCenterVisualEffectView, marqueeTextView5, linearLayout2, button, relativeLayout, scrollView, tgkSensitivityView, tgkSensitivityView2, tgkTopVisualEffectView, tgkVisualEffectTransparencyView, toggleButton2);
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

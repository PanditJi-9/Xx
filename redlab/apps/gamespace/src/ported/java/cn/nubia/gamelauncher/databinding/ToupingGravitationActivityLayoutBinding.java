package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ToupingGravitationActivityLayoutBinding implements ViewBinding {
    public final TextView bigScreenPlayView;
    public final TextView computerPlayView;
    public final TextView hostModeSwitchView;
    public final TextView instructionContentView;
    public final TextView instructionTitleView;
    public final ToupingGravitationTitleBinding layoutTitleBar;
    public final LinearLayout llBottomFlag;
    public final ConstraintLayout llTmp1;
    public final ConstraintLayout llTmp2;
    public final ImageView playCategoryPreview;
    private final ConstraintLayout rootView;
    public final ScrollView scTextCenter;
    public final ImageView scanProjectLayout;
    public final ImageView searchProjectLayout;
    public final TextView sportEquipmentKeyboard;
    public final TextView sportEquipmentLabel;
    public final TextView sportEquipmentMonitor;
    public final TextView sportEquipmentMouse;
    public final ConstraintLayout toupingGravitationLeftLayout;
    public final ConstraintLayout toupingGravitationRightLayout;
    public final TextView toupingSecretSwitchView;
    public final View vLine;

    private ToupingGravitationActivityLayoutBinding(ConstraintLayout constraintLayout, TextView textView, TextView textView2, TextView textView3, TextView textView4, TextView textView5, ToupingGravitationTitleBinding toupingGravitationTitleBinding, LinearLayout linearLayout, ConstraintLayout constraintLayout2, ConstraintLayout constraintLayout3, ImageView imageView, ScrollView scrollView, ImageView imageView2, ImageView imageView3, TextView textView6, TextView textView7, TextView textView8, TextView textView9, ConstraintLayout constraintLayout4, ConstraintLayout constraintLayout5, TextView textView10, View view) {
        this.rootView = constraintLayout;
        this.bigScreenPlayView = textView;
        this.computerPlayView = textView2;
        this.hostModeSwitchView = textView3;
        this.instructionContentView = textView4;
        this.instructionTitleView = textView5;
        this.layoutTitleBar = toupingGravitationTitleBinding;
        this.llBottomFlag = linearLayout;
        this.llTmp1 = constraintLayout2;
        this.llTmp2 = constraintLayout3;
        this.playCategoryPreview = imageView;
        this.scTextCenter = scrollView;
        this.scanProjectLayout = imageView2;
        this.searchProjectLayout = imageView3;
        this.sportEquipmentKeyboard = textView6;
        this.sportEquipmentLabel = textView7;
        this.sportEquipmentMonitor = textView8;
        this.sportEquipmentMouse = textView9;
        this.toupingGravitationLeftLayout = constraintLayout4;
        this.toupingGravitationRightLayout = constraintLayout5;
        this.toupingSecretSwitchView = textView10;
        this.vLine = view;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static ToupingGravitationActivityLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ToupingGravitationActivityLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.touping_gravitation_activity_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ToupingGravitationActivityLayoutBinding bind(View view) {
        int i = R.id.big_screen_play_view;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.big_screen_play_view);
        if (textView != null) {
            i = R.id.computer_play_view;
            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.computer_play_view);
            if (textView2 != null) {
                i = R.id.host_mode_switch_view;
                TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.host_mode_switch_view);
                if (textView3 != null) {
                    i = R.id.instruction_content_view;
                    TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.instruction_content_view);
                    if (textView4 != null) {
                        i = R.id.instruction_title_view;
                        TextView textView5 = (TextView) ViewBindings.findChildViewById(view, R.id.instruction_title_view);
                        if (textView5 != null) {
                            i = R.id.layout_title_bar;
                            View findChildViewById = ViewBindings.findChildViewById(view, R.id.layout_title_bar);
                            if (findChildViewById != null) {
                                ToupingGravitationTitleBinding bind = ToupingGravitationTitleBinding.bind(findChildViewById);
                                i = R.id.ll_bottom_flag;
                                LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.ll_bottom_flag);
                                if (linearLayout != null) {
                                    i = R.id.ll_tmp_1;
                                    ConstraintLayout constraintLayout = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.ll_tmp_1);
                                    if (constraintLayout != null) {
                                        i = R.id.ll_tmp_2;
                                        ConstraintLayout constraintLayout2 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.ll_tmp_2);
                                        if (constraintLayout2 != null) {
                                            i = R.id.play_category_preview;
                                            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.play_category_preview);
                                            if (imageView != null) {
                                                i = R.id.sc_text_center;
                                                ScrollView scrollView = (ScrollView) ViewBindings.findChildViewById(view, R.id.sc_text_center);
                                                if (scrollView != null) {
                                                    i = R.id.scan_project_layout;
                                                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.scan_project_layout);
                                                    if (imageView2 != null) {
                                                        i = R.id.search_project_layout;
                                                        ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.search_project_layout);
                                                        if (imageView3 != null) {
                                                            i = R.id.sport_equipment_keyboard;
                                                            TextView textView6 = (TextView) ViewBindings.findChildViewById(view, R.id.sport_equipment_keyboard);
                                                            if (textView6 != null) {
                                                                i = R.id.sport_equipment_label;
                                                                TextView textView7 = (TextView) ViewBindings.findChildViewById(view, R.id.sport_equipment_label);
                                                                if (textView7 != null) {
                                                                    i = R.id.sport_equipment_monitor;
                                                                    TextView textView8 = (TextView) ViewBindings.findChildViewById(view, R.id.sport_equipment_monitor);
                                                                    if (textView8 != null) {
                                                                        i = R.id.sport_equipment_mouse;
                                                                        TextView textView9 = (TextView) ViewBindings.findChildViewById(view, R.id.sport_equipment_mouse);
                                                                        if (textView9 != null) {
                                                                            i = R.id.touping_gravitation_left_layout;
                                                                            ConstraintLayout constraintLayout3 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.touping_gravitation_left_layout);
                                                                            if (constraintLayout3 != null) {
                                                                                i = R.id.touping_gravitation_right_layout;
                                                                                ConstraintLayout constraintLayout4 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.touping_gravitation_right_layout);
                                                                                if (constraintLayout4 != null) {
                                                                                    i = R.id.touping_secret_switch_view;
                                                                                    TextView textView10 = (TextView) ViewBindings.findChildViewById(view, R.id.touping_secret_switch_view);
                                                                                    if (textView10 != null) {
                                                                                        return new ToupingGravitationActivityLayoutBinding((ConstraintLayout) view, textView, textView2, textView3, textView4, textView5, bind, linearLayout, constraintLayout, constraintLayout2, imageView, scrollView, imageView2, imageView3, textView6, textView7, textView8, textView9, constraintLayout3, constraintLayout4, textView10, ViewBindings.findChildViewById(view, R.id.v_line));
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
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

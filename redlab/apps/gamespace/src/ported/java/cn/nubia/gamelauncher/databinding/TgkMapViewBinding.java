package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ToggleButton;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.FakeFloatView;
import cn.nubia.tgk.widget.LampCaseListPopView;
import cn.nubia.tgk.widget.MarqueeTextView;
import cn.nubia.tgk.widget.TgkCaseListPopView;
import cn.nubia.tgk.widget.TgkMenuPopView;
import cn.nubia.tgk.widget.TgkMenuView;

/* loaded from: classes.dex */
public final class TgkMapViewBinding implements ViewBinding {
    public final FakeFloatView lLinkBall;
    public final FakeFloatView leftBallOne;
    public final FakeFloatView leftBallTwo;
    public final LampCaseListPopView lmapPopCaseListView;
    public final FakeFloatView mLinkBall;
    public final FakeFloatView middleBallOne;
    public final FakeFloatView middleBallTwo;
    public final FakeFloatView rLinkBall;
    public final FakeFloatView rightBallOne;
    public final FakeFloatView rightBallTwo;
    private final FrameLayout rootView;
    public final ImageButton tgkCloseBtn;
    public final LinearLayout tgkHeadView;
    public final TgkMenuView tgkLMenu;
    public final TgkMenuView tgkMMenu;
    public final View tgkMainHelp;
    public final ToggleButton tgkMainSwitch;
    public final FrameLayout tgkMainView;
    public final View tgkMoreOpenBtn;
    public final ViewStub tgkMoreVs;
    public final ViewStub tgkMultViewVs;
    public final TgkCaseListPopView tgkPopCaseListView;
    public final TgkMenuPopView tgkPopMenuView;
    public final ViewStub tgkPreviewImgVs;
    public final TgkMenuView tgkRMenu;
    public final ViewStub tgkRenameVs;
    public final ViewStub tgkRotationGuidancePanel;
    public final MarqueeTextView tgkTitle;

    private TgkMapViewBinding(FrameLayout frameLayout, FakeFloatView fakeFloatView, FakeFloatView fakeFloatView2, FakeFloatView fakeFloatView3, LampCaseListPopView lampCaseListPopView, FakeFloatView fakeFloatView4, FakeFloatView fakeFloatView5, FakeFloatView fakeFloatView6, FakeFloatView fakeFloatView7, FakeFloatView fakeFloatView8, FakeFloatView fakeFloatView9, ImageButton imageButton, LinearLayout linearLayout, TgkMenuView tgkMenuView, TgkMenuView tgkMenuView2, View view, ToggleButton toggleButton, FrameLayout frameLayout2, View view2, ViewStub viewStub, ViewStub viewStub2, TgkCaseListPopView tgkCaseListPopView, TgkMenuPopView tgkMenuPopView, ViewStub viewStub3, TgkMenuView tgkMenuView3, ViewStub viewStub4, ViewStub viewStub5, MarqueeTextView marqueeTextView) {
        this.rootView = frameLayout;
        this.lLinkBall = fakeFloatView;
        this.leftBallOne = fakeFloatView2;
        this.leftBallTwo = fakeFloatView3;
        this.lmapPopCaseListView = lampCaseListPopView;
        this.mLinkBall = fakeFloatView4;
        this.middleBallOne = fakeFloatView5;
        this.middleBallTwo = fakeFloatView6;
        this.rLinkBall = fakeFloatView7;
        this.rightBallOne = fakeFloatView8;
        this.rightBallTwo = fakeFloatView9;
        this.tgkCloseBtn = imageButton;
        this.tgkHeadView = linearLayout;
        this.tgkLMenu = tgkMenuView;
        this.tgkMMenu = tgkMenuView2;
        this.tgkMainHelp = view;
        this.tgkMainSwitch = toggleButton;
        this.tgkMainView = frameLayout2;
        this.tgkMoreOpenBtn = view2;
        this.tgkMoreVs = viewStub;
        this.tgkMultViewVs = viewStub2;
        this.tgkPopCaseListView = tgkCaseListPopView;
        this.tgkPopMenuView = tgkMenuPopView;
        this.tgkPreviewImgVs = viewStub3;
        this.tgkRMenu = tgkMenuView3;
        this.tgkRenameVs = viewStub4;
        this.tgkRotationGuidancePanel = viewStub5;
        this.tgkTitle = marqueeTextView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static TgkMapViewBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkMapViewBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_map_view, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkMapViewBinding bind(View view) {
        int i = R.id.l_link_ball;
        FakeFloatView fakeFloatView = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.l_link_ball);
        if (fakeFloatView != null) {
            i = R.id.left_ball_one;
            FakeFloatView fakeFloatView2 = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.left_ball_one);
            if (fakeFloatView2 != null) {
                i = R.id.left_ball_two;
                FakeFloatView fakeFloatView3 = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.left_ball_two);
                if (fakeFloatView3 != null) {
                    i = R.id.lmap_pop_case_list_view;
                    LampCaseListPopView lampCaseListPopView = (LampCaseListPopView) ViewBindings.findChildViewById(view, R.id.lmap_pop_case_list_view);
                    if (lampCaseListPopView != null) {
                        i = R.id.m_link_ball;
                        FakeFloatView fakeFloatView4 = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.m_link_ball);
                        if (fakeFloatView4 != null) {
                            i = R.id.middle_ball_one;
                            FakeFloatView fakeFloatView5 = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.middle_ball_one);
                            if (fakeFloatView5 != null) {
                                i = R.id.middle_ball_two;
                                FakeFloatView fakeFloatView6 = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.middle_ball_two);
                                if (fakeFloatView6 != null) {
                                    i = R.id.r_link_ball;
                                    FakeFloatView fakeFloatView7 = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.r_link_ball);
                                    if (fakeFloatView7 != null) {
                                        i = R.id.right_ball_one;
                                        FakeFloatView fakeFloatView8 = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.right_ball_one);
                                        if (fakeFloatView8 != null) {
                                            i = R.id.right_ball_two;
                                            FakeFloatView fakeFloatView9 = (FakeFloatView) ViewBindings.findChildViewById(view, R.id.right_ball_two);
                                            if (fakeFloatView9 != null) {
                                                i = R.id.tgk_close_btn;
                                                ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, R.id.tgk_close_btn);
                                                if (imageButton != null) {
                                                    i = R.id.tgk_head_view;
                                                    LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.tgk_head_view);
                                                    if (linearLayout != null) {
                                                        i = R.id.tgk_l_menu;
                                                        TgkMenuView tgkMenuView = (TgkMenuView) ViewBindings.findChildViewById(view, R.id.tgk_l_menu);
                                                        if (tgkMenuView != null) {
                                                            i = R.id.tgk_m_menu;
                                                            TgkMenuView tgkMenuView2 = (TgkMenuView) ViewBindings.findChildViewById(view, R.id.tgk_m_menu);
                                                            if (tgkMenuView2 != null) {
                                                                i = R.id.tgk_main_help;
                                                                View findChildViewById = ViewBindings.findChildViewById(view, R.id.tgk_main_help);
                                                                if (findChildViewById != null) {
                                                                    i = R.id.tgk_main_switch;
                                                                    ToggleButton toggleButton = (ToggleButton) ViewBindings.findChildViewById(view, R.id.tgk_main_switch);
                                                                    if (toggleButton != null) {
                                                                        i = R.id.tgk_main_view;
                                                                        FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, R.id.tgk_main_view);
                                                                        if (frameLayout != null) {
                                                                            i = R.id.tgk_more_open_btn;
                                                                            View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.tgk_more_open_btn);
                                                                            if (findChildViewById2 != null) {
                                                                                i = R.id.tgk_more_vs;
                                                                                ViewStub viewStub = (ViewStub) ViewBindings.findChildViewById(view, R.id.tgk_more_vs);
                                                                                if (viewStub != null) {
                                                                                    i = R.id.tgk_mult_view_vs;
                                                                                    ViewStub viewStub2 = (ViewStub) ViewBindings.findChildViewById(view, R.id.tgk_mult_view_vs);
                                                                                    if (viewStub2 != null) {
                                                                                        i = R.id.tgk_pop_case_list_view;
                                                                                        TgkCaseListPopView tgkCaseListPopView = (TgkCaseListPopView) ViewBindings.findChildViewById(view, R.id.tgk_pop_case_list_view);
                                                                                        if (tgkCaseListPopView != null) {
                                                                                            i = R.id.tgk_pop_menu_view;
                                                                                            TgkMenuPopView tgkMenuPopView = (TgkMenuPopView) ViewBindings.findChildViewById(view, R.id.tgk_pop_menu_view);
                                                                                            if (tgkMenuPopView != null) {
                                                                                                i = R.id.tgk_preview_img_vs;
                                                                                                ViewStub viewStub3 = (ViewStub) ViewBindings.findChildViewById(view, R.id.tgk_preview_img_vs);
                                                                                                if (viewStub3 != null) {
                                                                                                    i = R.id.tgk_r_menu;
                                                                                                    TgkMenuView tgkMenuView3 = (TgkMenuView) ViewBindings.findChildViewById(view, R.id.tgk_r_menu);
                                                                                                    if (tgkMenuView3 != null) {
                                                                                                        i = R.id.tgk_rename_vs;
                                                                                                        ViewStub viewStub4 = (ViewStub) ViewBindings.findChildViewById(view, R.id.tgk_rename_vs);
                                                                                                        if (viewStub4 != null) {
                                                                                                            i = R.id.tgk_rotation_guidance_panel;
                                                                                                            ViewStub viewStub5 = (ViewStub) ViewBindings.findChildViewById(view, R.id.tgk_rotation_guidance_panel);
                                                                                                            if (viewStub5 != null) {
                                                                                                                i = R.id.tgk_title;
                                                                                                                MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_title);
                                                                                                                if (marqueeTextView != null) {
                                                                                                                    return new TgkMapViewBinding((FrameLayout) view, fakeFloatView, fakeFloatView2, fakeFloatView3, lampCaseListPopView, fakeFloatView4, fakeFloatView5, fakeFloatView6, fakeFloatView7, fakeFloatView8, fakeFloatView9, imageButton, linearLayout, tgkMenuView, tgkMenuView2, findChildViewById, toggleButton, frameLayout, findChildViewById2, viewStub, viewStub2, tgkCaseListPopView, tgkMenuPopView, viewStub3, tgkMenuView3, viewStub4, viewStub5, marqueeTextView);
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

package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.LargeGameTabView;
import dev.redlab.gamespace.view.MarqueeTextView;
import dev.redlab.gamespace.view.SelectedButton;

/* loaded from: classes.dex */
public final class LargeGameLayoutBinding implements ViewBinding {
    public final ImageView actionbarBg;
    public final Space baseLineGameExplain;
    public final Space baseLineTopLayout;
    public final ConstraintLayout content;
    public final ConstraintLayout explainContent;
    public final TextView explainDetails;
    public final ImageView gameExplainBg;
    public final TextView gameExplainTitle;
    public final ImageView iv3aGamePcCard;
    public final ImageView iv3aGameStreamCard;
    public final ImageView ivTabContentGame1;
    public final ImageView ivTabContentGame2;
    public final ImageView ivTabContentGame3;
    public final ImageView largeGameExplain;
    public final MarqueeTextView largePop;
    public final LinearLayout llTabContentGame;
    public final NestedScrollView mainContentSc;
    public final ImageView returnIcon;
    private final ConstraintLayout rootView;
    public final NestedScrollView scrollExplain;
    public final SelectedButton selectedKeyboard;
    public final SelectedButton selectedMonitor;
    public final SelectedButton selectedMouse;
    public final SelectedButton selectedPlayHandle;
    public final SelectedButton selectedPlayMirror;
    public final SelectedButton selectedPlayMouse;
    public final ImageView tabContent;
    public final LargeGameTabView tabView;
    public final TextView title;
    public final View titleGear;
    public final View titleServices;
    public final ConstraintLayout topRemoteGameLayout;
    public final TextView tv3aGamePcCardSecTitle;
    public final TextView tv3aGamePcCardTitle;
    public final TextView tv3aGameStreamCardTitle;
    public final TextView tvLargeGameTips1;
    public final TextView tvLargeGameTips2;

    private LargeGameLayoutBinding(ConstraintLayout constraintLayout, ImageView imageView, Space space, Space space2, ConstraintLayout constraintLayout2, ConstraintLayout constraintLayout3, TextView textView, ImageView imageView2, TextView textView2, ImageView imageView3, ImageView imageView4, ImageView imageView5, ImageView imageView6, ImageView imageView7, ImageView imageView8, MarqueeTextView marqueeTextView, LinearLayout linearLayout, NestedScrollView nestedScrollView, ImageView imageView9, NestedScrollView nestedScrollView2, SelectedButton selectedButton, SelectedButton selectedButton2, SelectedButton selectedButton3, SelectedButton selectedButton4, SelectedButton selectedButton5, SelectedButton selectedButton6, ImageView imageView10, LargeGameTabView largeGameTabView, TextView textView3, View view, View view2, ConstraintLayout constraintLayout4, TextView textView4, TextView textView5, TextView textView6, TextView textView7, TextView textView8) {
        this.rootView = constraintLayout;
        this.actionbarBg = imageView;
        this.baseLineGameExplain = space;
        this.baseLineTopLayout = space2;
        this.content = constraintLayout2;
        this.explainContent = constraintLayout3;
        this.explainDetails = textView;
        this.gameExplainBg = imageView2;
        this.gameExplainTitle = textView2;
        this.iv3aGamePcCard = imageView3;
        this.iv3aGameStreamCard = imageView4;
        this.ivTabContentGame1 = imageView5;
        this.ivTabContentGame2 = imageView6;
        this.ivTabContentGame3 = imageView7;
        this.largeGameExplain = imageView8;
        this.largePop = marqueeTextView;
        this.llTabContentGame = linearLayout;
        this.mainContentSc = nestedScrollView;
        this.returnIcon = imageView9;
        this.scrollExplain = nestedScrollView2;
        this.selectedKeyboard = selectedButton;
        this.selectedMonitor = selectedButton2;
        this.selectedMouse = selectedButton3;
        this.selectedPlayHandle = selectedButton4;
        this.selectedPlayMirror = selectedButton5;
        this.selectedPlayMouse = selectedButton6;
        this.tabContent = imageView10;
        this.tabView = largeGameTabView;
        this.title = textView3;
        this.titleGear = view;
        this.titleServices = view2;
        this.topRemoteGameLayout = constraintLayout4;
        this.tv3aGamePcCardSecTitle = textView4;
        this.tv3aGamePcCardTitle = textView5;
        this.tv3aGameStreamCardTitle = textView6;
        this.tvLargeGameTips1 = textView7;
        this.tvLargeGameTips2 = textView8;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static LargeGameLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static LargeGameLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.large_game_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static LargeGameLayoutBinding bind(View view) {
        int i = R.id.actionbar_bg;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.actionbar_bg);
        if (imageView != null) {
            i = R.id.base_line_game_explain;
            Space space = (Space) ViewBindings.findChildViewById(view, R.id.base_line_game_explain);
            if (space != null) {
                i = R.id.base_line_top_layout;
                Space space2 = (Space) ViewBindings.findChildViewById(view, R.id.base_line_top_layout);
                if (space2 != null) {
                    ConstraintLayout constraintLayout = (ConstraintLayout) view;
                    i = R.id.explain_content;
                    ConstraintLayout constraintLayout2 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.explain_content);
                    if (constraintLayout2 != null) {
                        i = R.id.explain_details;
                        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.explain_details);
                        if (textView != null) {
                            i = R.id.game_explain_bg;
                            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.game_explain_bg);
                            if (imageView2 != null) {
                                i = R.id.game_explain_title;
                                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.game_explain_title);
                                if (textView2 != null) {
                                    i = R.id.iv_3a_game_pc_card;
                                    ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_3a_game_pc_card);
                                    if (imageView3 != null) {
                                        i = R.id.iv_3a_game_stream_card;
                                        ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_3a_game_stream_card);
                                        if (imageView4 != null) {
                                            i = R.id.iv_tab_content_game_1;
                                            ImageView imageView5 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_tab_content_game_1);
                                            if (imageView5 != null) {
                                                i = R.id.iv_tab_content_game_2;
                                                ImageView imageView6 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_tab_content_game_2);
                                                if (imageView6 != null) {
                                                    i = R.id.iv_tab_content_game_3;
                                                    ImageView imageView7 = (ImageView) ViewBindings.findChildViewById(view, R.id.iv_tab_content_game_3);
                                                    if (imageView7 != null) {
                                                        i = R.id.large_game_explain;
                                                        ImageView imageView8 = (ImageView) ViewBindings.findChildViewById(view, R.id.large_game_explain);
                                                        if (imageView8 != null) {
                                                            MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.large_pop);
                                                            i = R.id.ll_tab_content_game;
                                                            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.ll_tab_content_game);
                                                            if (linearLayout != null) {
                                                                NestedScrollView nestedScrollView = (NestedScrollView) ViewBindings.findChildViewById(view, R.id.main_content_sc);
                                                                i = R.id.return_icon;
                                                                ImageView imageView9 = (ImageView) ViewBindings.findChildViewById(view, R.id.return_icon);
                                                                if (imageView9 != null) {
                                                                    i = R.id.scroll_explain;
                                                                    NestedScrollView nestedScrollView2 = (NestedScrollView) ViewBindings.findChildViewById(view, R.id.scroll_explain);
                                                                    if (nestedScrollView2 != null) {
                                                                        i = R.id.selected_keyboard;
                                                                        SelectedButton selectedButton = (SelectedButton) ViewBindings.findChildViewById(view, R.id.selected_keyboard);
                                                                        if (selectedButton != null) {
                                                                            i = R.id.selected_monitor;
                                                                            SelectedButton selectedButton2 = (SelectedButton) ViewBindings.findChildViewById(view, R.id.selected_monitor);
                                                                            if (selectedButton2 != null) {
                                                                                i = R.id.selected_mouse;
                                                                                SelectedButton selectedButton3 = (SelectedButton) ViewBindings.findChildViewById(view, R.id.selected_mouse);
                                                                                if (selectedButton3 != null) {
                                                                                    i = R.id.selected_play_handle;
                                                                                    SelectedButton selectedButton4 = (SelectedButton) ViewBindings.findChildViewById(view, R.id.selected_play_handle);
                                                                                    if (selectedButton4 != null) {
                                                                                        i = R.id.selected_play_mirror;
                                                                                        SelectedButton selectedButton5 = (SelectedButton) ViewBindings.findChildViewById(view, R.id.selected_play_mirror);
                                                                                        if (selectedButton5 != null) {
                                                                                            i = R.id.selected_play_mouse;
                                                                                            SelectedButton selectedButton6 = (SelectedButton) ViewBindings.findChildViewById(view, R.id.selected_play_mouse);
                                                                                            if (selectedButton6 != null) {
                                                                                                i = R.id.tab_content;
                                                                                                ImageView imageView10 = (ImageView) ViewBindings.findChildViewById(view, R.id.tab_content);
                                                                                                if (imageView10 != null) {
                                                                                                    i = R.id.tab_view;
                                                                                                    LargeGameTabView largeGameTabView = (LargeGameTabView) ViewBindings.findChildViewById(view, R.id.tab_view);
                                                                                                    if (largeGameTabView != null) {
                                                                                                        i = R.id.title;
                                                                                                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.title);
                                                                                                        if (textView3 != null) {
                                                                                                            i = R.id.title_gear;
                                                                                                            View findChildViewById = ViewBindings.findChildViewById(view, R.id.title_gear);
                                                                                                            if (findChildViewById != null) {
                                                                                                                i = R.id.title_services;
                                                                                                                View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.title_services);
                                                                                                                if (findChildViewById2 != null) {
                                                                                                                    i = R.id.top_remote_game_layout;
                                                                                                                    ConstraintLayout constraintLayout3 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.top_remote_game_layout);
                                                                                                                    if (constraintLayout3 != null) {
                                                                                                                        i = R.id.tv_3a_game_pc_card_sec_title;
                                                                                                                        TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_3a_game_pc_card_sec_title);
                                                                                                                        if (textView4 != null) {
                                                                                                                            i = R.id.tv_3a_game_pc_card_title;
                                                                                                                            TextView textView5 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_3a_game_pc_card_title);
                                                                                                                            if (textView5 != null) {
                                                                                                                                i = R.id.tv_3a_game_stream_card_title;
                                                                                                                                TextView textView6 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_3a_game_stream_card_title);
                                                                                                                                if (textView6 != null) {
                                                                                                                                    i = R.id.tv_large_game_tips_1;
                                                                                                                                    TextView textView7 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_large_game_tips_1);
                                                                                                                                    if (textView7 != null) {
                                                                                                                                        i = R.id.tv_large_game_tips_2;
                                                                                                                                        TextView textView8 = (TextView) ViewBindings.findChildViewById(view, R.id.tv_large_game_tips_2);
                                                                                                                                        if (textView8 != null) {
                                                                                                                                            return new LargeGameLayoutBinding(constraintLayout, imageView, space, space2, constraintLayout, constraintLayout2, textView, imageView2, textView2, imageView3, imageView4, imageView5, imageView6, imageView7, imageView8, marqueeTextView, linearLayout, nestedScrollView, imageView9, nestedScrollView2, selectedButton, selectedButton2, selectedButton3, selectedButton4, selectedButton5, selectedButton6, imageView10, largeGameTabView, textView3, findChildViewById, findChildViewById2, constraintLayout3, textView4, textView5, textView6, textView7, textView8);
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
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

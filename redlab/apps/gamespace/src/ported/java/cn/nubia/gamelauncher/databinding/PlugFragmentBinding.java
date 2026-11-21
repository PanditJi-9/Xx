package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.plug.view.PlugMarqueeTextView;
import cn.nubia.plug.view.plugRatingBarView.PlugScoreRatingBar;

/* loaded from: classes.dex */
public final class PlugFragmentBinding implements ViewBinding {
    public final TextView adapterDeviceTitle;
    public final LinearLayout adapterDevices;
    public final TextView content;
    public final LinearLayout gamesPlugScore;
    public final LinearLayout gamesPlugScoreLine1;
    public final LinearLayout gamesPlugScoreLine2;
    public final PlugScoreRatingBar plugRatingAdvanced;
    public final PlugScoreRatingBar plugRatingDifficulty;
    public final PlugScoreRatingBar plugRatingOperate;
    public final PlugScoreRatingBar plugRatingStrategy;
    public final PlugMarqueeTextView plugRatingStrategyTitle;
    private final ScrollView rootView;
    public final LinearLayout supportGames;
    public final TextView textSupportGameTitle;
    public final TextView title;

    private PlugFragmentBinding(ScrollView scrollView, TextView textView, LinearLayout linearLayout, TextView textView2, LinearLayout linearLayout2, LinearLayout linearLayout3, LinearLayout linearLayout4, PlugScoreRatingBar plugScoreRatingBar, PlugScoreRatingBar plugScoreRatingBar2, PlugScoreRatingBar plugScoreRatingBar3, PlugScoreRatingBar plugScoreRatingBar4, PlugMarqueeTextView plugMarqueeTextView, LinearLayout linearLayout5, TextView textView3, TextView textView4) {
        this.rootView = scrollView;
        this.adapterDeviceTitle = textView;
        this.adapterDevices = linearLayout;
        this.content = textView2;
        this.gamesPlugScore = linearLayout2;
        this.gamesPlugScoreLine1 = linearLayout3;
        this.gamesPlugScoreLine2 = linearLayout4;
        this.plugRatingAdvanced = plugScoreRatingBar;
        this.plugRatingDifficulty = plugScoreRatingBar2;
        this.plugRatingOperate = plugScoreRatingBar3;
        this.plugRatingStrategy = plugScoreRatingBar4;
        this.plugRatingStrategyTitle = plugMarqueeTextView;
        this.supportGames = linearLayout5;
        this.textSupportGameTitle = textView3;
        this.title = textView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ScrollView getRoot() {
        return this.rootView;
    }

    public static PlugFragmentBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static PlugFragmentBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.plug_fragment, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static PlugFragmentBinding bind(View view) {
        int i = R.id.adapter_device_title;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.adapter_device_title);
        if (textView != null) {
            i = R.id.adapter_devices;
            LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.adapter_devices);
            if (linearLayout != null) {
                i = R.id.content;
                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.content);
                if (textView2 != null) {
                    i = R.id.games_plug_score;
                    LinearLayout linearLayout2 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.games_plug_score);
                    if (linearLayout2 != null) {
                        i = R.id.games_plug_score_line1;
                        LinearLayout linearLayout3 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.games_plug_score_line1);
                        if (linearLayout3 != null) {
                            i = R.id.games_plug_score_line2;
                            LinearLayout linearLayout4 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.games_plug_score_line2);
                            if (linearLayout4 != null) {
                                i = R.id.plug_rating_advanced;
                                PlugScoreRatingBar plugScoreRatingBar = (PlugScoreRatingBar) ViewBindings.findChildViewById(view, R.id.plug_rating_advanced);
                                if (plugScoreRatingBar != null) {
                                    i = R.id.plug_rating_difficulty;
                                    PlugScoreRatingBar plugScoreRatingBar2 = (PlugScoreRatingBar) ViewBindings.findChildViewById(view, R.id.plug_rating_difficulty);
                                    if (plugScoreRatingBar2 != null) {
                                        i = R.id.plug_rating_operate;
                                        PlugScoreRatingBar plugScoreRatingBar3 = (PlugScoreRatingBar) ViewBindings.findChildViewById(view, R.id.plug_rating_operate);
                                        if (plugScoreRatingBar3 != null) {
                                            i = R.id.plug_rating_strategy;
                                            PlugScoreRatingBar plugScoreRatingBar4 = (PlugScoreRatingBar) ViewBindings.findChildViewById(view, R.id.plug_rating_strategy);
                                            if (plugScoreRatingBar4 != null) {
                                                i = R.id.plug_rating_strategy_title;
                                                PlugMarqueeTextView plugMarqueeTextView = (PlugMarqueeTextView) ViewBindings.findChildViewById(view, R.id.plug_rating_strategy_title);
                                                if (plugMarqueeTextView != null) {
                                                    i = R.id.support_games;
                                                    LinearLayout linearLayout5 = (LinearLayout) ViewBindings.findChildViewById(view, R.id.support_games);
                                                    if (linearLayout5 != null) {
                                                        i = R.id.text_support_game_title;
                                                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.text_support_game_title);
                                                        if (textView3 != null) {
                                                            i = R.id.title;
                                                            TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.title);
                                                            if (textView4 != null) {
                                                                return new PlugFragmentBinding((ScrollView) view, textView, linearLayout, textView2, linearLayout2, linearLayout3, linearLayout4, plugScoreRatingBar, plugScoreRatingBar2, plugScoreRatingBar3, plugScoreRatingBar4, plugMarqueeTextView, linearLayout5, textView3, textView4);
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

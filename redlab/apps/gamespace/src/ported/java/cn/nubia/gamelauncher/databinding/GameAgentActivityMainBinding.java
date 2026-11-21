package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Placeholder;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import androidx.viewpager.widget.ViewPager;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class GameAgentActivityMainBinding implements ViewBinding {
    public final ImageView back;
    public final ConstraintLayout behaviorLearnedItem;
    public final LinearLayout gameAgentIndicator;
    public final ViewPager gameAgentViewpager;
    public final View indicator0;
    public final View indicator1;
    public final ConstraintLayout lowSugarGameItem;
    public final Placeholder placeHolder;
    private final ConstraintLayout rootView;
    public final ImageView titleBackground;
    public final TextView tvDescription;
    public final TextView voiceInteractionIntroduce;
    public final ConstraintLayout voiceInteractionItem;
    public final TextView voiceInteractionName;
    public final BehaviorLearnedWidgetSwitchBinding voiceInteractionSwitch;

    private GameAgentActivityMainBinding(ConstraintLayout constraintLayout, ImageView imageView, ConstraintLayout constraintLayout2, LinearLayout linearLayout, ViewPager viewPager, View view, View view2, ConstraintLayout constraintLayout3, Placeholder placeholder, ImageView imageView2, TextView textView, TextView textView2, ConstraintLayout constraintLayout4, TextView textView3, BehaviorLearnedWidgetSwitchBinding behaviorLearnedWidgetSwitchBinding) {
        this.rootView = constraintLayout;
        this.back = imageView;
        this.behaviorLearnedItem = constraintLayout2;
        this.gameAgentIndicator = linearLayout;
        this.gameAgentViewpager = viewPager;
        this.indicator0 = view;
        this.indicator1 = view2;
        this.lowSugarGameItem = constraintLayout3;
        this.placeHolder = placeholder;
        this.titleBackground = imageView2;
        this.tvDescription = textView;
        this.voiceInteractionIntroduce = textView2;
        this.voiceInteractionItem = constraintLayout4;
        this.voiceInteractionName = textView3;
        this.voiceInteractionSwitch = behaviorLearnedWidgetSwitchBinding;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static GameAgentActivityMainBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GameAgentActivityMainBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.game_agent_activity_main, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GameAgentActivityMainBinding bind(View view) {
        int i = R.id.back;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.back);
        if (imageView != null) {
            i = R.id.behavior_learned_item;
            ConstraintLayout constraintLayout = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.behavior_learned_item);
            if (constraintLayout != null) {
                i = R.id.game_agent_indicator;
                LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.game_agent_indicator);
                if (linearLayout != null) {
                    i = R.id.game_agent_viewpager;
                    ViewPager viewPager = (ViewPager) ViewBindings.findChildViewById(view, R.id.game_agent_viewpager);
                    if (viewPager != null) {
                        i = R.id.indicator_0;
                        View findChildViewById = ViewBindings.findChildViewById(view, R.id.indicator_0);
                        if (findChildViewById != null) {
                            i = R.id.indicator_1;
                            View findChildViewById2 = ViewBindings.findChildViewById(view, R.id.indicator_1);
                            if (findChildViewById2 != null) {
                                i = R.id.low_sugar_game_item;
                                ConstraintLayout constraintLayout2 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.low_sugar_game_item);
                                if (constraintLayout2 != null) {
                                    i = R.id.place_holder;
                                    Placeholder placeholder = (Placeholder) ViewBindings.findChildViewById(view, R.id.place_holder);
                                    if (placeholder != null) {
                                        i = R.id.title_background;
                                        ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.title_background);
                                        if (imageView2 != null) {
                                            i = R.id.tv_description;
                                            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tv_description);
                                            if (textView != null) {
                                                i = R.id.voice_interaction_introduce;
                                                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.voice_interaction_introduce);
                                                if (textView2 != null) {
                                                    i = R.id.voice_interaction_item;
                                                    ConstraintLayout constraintLayout3 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.voice_interaction_item);
                                                    if (constraintLayout3 != null) {
                                                        i = R.id.voice_interaction_name;
                                                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.voice_interaction_name);
                                                        if (textView3 != null) {
                                                            i = R.id.voice_interaction_switch;
                                                            View findChildViewById3 = ViewBindings.findChildViewById(view, R.id.voice_interaction_switch);
                                                            if (findChildViewById3 != null) {
                                                                return new GameAgentActivityMainBinding((ConstraintLayout) view, imageView, constraintLayout, linearLayout, viewPager, findChildViewById, findChildViewById2, constraintLayout2, placeholder, imageView2, textView, textView2, constraintLayout3, textView3, BehaviorLearnedWidgetSwitchBinding.bind(findChildViewById3));
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

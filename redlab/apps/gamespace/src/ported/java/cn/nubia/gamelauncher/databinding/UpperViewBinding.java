package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.Group;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import cn.nubia.common.view.BatteryView;
import cn.nubia.common.view.NubiaFontTextClockView;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.ZoomButton;

/* loaded from: classes.dex */
public final class UpperViewBinding implements ViewBinding {
    public final ZoomButton addShortcut;
    public final TextView addShortcutTips;
    public final ZoomButton exitButton80;
    public final ZoomButton externDevice;
    public final ZoomButton gameRecommend;
    public final Group groupPop;
    public final BatteryView levelRemain;
    public final ImageView magicLogo80;
    public final ZoomButton personalCenter;
    public final ZoomButton playCenter;
    public final ImageView playCenterPoint;
    public final FrameLayout popStateContent;
    public final ZoomButton positionView;
    public final ImageView redDot;
    public final ImageView redPoint;
    private final View rootView;
    public final NubiaFontTextClockView timeDisplay;
    public final ZoomButton topDetector;
    public final ZoomButton topMora;
    public final ZoomButton topNews;
    public final ImageView topPopContent;
    public final ImageView topPopDirectionDetector;
    public final ImageView topPopDirectionNews;
    public final ImageView topPopDirectionScore;
    public final TextView topPopTitle;
    public final ImageView topPopTitleBg;
    public final ZoomButton topRelevant;
    public final ZoomButton topScore;

    private UpperViewBinding(View view, ZoomButton zoomButton, TextView textView, ZoomButton zoomButton2, ZoomButton zoomButton3, ZoomButton zoomButton4, Group group, BatteryView batteryView, ImageView imageView, ZoomButton zoomButton5, ZoomButton zoomButton6, ImageView imageView2, FrameLayout frameLayout, ZoomButton zoomButton7, ImageView imageView3, ImageView imageView4, NubiaFontTextClockView nubiaFontTextClockView, ZoomButton zoomButton8, ZoomButton zoomButton9, ZoomButton zoomButton10, ImageView imageView5, ImageView imageView6, ImageView imageView7, ImageView imageView8, TextView textView2, ImageView imageView9, ZoomButton zoomButton11, ZoomButton zoomButton12) {
        this.rootView = view;
        this.addShortcut = zoomButton;
        this.addShortcutTips = textView;
        this.exitButton80 = zoomButton2;
        this.externDevice = zoomButton3;
        this.gameRecommend = zoomButton4;
        this.groupPop = group;
        this.levelRemain = batteryView;
        this.magicLogo80 = imageView;
        this.personalCenter = zoomButton5;
        this.playCenter = zoomButton6;
        this.playCenterPoint = imageView2;
        this.popStateContent = frameLayout;
        this.positionView = zoomButton7;
        this.redDot = imageView3;
        this.redPoint = imageView4;
        this.timeDisplay = nubiaFontTextClockView;
        this.topDetector = zoomButton8;
        this.topMora = zoomButton9;
        this.topNews = zoomButton10;
        this.topPopContent = imageView5;
        this.topPopDirectionDetector = imageView6;
        this.topPopDirectionNews = imageView7;
        this.topPopDirectionScore = imageView8;
        this.topPopTitle = textView2;
        this.topPopTitleBg = imageView9;
        this.topRelevant = zoomButton11;
        this.topScore = zoomButton12;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static UpperViewBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.upper_view, viewGroup);
        return bind(viewGroup);
    }

    public static UpperViewBinding bind(View view) {
        int i = R.id.add_shortcut;
        ZoomButton zoomButton = (ZoomButton) ViewBindings.findChildViewById(view, R.id.add_shortcut);
        if (zoomButton != null) {
            i = R.id.add_shortcut_tips;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.add_shortcut_tips);
            if (textView != null) {
                i = R.id.exit_button80;
                ZoomButton zoomButton2 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.exit_button80);
                if (zoomButton2 != null) {
                    i = R.id.extern_device;
                    ZoomButton zoomButton3 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.extern_device);
                    if (zoomButton3 != null) {
                        i = R.id.game_recommend;
                        ZoomButton zoomButton4 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.game_recommend);
                        if (zoomButton4 != null) {
                            i = R.id.group_pop;
                            Group group = (Group) ViewBindings.findChildViewById(view, R.id.group_pop);
                            if (group != null) {
                                i = R.id.level_remain;
                                BatteryView batteryView = (BatteryView) ViewBindings.findChildViewById(view, R.id.level_remain);
                                if (batteryView != null) {
                                    i = R.id.magic_logo80;
                                    ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.magic_logo80);
                                    if (imageView != null) {
                                        i = R.id.personal_center;
                                        ZoomButton zoomButton5 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.personal_center);
                                        if (zoomButton5 != null) {
                                            i = R.id.play_center;
                                            ZoomButton zoomButton6 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.play_center);
                                            if (zoomButton6 != null) {
                                                i = R.id.play_center_point;
                                                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.play_center_point);
                                                if (imageView2 != null) {
                                                    i = R.id.pop_state_content;
                                                    FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, R.id.pop_state_content);
                                                    if (frameLayout != null) {
                                                        i = R.id.position_view;
                                                        ZoomButton zoomButton7 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.position_view);
                                                        if (zoomButton7 != null) {
                                                            i = R.id.red_dot;
                                                            ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.red_dot);
                                                            if (imageView3 != null) {
                                                                i = R.id.red_point;
                                                                ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.red_point);
                                                                if (imageView4 != null) {
                                                                    i = R.id.time_display;
                                                                    NubiaFontTextClockView nubiaFontTextClockView = (NubiaFontTextClockView) ViewBindings.findChildViewById(view, R.id.time_display);
                                                                    if (nubiaFontTextClockView != null) {
                                                                        i = R.id.top_detector;
                                                                        ZoomButton zoomButton8 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.top_detector);
                                                                        if (zoomButton8 != null) {
                                                                            i = R.id.top_mora;
                                                                            ZoomButton zoomButton9 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.top_mora);
                                                                            if (zoomButton9 != null) {
                                                                                i = R.id.top_news;
                                                                                ZoomButton zoomButton10 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.top_news);
                                                                                if (zoomButton10 != null) {
                                                                                    i = R.id.top_pop_content;
                                                                                    ImageView imageView5 = (ImageView) ViewBindings.findChildViewById(view, R.id.top_pop_content);
                                                                                    if (imageView5 != null) {
                                                                                        i = R.id.top_pop_direction_detector;
                                                                                        ImageView imageView6 = (ImageView) ViewBindings.findChildViewById(view, R.id.top_pop_direction_detector);
                                                                                        if (imageView6 != null) {
                                                                                            i = R.id.top_pop_direction_news;
                                                                                            ImageView imageView7 = (ImageView) ViewBindings.findChildViewById(view, R.id.top_pop_direction_news);
                                                                                            if (imageView7 != null) {
                                                                                                i = R.id.top_pop_direction_score;
                                                                                                ImageView imageView8 = (ImageView) ViewBindings.findChildViewById(view, R.id.top_pop_direction_score);
                                                                                                if (imageView8 != null) {
                                                                                                    i = R.id.top_pop_title;
                                                                                                    TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.top_pop_title);
                                                                                                    if (textView2 != null) {
                                                                                                        i = R.id.top_pop_title_bg;
                                                                                                        ImageView imageView9 = (ImageView) ViewBindings.findChildViewById(view, R.id.top_pop_title_bg);
                                                                                                        if (imageView9 != null) {
                                                                                                            i = R.id.top_relevant;
                                                                                                            ZoomButton zoomButton11 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.top_relevant);
                                                                                                            if (zoomButton11 != null) {
                                                                                                                i = R.id.top_score;
                                                                                                                ZoomButton zoomButton12 = (ZoomButton) ViewBindings.findChildViewById(view, R.id.top_score);
                                                                                                                if (zoomButton12 != null) {
                                                                                                                    return new UpperViewBinding(view, zoomButton, textView, zoomButton2, zoomButton3, zoomButton4, group, batteryView, imageView, zoomButton5, zoomButton6, imageView2, frameLayout, zoomButton7, imageView3, imageView4, nubiaFontTextClockView, zoomButton8, zoomButton9, zoomButton10, imageView5, imageView6, imageView7, imageView8, textView2, imageView9, zoomButton11, zoomButton12);
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

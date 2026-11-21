package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.TgkCustomRadioButton;

/* loaded from: classes.dex */
public final class TgkMenuPopWindowLayoutBinding implements ViewBinding {
    private final ScrollView rootView;
    public final TgkCustomRadioButton tgkCopy;
    public final TgkCustomRadioButton tgkLink;
    public final TgkCustomRadioButton tgkLongPress;
    public final TgkCustomRadioButton tgkMoveVision;
    public final TgkCustomRadioButton tgkMultClicks;
    public final TgkCustomRadioButton tgkOff;
    public final TgkCustomRadioButton tgkSingle;
    public final TgkCustomRadioButton tgkSlideCopy;
    public final TgkCustomRadioButton tgkSlideSingle;
    public final TgkCustomRadioButton tgkUpDown;

    private TgkMenuPopWindowLayoutBinding(ScrollView scrollView, TgkCustomRadioButton tgkCustomRadioButton, TgkCustomRadioButton tgkCustomRadioButton2, TgkCustomRadioButton tgkCustomRadioButton3, TgkCustomRadioButton tgkCustomRadioButton4, TgkCustomRadioButton tgkCustomRadioButton5, TgkCustomRadioButton tgkCustomRadioButton6, TgkCustomRadioButton tgkCustomRadioButton7, TgkCustomRadioButton tgkCustomRadioButton8, TgkCustomRadioButton tgkCustomRadioButton9, TgkCustomRadioButton tgkCustomRadioButton10) {
        this.rootView = scrollView;
        this.tgkCopy = tgkCustomRadioButton;
        this.tgkLink = tgkCustomRadioButton2;
        this.tgkLongPress = tgkCustomRadioButton3;
        this.tgkMoveVision = tgkCustomRadioButton4;
        this.tgkMultClicks = tgkCustomRadioButton5;
        this.tgkOff = tgkCustomRadioButton6;
        this.tgkSingle = tgkCustomRadioButton7;
        this.tgkSlideCopy = tgkCustomRadioButton8;
        this.tgkSlideSingle = tgkCustomRadioButton9;
        this.tgkUpDown = tgkCustomRadioButton10;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ScrollView getRoot() {
        return this.rootView;
    }

    public static TgkMenuPopWindowLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkMenuPopWindowLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_menu_pop_window_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkMenuPopWindowLayoutBinding bind(View view) {
        int i = R.id.tgk_copy;
        TgkCustomRadioButton tgkCustomRadioButton = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_copy);
        if (tgkCustomRadioButton != null) {
            i = R.id.tgk_link;
            TgkCustomRadioButton tgkCustomRadioButton2 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_link);
            if (tgkCustomRadioButton2 != null) {
                i = R.id.tgk_long_press;
                TgkCustomRadioButton tgkCustomRadioButton3 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_long_press);
                if (tgkCustomRadioButton3 != null) {
                    i = R.id.tgk_move_vision;
                    TgkCustomRadioButton tgkCustomRadioButton4 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_move_vision);
                    if (tgkCustomRadioButton4 != null) {
                        i = R.id.tgk_mult_clicks;
                        TgkCustomRadioButton tgkCustomRadioButton5 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_mult_clicks);
                        if (tgkCustomRadioButton5 != null) {
                            i = R.id.tgk_off;
                            TgkCustomRadioButton tgkCustomRadioButton6 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_off);
                            if (tgkCustomRadioButton6 != null) {
                                i = R.id.tgk_single;
                                TgkCustomRadioButton tgkCustomRadioButton7 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_single);
                                if (tgkCustomRadioButton7 != null) {
                                    i = R.id.tgk_slide_copy;
                                    TgkCustomRadioButton tgkCustomRadioButton8 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_slide_copy);
                                    if (tgkCustomRadioButton8 != null) {
                                        i = R.id.tgk_slide_single;
                                        TgkCustomRadioButton tgkCustomRadioButton9 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_slide_single);
                                        if (tgkCustomRadioButton9 != null) {
                                            i = R.id.tgk_up_down;
                                            TgkCustomRadioButton tgkCustomRadioButton10 = (TgkCustomRadioButton) ViewBindings.findChildViewById(view, R.id.tgk_up_down);
                                            if (tgkCustomRadioButton10 != null) {
                                                return new TgkMenuPopWindowLayoutBinding((ScrollView) view, tgkCustomRadioButton, tgkCustomRadioButton2, tgkCustomRadioButton3, tgkCustomRadioButton4, tgkCustomRadioButton5, tgkCustomRadioButton6, tgkCustomRadioButton7, tgkCustomRadioButton8, tgkCustomRadioButton9, tgkCustomRadioButton10);
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

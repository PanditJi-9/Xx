package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.aimhelper.SelectableImageView;

/* loaded from: classes.dex */
public final class AimSettingLayoutBinding implements ViewBinding {
    public final SelectableImageView blue;
    public final ImageButton btnClose;
    public final ImageButton btnQuickHideTip;
    public final SelectableImageView green;
    public final SeekBar opacityseekbar;
    public final ImageView quickhideCheckbox;
    public final SelectableImageView red;
    private final RelativeLayout rootView;
    public final SeekBar seekbar;
    public final SelectableImageView style1;
    public final SelectableImageView style2;
    public final SelectableImageView style3;
    public final SelectableImageView style4;
    public final SelectableImageView style5;
    public final SelectableImageView white;
    public final SelectableImageView yellow;

    private AimSettingLayoutBinding(RelativeLayout relativeLayout, SelectableImageView selectableImageView, ImageButton imageButton, ImageButton imageButton2, SelectableImageView selectableImageView2, SeekBar seekBar, ImageView imageView, SelectableImageView selectableImageView3, SeekBar seekBar2, SelectableImageView selectableImageView4, SelectableImageView selectableImageView5, SelectableImageView selectableImageView6, SelectableImageView selectableImageView7, SelectableImageView selectableImageView8, SelectableImageView selectableImageView9, SelectableImageView selectableImageView10) {
        this.rootView = relativeLayout;
        this.blue = selectableImageView;
        this.btnClose = imageButton;
        this.btnQuickHideTip = imageButton2;
        this.green = selectableImageView2;
        this.opacityseekbar = seekBar;
        this.quickhideCheckbox = imageView;
        this.red = selectableImageView3;
        this.seekbar = seekBar2;
        this.style1 = selectableImageView4;
        this.style2 = selectableImageView5;
        this.style3 = selectableImageView6;
        this.style4 = selectableImageView7;
        this.style5 = selectableImageView8;
        this.white = selectableImageView9;
        this.yellow = selectableImageView10;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static AimSettingLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static AimSettingLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.aim_setting_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static AimSettingLayoutBinding bind(View view) {
        int i = R.id.blue;
        SelectableImageView selectableImageView = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.blue);
        if (selectableImageView != null) {
            i = R.id.btn_close;
            ImageButton imageButton = (ImageButton) ViewBindings.findChildViewById(view, R.id.btn_close);
            if (imageButton != null) {
                i = R.id.btn_quick_hide_tip;
                ImageButton imageButton2 = (ImageButton) ViewBindings.findChildViewById(view, R.id.btn_quick_hide_tip);
                if (imageButton2 != null) {
                    i = R.id.green;
                    SelectableImageView selectableImageView2 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.green);
                    if (selectableImageView2 != null) {
                        i = R.id.opacityseekbar;
                        SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.opacityseekbar);
                        if (seekBar != null) {
                            i = R.id.quickhide_checkbox;
                            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.quickhide_checkbox);
                            if (imageView != null) {
                                i = R.id.red;
                                SelectableImageView selectableImageView3 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.red);
                                if (selectableImageView3 != null) {
                                    i = R.id.seekbar;
                                    SeekBar seekBar2 = (SeekBar) ViewBindings.findChildViewById(view, R.id.seekbar);
                                    if (seekBar2 != null) {
                                        i = R.id.style1;
                                        SelectableImageView selectableImageView4 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.style1);
                                        if (selectableImageView4 != null) {
                                            i = R.id.style2;
                                            SelectableImageView selectableImageView5 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.style2);
                                            if (selectableImageView5 != null) {
                                                i = R.id.style3;
                                                SelectableImageView selectableImageView6 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.style3);
                                                if (selectableImageView6 != null) {
                                                    i = R.id.style4;
                                                    SelectableImageView selectableImageView7 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.style4);
                                                    if (selectableImageView7 != null) {
                                                        i = R.id.style5;
                                                        SelectableImageView selectableImageView8 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.style5);
                                                        if (selectableImageView8 != null) {
                                                            i = R.id.white;
                                                            SelectableImageView selectableImageView9 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.white);
                                                            if (selectableImageView9 != null) {
                                                                i = R.id.yellow;
                                                                SelectableImageView selectableImageView10 = (SelectableImageView) ViewBindings.findChildViewById(view, R.id.yellow);
                                                                if (selectableImageView10 != null) {
                                                                    return new AimSettingLayoutBinding((RelativeLayout) view, selectableImageView, imageButton, imageButton2, selectableImageView2, seekBar, imageView, selectableImageView3, seekBar2, selectableImageView4, selectableImageView5, selectableImageView6, selectableImageView7, selectableImageView8, selectableImageView9, selectableImageView10);
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

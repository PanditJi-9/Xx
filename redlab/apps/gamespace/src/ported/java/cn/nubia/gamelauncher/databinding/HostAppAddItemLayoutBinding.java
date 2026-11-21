package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class HostAppAddItemLayoutBinding implements ViewBinding {
    public final ImageView appIcon;
    public final ImageView appRadioBtn;
    public final ImageView appSwitch;
    public final RelativeLayout backColor;
    public final View bottomLine;
    public final TextView gameSpaceAppName;
    public final TextView joinSwitch;
    public final TextView removeSwitch;
    private final LinearLayout rootView;
    public final TextView titleName;

    private HostAppAddItemLayoutBinding(LinearLayout linearLayout, ImageView imageView, ImageView imageView2, ImageView imageView3, RelativeLayout relativeLayout, View view, TextView textView, TextView textView2, TextView textView3, TextView textView4) {
        this.rootView = linearLayout;
        this.appIcon = imageView;
        this.appRadioBtn = imageView2;
        this.appSwitch = imageView3;
        this.backColor = relativeLayout;
        this.bottomLine = view;
        this.gameSpaceAppName = textView;
        this.joinSwitch = textView2;
        this.removeSwitch = textView3;
        this.titleName = textView4;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static HostAppAddItemLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HostAppAddItemLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.host_app_add_item_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HostAppAddItemLayoutBinding bind(View view) {
        int i = R.id.app_icon;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.app_icon);
        if (imageView != null) {
            i = R.id.app_radio_btn;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.app_radio_btn);
            if (imageView2 != null) {
                i = R.id.app_switch;
                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.app_switch);
                if (imageView3 != null) {
                    i = R.id.back_color;
                    RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.back_color);
                    if (relativeLayout != null) {
                        i = R.id.bottom_line;
                        View findChildViewById = ViewBindings.findChildViewById(view, R.id.bottom_line);
                        if (findChildViewById != null) {
                            i = R.id.game_space_app_name;
                            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.game_space_app_name);
                            if (textView != null) {
                                i = R.id.join_switch;
                                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.join_switch);
                                if (textView2 != null) {
                                    i = R.id.remove_switch;
                                    TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.remove_switch);
                                    if (textView3 != null) {
                                        i = R.id.title_name;
                                        TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.title_name);
                                        if (textView4 != null) {
                                            return new HostAppAddItemLayoutBinding((LinearLayout) view, imageView, imageView2, imageView3, relativeLayout, findChildViewById, textView, textView2, textView3, textView4);
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

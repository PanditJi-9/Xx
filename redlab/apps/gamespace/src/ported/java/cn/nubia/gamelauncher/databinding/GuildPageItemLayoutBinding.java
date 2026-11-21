package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class GuildPageItemLayoutBinding implements ViewBinding {
    public final TextView guideExplain;
    public final ImageView guideLogo;
    public final ImageView guideSlidLeft;
    public final ImageView guideSlidMid;
    public final ImageView guideSlidRight;
    public final Button guideStart;
    public final TextView guideTitle;
    private final ConstraintLayout rootView;

    private GuildPageItemLayoutBinding(ConstraintLayout constraintLayout, TextView textView, ImageView imageView, ImageView imageView2, ImageView imageView3, ImageView imageView4, Button button, TextView textView2) {
        this.rootView = constraintLayout;
        this.guideExplain = textView;
        this.guideLogo = imageView;
        this.guideSlidLeft = imageView2;
        this.guideSlidMid = imageView3;
        this.guideSlidRight = imageView4;
        this.guideStart = button;
        this.guideTitle = textView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static GuildPageItemLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GuildPageItemLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.guild_page_item_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GuildPageItemLayoutBinding bind(View view) {
        int i = R.id.guide_explain;
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.guide_explain);
        if (textView != null) {
            i = R.id.guide_logo;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.guide_logo);
            if (imageView != null) {
                i = R.id.guide_slid_left;
                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.guide_slid_left);
                if (imageView2 != null) {
                    i = R.id.guide_slid_mid;
                    ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.guide_slid_mid);
                    if (imageView3 != null) {
                        i = R.id.guide_slid_right;
                        ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.guide_slid_right);
                        if (imageView4 != null) {
                            i = R.id.guide_start;
                            Button button = (Button) ViewBindings.findChildViewById(view, R.id.guide_start);
                            if (button != null) {
                                i = R.id.guide_title;
                                TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.guide_title);
                                if (textView2 != null) {
                                    return new GuildPageItemLayoutBinding((ConstraintLayout) view, textView, imageView, imageView2, imageView3, imageView4, button, textView2);
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

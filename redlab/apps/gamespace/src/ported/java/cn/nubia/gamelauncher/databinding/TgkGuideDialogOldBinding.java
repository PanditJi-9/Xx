package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import androidx.viewpager.widget.ViewPager;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class TgkGuideDialogOldBinding implements ViewBinding {
    public final LinearLayout pageIndicator;
    public final ImageView pageIndicator0;
    public final ImageView pageIndicator1;
    public final ViewPager pager;
    private final LinearLayout rootView;

    private TgkGuideDialogOldBinding(LinearLayout linearLayout, LinearLayout linearLayout2, ImageView imageView, ImageView imageView2, ViewPager viewPager) {
        this.rootView = linearLayout;
        this.pageIndicator = linearLayout2;
        this.pageIndicator0 = imageView;
        this.pageIndicator1 = imageView2;
        this.pager = viewPager;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static TgkGuideDialogOldBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkGuideDialogOldBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_guide_dialog_old, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkGuideDialogOldBinding bind(View view) {
        int i = R.id.page_indicator;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.page_indicator);
        if (linearLayout != null) {
            i = R.id.page_indicator0;
            ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.page_indicator0);
            if (imageView != null) {
                i = R.id.page_indicator1;
                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.page_indicator1);
                if (imageView2 != null) {
                    i = R.id.pager;
                    ViewPager viewPager = (ViewPager) ViewBindings.findChildViewById(view, R.id.pager);
                    if (viewPager != null) {
                        return new TgkGuideDialogOldBinding((LinearLayout) view, linearLayout, imageView, imageView2, viewPager);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import cn.nubia.tgk.widget.MarqueeTextView;

/* loaded from: classes.dex */
public final class TgkCaseShowLayoutBinding implements ViewBinding {
    private final LinearLayout rootView;
    public final LinearLayout tgkCaseButton;
    public final LinearLayout tgkCaseListView;
    public final MarqueeTextView tgkCaseTitle;
    public final View tgkCaseView;

    private TgkCaseShowLayoutBinding(LinearLayout linearLayout, LinearLayout linearLayout2, LinearLayout linearLayout3, MarqueeTextView marqueeTextView, View view) {
        this.rootView = linearLayout;
        this.tgkCaseButton = linearLayout2;
        this.tgkCaseListView = linearLayout3;
        this.tgkCaseTitle = marqueeTextView;
        this.tgkCaseView = view;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static TgkCaseShowLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkCaseShowLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_case_show_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkCaseShowLayoutBinding bind(View view) {
        int i = R.id.tgk_case_button;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.tgk_case_button);
        if (linearLayout != null) {
            LinearLayout linearLayout2 = (LinearLayout) view;
            i = R.id.tgk_case_title;
            MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.tgk_case_title);
            if (marqueeTextView != null) {
                i = R.id.tgk_case_view;
                View findChildViewById = ViewBindings.findChildViewById(view, R.id.tgk_case_view);
                if (findChildViewById != null) {
                    return new TgkCaseShowLayoutBinding(linearLayout2, linearLayout, linearLayout2, marqueeTextView, findChildViewById);
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

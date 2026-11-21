package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.MarqueeTextView;

/* loaded from: classes.dex */
public final class OneMoreThingLayoutPureBinding implements ViewBinding {
    public final MarqueeTextView omtText;
    public final TextView omtZan;
    private final View rootView;

    private OneMoreThingLayoutPureBinding(View view, MarqueeTextView marqueeTextView, TextView textView) {
        this.rootView = view;
        this.omtText = marqueeTextView;
        this.omtZan = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public View getRoot() {
        return this.rootView;
    }

    public static OneMoreThingLayoutPureBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        if (viewGroup == null) {
            throw new NullPointerException("parent");
        }
        layoutInflater.inflate(R.layout.one_more_thing_layout_pure, viewGroup);
        return bind(viewGroup);
    }

    public static OneMoreThingLayoutPureBinding bind(View view) {
        int i = R.id.omt_text;
        MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.omt_text);
        if (marqueeTextView != null) {
            i = R.id.omt_zan;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.omt_zan);
            if (textView != null) {
                return new OneMoreThingLayoutPureBinding(view, marqueeTextView, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

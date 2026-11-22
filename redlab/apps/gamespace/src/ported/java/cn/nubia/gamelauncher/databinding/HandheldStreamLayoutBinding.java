package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.HandheldItemLayout;

/* loaded from: classes.dex */
public final class HandheldStreamLayoutBinding implements ViewBinding {
    public final HandheldItemLayout handheldBorder;
    public final TextView handheldStreamName;
    private final ConstraintLayout rootView;

    private HandheldStreamLayoutBinding(ConstraintLayout constraintLayout, HandheldItemLayout handheldItemLayout, TextView textView) {
        this.rootView = constraintLayout;
        this.handheldBorder = handheldItemLayout;
        this.handheldStreamName = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static HandheldStreamLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static HandheldStreamLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.handheld_stream_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static HandheldStreamLayoutBinding bind(View view) {
        int i = R.id.handheld_border;
        HandheldItemLayout handheldItemLayout = (HandheldItemLayout) ViewBindings.findChildViewById(view, R.id.handheld_border);
        if (handheldItemLayout != null) {
            i = R.id.handheld_stream_name;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.handheld_stream_name);
            if (textView != null) {
                return new HandheldStreamLayoutBinding((ConstraintLayout) view, handheldItemLayout, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class TgkFilePickActivityLayoutBinding implements ViewBinding {
    private final FrameLayout rootView;
    public final TextView tgkParsingView;

    private TgkFilePickActivityLayoutBinding(FrameLayout frameLayout, TextView textView) {
        this.rootView = frameLayout;
        this.tgkParsingView = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public FrameLayout getRoot() {
        return this.rootView;
    }

    public static TgkFilePickActivityLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static TgkFilePickActivityLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.tgk_file_pick_activity_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static TgkFilePickActivityLayoutBinding bind(View view) {
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.tgk_parsing_view);
        if (textView != null) {
            return new TgkFilePickActivityLayoutBinding((FrameLayout) view, textView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.tgk_parsing_view)));
    }
}

package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class FollowLayoutPortBinding implements ViewBinding {
    public final LinearLayout followLayout;
    private final LinearLayout rootView;
    public final SeekBar seekBarFollow;

    private FollowLayoutPortBinding(LinearLayout linearLayout, LinearLayout linearLayout2, SeekBar seekBar) {
        this.rootView = linearLayout;
        this.followLayout = linearLayout2;
        this.seekBarFollow = seekBar;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static FollowLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static FollowLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.follow_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static FollowLayoutPortBinding bind(View view) {
        LinearLayout linearLayout = (LinearLayout) view;
        SeekBar seekBar = (SeekBar) ViewBindings.findChildViewById(view, R.id.seek_bar_follow);
        if (seekBar != null) {
            return new FollowLayoutPortBinding(linearLayout, linearLayout, seekBar);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.seek_bar_follow)));
    }
}

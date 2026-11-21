package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.MarqueeTextView;

/* loaded from: classes.dex */
public final class GameStartButtonBinding implements ViewBinding {
    public final ImageView gameStart;
    public final ImageView gameStartLeft;
    public final ImageView gameStartRight;
    public final MarqueeTextView gameStartText;
    private final ConstraintLayout rootView;

    private GameStartButtonBinding(ConstraintLayout constraintLayout, ImageView imageView, ImageView imageView2, ImageView imageView3, MarqueeTextView marqueeTextView) {
        this.rootView = constraintLayout;
        this.gameStart = imageView;
        this.gameStartLeft = imageView2;
        this.gameStartRight = imageView3;
        this.gameStartText = marqueeTextView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static GameStartButtonBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GameStartButtonBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.game_start_button, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GameStartButtonBinding bind(View view) {
        int i = R.id.game_start;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.game_start);
        if (imageView != null) {
            i = R.id.game_start_left;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.game_start_left);
            if (imageView2 != null) {
                i = R.id.game_start_right;
                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.game_start_right);
                if (imageView3 != null) {
                    i = R.id.game_start_text;
                    MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.game_start_text);
                    if (marqueeTextView != null) {
                        return new GameStartButtonBinding((ConstraintLayout) view, imageView, imageView2, imageView3, marqueeTextView);
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

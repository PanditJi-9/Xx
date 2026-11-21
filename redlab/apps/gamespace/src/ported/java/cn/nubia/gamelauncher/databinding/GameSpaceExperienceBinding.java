package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.MarqueeTextView;

/* loaded from: classes.dex */
public final class GameSpaceExperienceBinding implements ViewBinding {
    public final ImageView experienceMirror;
    public final FrameLayout fragmentContainer;
    public final ImageView modeBack;
    public final ImageView modeBackground;
    public final MarqueeTextView modeIntroduction;
    public final MarqueeTextView modeIntroductionNote;
    private final ConstraintLayout rootView;

    private GameSpaceExperienceBinding(ConstraintLayout constraintLayout, ImageView imageView, FrameLayout frameLayout, ImageView imageView2, ImageView imageView3, MarqueeTextView marqueeTextView, MarqueeTextView marqueeTextView2) {
        this.rootView = constraintLayout;
        this.experienceMirror = imageView;
        this.fragmentContainer = frameLayout;
        this.modeBack = imageView2;
        this.modeBackground = imageView3;
        this.modeIntroduction = marqueeTextView;
        this.modeIntroductionNote = marqueeTextView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static GameSpaceExperienceBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GameSpaceExperienceBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.game_space_experience, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GameSpaceExperienceBinding bind(View view) {
        int i = R.id.experience_mirror;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.experience_mirror);
        if (imageView != null) {
            i = R.id.fragment_container;
            FrameLayout frameLayout = (FrameLayout) ViewBindings.findChildViewById(view, R.id.fragment_container);
            if (frameLayout != null) {
                i = R.id.mode_back;
                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.mode_back);
                if (imageView2 != null) {
                    i = R.id.mode_background;
                    ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.mode_background);
                    if (imageView3 != null) {
                        i = R.id.mode_introduction;
                        MarqueeTextView marqueeTextView = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.mode_introduction);
                        if (marqueeTextView != null) {
                            i = R.id.mode_introduction_note;
                            MarqueeTextView marqueeTextView2 = (MarqueeTextView) ViewBindings.findChildViewById(view, R.id.mode_introduction_note);
                            if (marqueeTextView2 != null) {
                                return new GameSpaceExperienceBinding((ConstraintLayout) view, imageView, frameLayout, imageView2, imageView3, marqueeTextView, marqueeTextView2);
                            }
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

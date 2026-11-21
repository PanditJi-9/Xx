package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.ZoomTextView;

/* loaded from: classes.dex */
public final class GameLobbyEntryPureBinding implements ViewBinding {
    public final ZoomTextView lobbyControl;
    public final ZoomTextView lobbyCustom;
    public final ImageView lobbyDivider;
    public final ZoomTextView lobbyGiftBag;
    public final ZoomTextView lobbyNotes;
    public final ZoomTextView lobbyRedTime;
    public final ImageView redMagicPoint;
    private final ConstraintLayout rootView;

    private GameLobbyEntryPureBinding(ConstraintLayout constraintLayout, ZoomTextView zoomTextView, ZoomTextView zoomTextView2, ImageView imageView, ZoomTextView zoomTextView3, ZoomTextView zoomTextView4, ZoomTextView zoomTextView5, ImageView imageView2) {
        this.rootView = constraintLayout;
        this.lobbyControl = zoomTextView;
        this.lobbyCustom = zoomTextView2;
        this.lobbyDivider = imageView;
        this.lobbyGiftBag = zoomTextView3;
        this.lobbyNotes = zoomTextView4;
        this.lobbyRedTime = zoomTextView5;
        this.redMagicPoint = imageView2;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static GameLobbyEntryPureBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GameLobbyEntryPureBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.game_lobby_entry_pure, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GameLobbyEntryPureBinding bind(View view) {
        int i = R.id.lobby_control;
        ZoomTextView zoomTextView = (ZoomTextView) ViewBindings.findChildViewById(view, R.id.lobby_control);
        if (zoomTextView != null) {
            i = R.id.lobby_custom;
            ZoomTextView zoomTextView2 = (ZoomTextView) ViewBindings.findChildViewById(view, R.id.lobby_custom);
            if (zoomTextView2 != null) {
                i = R.id.lobby_divider;
                ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.lobby_divider);
                if (imageView != null) {
                    i = R.id.lobby_gift_bag;
                    ZoomTextView zoomTextView3 = (ZoomTextView) ViewBindings.findChildViewById(view, R.id.lobby_gift_bag);
                    if (zoomTextView3 != null) {
                        i = R.id.lobby_notes;
                        ZoomTextView zoomTextView4 = (ZoomTextView) ViewBindings.findChildViewById(view, R.id.lobby_notes);
                        if (zoomTextView4 != null) {
                            i = R.id.lobby_red_time;
                            ZoomTextView zoomTextView5 = (ZoomTextView) ViewBindings.findChildViewById(view, R.id.lobby_red_time);
                            if (zoomTextView5 != null) {
                                i = R.id.red_magic_point;
                                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.red_magic_point);
                                if (imageView2 != null) {
                                    return new GameLobbyEntryPureBinding((ConstraintLayout) view, zoomTextView, zoomTextView2, imageView, zoomTextView3, zoomTextView4, zoomTextView5, imageView2);
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

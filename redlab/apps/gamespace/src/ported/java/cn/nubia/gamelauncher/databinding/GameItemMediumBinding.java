package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.view.DrawableLeftTextView;

/* loaded from: classes.dex */
public final class GameItemMediumBinding implements ViewBinding {
    public final ImageView cardMask;
    public final CardView cardView;
    public final TextView downloadStateText;
    public final ImageView gameBanner;
    public final DrawableLeftTextView gameName;
    public final ImageView icon;
    private final CardView rootView;

    private GameItemMediumBinding(CardView cardView, ImageView imageView, CardView cardView2, TextView textView, ImageView imageView2, DrawableLeftTextView drawableLeftTextView, ImageView imageView3) {
        this.rootView = cardView;
        this.cardMask = imageView;
        this.cardView = cardView2;
        this.downloadStateText = textView;
        this.gameBanner = imageView2;
        this.gameName = drawableLeftTextView;
        this.icon = imageView3;
    }

    @Override // androidx.viewbinding.ViewBinding
    public CardView getRoot() {
        return this.rootView;
    }

    public static GameItemMediumBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static GameItemMediumBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.game_item_medium, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static GameItemMediumBinding bind(View view) {
        int i = R.id.card_mask;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.card_mask);
        if (imageView != null) {
            CardView cardView = (CardView) view;
            i = R.id.download_state_text;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.download_state_text);
            if (textView != null) {
                i = R.id.game_banner;
                ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.game_banner);
                if (imageView2 != null) {
                    i = R.id.game_name;
                    DrawableLeftTextView drawableLeftTextView = (DrawableLeftTextView) ViewBindings.findChildViewById(view, R.id.game_name);
                    if (drawableLeftTextView != null) {
                        i = R.id.icon;
                        ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.icon);
                        if (imageView3 != null) {
                            return new GameItemMediumBinding(cardView, imageView, cardView, textView, imageView2, drawableLeftTextView, imageView3);
                        }
                    }
                }
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

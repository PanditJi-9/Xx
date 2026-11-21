package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;
import dev.redlab.gamespace.xgravitation.ui.XGravitationMarqueeTextView;
import dev.redlab.gamespace.xgravitation.ui.XGravitationTextureView;

/* loaded from: classes.dex */
public final class XgravitationActivityMainBinding implements ViewBinding {
    public final ImageView back;
    public final ImageView interStaticImagePreview;
    private final ConstraintLayout rootView;
    public final ImageView titleBackground;
    public final XGravitationTextureView videoPreview;
    public final ImageView voiceSwitch;
    public final ImageView xGravitation3aGame;
    public final XGravitationMarqueeTextView xGravitation3aGameTitle;
    public final TextView xGravitationEquipmentGravitationTitle;
    public final ImageView xGravitationEquipmentHandle;
    public final ImageView xGravitationEquipmentHandleHasUpdateIcon;
    public final XGravitationMarqueeTextView xGravitationEquipmentHandleName;
    public final ImageView xGravitationEquipmentMouse;
    public final ImageView xGravitationEquipmentMouseHasUpdateIcon;
    public final XGravitationMarqueeTextView xGravitationEquipmentMouseName;
    public final ImageView xGravitationEquipmentTouping;
    public final XGravitationMarqueeTextView xGravitationEquipmentToupingName;
    public final TextView xGravitationGameGravitation;
    public final ConstraintLayout xGravitationLeftLayout;
    public final ConstraintLayout xGravitationRightLayout;
    public final TextView xGravitationTipsContent;
    public final LinearLayout xGravitationTipsLayout;
    public final ProgressBar xGravitationTipsProgressbar;
    public final TextView xGravitationTipsTitle;
    public final TextView xGravitationTitle;

    private XgravitationActivityMainBinding(ConstraintLayout constraintLayout, ImageView imageView, ImageView imageView2, ImageView imageView3, XGravitationTextureView xGravitationTextureView, ImageView imageView4, ImageView imageView5, XGravitationMarqueeTextView xGravitationMarqueeTextView, TextView textView, ImageView imageView6, ImageView imageView7, XGravitationMarqueeTextView xGravitationMarqueeTextView2, ImageView imageView8, ImageView imageView9, XGravitationMarqueeTextView xGravitationMarqueeTextView3, ImageView imageView10, XGravitationMarqueeTextView xGravitationMarqueeTextView4, TextView textView2, ConstraintLayout constraintLayout2, ConstraintLayout constraintLayout3, TextView textView3, LinearLayout linearLayout, ProgressBar progressBar, TextView textView4, TextView textView5) {
        this.rootView = constraintLayout;
        this.back = imageView;
        this.interStaticImagePreview = imageView2;
        this.titleBackground = imageView3;
        this.videoPreview = xGravitationTextureView;
        this.voiceSwitch = imageView4;
        this.xGravitation3aGame = imageView5;
        this.xGravitation3aGameTitle = xGravitationMarqueeTextView;
        this.xGravitationEquipmentGravitationTitle = textView;
        this.xGravitationEquipmentHandle = imageView6;
        this.xGravitationEquipmentHandleHasUpdateIcon = imageView7;
        this.xGravitationEquipmentHandleName = xGravitationMarqueeTextView2;
        this.xGravitationEquipmentMouse = imageView8;
        this.xGravitationEquipmentMouseHasUpdateIcon = imageView9;
        this.xGravitationEquipmentMouseName = xGravitationMarqueeTextView3;
        this.xGravitationEquipmentTouping = imageView10;
        this.xGravitationEquipmentToupingName = xGravitationMarqueeTextView4;
        this.xGravitationGameGravitation = textView2;
        this.xGravitationLeftLayout = constraintLayout2;
        this.xGravitationRightLayout = constraintLayout3;
        this.xGravitationTipsContent = textView3;
        this.xGravitationTipsLayout = linearLayout;
        this.xGravitationTipsProgressbar = progressBar;
        this.xGravitationTipsTitle = textView4;
        this.xGravitationTitle = textView5;
    }

    @Override // androidx.viewbinding.ViewBinding
    public ConstraintLayout getRoot() {
        return this.rootView;
    }

    public static XgravitationActivityMainBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static XgravitationActivityMainBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.xgravitation_activity_main, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static XgravitationActivityMainBinding bind(View view) {
        int i = R.id.back;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.back);
        if (imageView != null) {
            i = R.id.inter_static_image_preview;
            ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.inter_static_image_preview);
            if (imageView2 != null) {
                i = R.id.title_background;
                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.title_background);
                if (imageView3 != null) {
                    i = R.id.video_preview;
                    XGravitationTextureView xGravitationTextureView = (XGravitationTextureView) ViewBindings.findChildViewById(view, R.id.video_preview);
                    if (xGravitationTextureView != null) {
                        i = R.id.voice_switch;
                        ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.voice_switch);
                        if (imageView4 != null) {
                            i = R.id.x_gravitation_3a_game;
                            ImageView imageView5 = (ImageView) ViewBindings.findChildViewById(view, R.id.x_gravitation_3a_game);
                            if (imageView5 != null) {
                                i = R.id.x_gravitation_3a_game_title;
                                XGravitationMarqueeTextView xGravitationMarqueeTextView = (XGravitationMarqueeTextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_3a_game_title);
                                if (xGravitationMarqueeTextView != null) {
                                    i = R.id.x_gravitation_equipment_gravitation_title;
                                    TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_gravitation_title);
                                    if (textView != null) {
                                        i = R.id.x_gravitation_equipment_handle;
                                        ImageView imageView6 = (ImageView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_handle);
                                        if (imageView6 != null) {
                                            i = R.id.x_gravitation_equipment_handle_has_update_icon;
                                            ImageView imageView7 = (ImageView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_handle_has_update_icon);
                                            if (imageView7 != null) {
                                                i = R.id.x_gravitation_equipment_handle_name;
                                                XGravitationMarqueeTextView xGravitationMarqueeTextView2 = (XGravitationMarqueeTextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_handle_name);
                                                if (xGravitationMarqueeTextView2 != null) {
                                                    i = R.id.x_gravitation_equipment_mouse;
                                                    ImageView imageView8 = (ImageView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_mouse);
                                                    if (imageView8 != null) {
                                                        i = R.id.x_gravitation_equipment_mouse_has_update_icon;
                                                        ImageView imageView9 = (ImageView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_mouse_has_update_icon);
                                                        if (imageView9 != null) {
                                                            i = R.id.x_gravitation_equipment_mouse_name;
                                                            XGravitationMarqueeTextView xGravitationMarqueeTextView3 = (XGravitationMarqueeTextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_mouse_name);
                                                            if (xGravitationMarqueeTextView3 != null) {
                                                                i = R.id.x_gravitation_equipment_touping;
                                                                ImageView imageView10 = (ImageView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_touping);
                                                                if (imageView10 != null) {
                                                                    i = R.id.x_gravitation_equipment_touping_name;
                                                                    XGravitationMarqueeTextView xGravitationMarqueeTextView4 = (XGravitationMarqueeTextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_equipment_touping_name);
                                                                    if (xGravitationMarqueeTextView4 != null) {
                                                                        i = R.id.x_gravitation_game_gravitation;
                                                                        TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_game_gravitation);
                                                                        if (textView2 != null) {
                                                                            i = R.id.x_gravitation_left_layout;
                                                                            ConstraintLayout constraintLayout = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.x_gravitation_left_layout);
                                                                            if (constraintLayout != null) {
                                                                                i = R.id.x_gravitation_right_layout;
                                                                                ConstraintLayout constraintLayout2 = (ConstraintLayout) ViewBindings.findChildViewById(view, R.id.x_gravitation_right_layout);
                                                                                if (constraintLayout2 != null) {
                                                                                    i = R.id.x_gravitation_tips_content;
                                                                                    TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_tips_content);
                                                                                    if (textView3 != null) {
                                                                                        i = R.id.x_gravitation_tips_layout;
                                                                                        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.x_gravitation_tips_layout);
                                                                                        if (linearLayout != null) {
                                                                                            i = R.id.x_gravitation_tips_progressbar;
                                                                                            ProgressBar progressBar = (ProgressBar) ViewBindings.findChildViewById(view, R.id.x_gravitation_tips_progressbar);
                                                                                            if (progressBar != null) {
                                                                                                i = R.id.x_gravitation_tips_title;
                                                                                                TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_tips_title);
                                                                                                if (textView4 != null) {
                                                                                                    i = R.id.x_gravitation_title;
                                                                                                    TextView textView5 = (TextView) ViewBindings.findChildViewById(view, R.id.x_gravitation_title);
                                                                                                    if (textView5 != null) {
                                                                                                        return new XgravitationActivityMainBinding((ConstraintLayout) view, imageView, imageView2, imageView3, xGravitationTextureView, imageView4, imageView5, xGravitationMarqueeTextView, textView, imageView6, imageView7, xGravitationMarqueeTextView2, imageView8, imageView9, xGravitationMarqueeTextView3, imageView10, xGravitationMarqueeTextView4, textView2, constraintLayout, constraintLayout2, textView3, linearLayout, progressBar, textView4, textView5);
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
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

package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class FunctionVirtualControllerLayoutBinding implements ViewBinding {
    public final LinearLayout functionVirtualControllerLayout;
    public final TextView functionVirtualControllerLayoutText;
    public final RecyclerView functionVirtualControllerRecyclerview;
    public final RelativeLayout functionVirtualKeyShow;
    public final ImageView functionVirtualKeyShowCheckbox;
    public final TextView functionVirtualKeyShowText;
    public final RelativeLayout functionVirtualOpenSuggestion;
    public final ImageView functionVirtualOpenSuggestionCheckbox;
    public final TextView functionVirtualOpenSuggestionText;
    public final RelativeLayout functionVirtualShakeFeedback;
    public final ImageView functionVirtualShakeFeedbackCheckbox;
    public final TextView functionVirtualShakeFeedbackText;
    public final ImageView functionVirtualVideoCourseCheckbox;
    public final RelativeLayout functionVirtualVideoCourseLayout;
    public final TextView functionVirtualVideoCourseText;
    private final LinearLayout rootView;
    public final LinearLayout virtualControllerLayout;
    public final ImageView virtualSettingImage;
    public final ImageView virtualSettingImageBtnDelete;
    public final ImageView virtualSettingImageBtnRename;

    private FunctionVirtualControllerLayoutBinding(LinearLayout linearLayout, LinearLayout linearLayout2, TextView textView, RecyclerView recyclerView, RelativeLayout relativeLayout, ImageView imageView, TextView textView2, RelativeLayout relativeLayout2, ImageView imageView2, TextView textView3, RelativeLayout relativeLayout3, ImageView imageView3, TextView textView4, ImageView imageView4, RelativeLayout relativeLayout4, TextView textView5, LinearLayout linearLayout3, ImageView imageView5, ImageView imageView6, ImageView imageView7) {
        this.rootView = linearLayout;
        this.functionVirtualControllerLayout = linearLayout2;
        this.functionVirtualControllerLayoutText = textView;
        this.functionVirtualControllerRecyclerview = recyclerView;
        this.functionVirtualKeyShow = relativeLayout;
        this.functionVirtualKeyShowCheckbox = imageView;
        this.functionVirtualKeyShowText = textView2;
        this.functionVirtualOpenSuggestion = relativeLayout2;
        this.functionVirtualOpenSuggestionCheckbox = imageView2;
        this.functionVirtualOpenSuggestionText = textView3;
        this.functionVirtualShakeFeedback = relativeLayout3;
        this.functionVirtualShakeFeedbackCheckbox = imageView3;
        this.functionVirtualShakeFeedbackText = textView4;
        this.functionVirtualVideoCourseCheckbox = imageView4;
        this.functionVirtualVideoCourseLayout = relativeLayout4;
        this.functionVirtualVideoCourseText = textView5;
        this.virtualControllerLayout = linearLayout3;
        this.virtualSettingImage = imageView5;
        this.virtualSettingImageBtnDelete = imageView6;
        this.virtualSettingImageBtnRename = imageView7;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static FunctionVirtualControllerLayoutBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static FunctionVirtualControllerLayoutBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.function_virtual_controller_layout, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static FunctionVirtualControllerLayoutBinding bind(View view) {
        int i = R.id.function_virtual_controller_layout;
        LinearLayout linearLayout = (LinearLayout) ViewBindings.findChildViewById(view, R.id.function_virtual_controller_layout);
        if (linearLayout != null) {
            i = R.id.function_virtual_controller_layout_text;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.function_virtual_controller_layout_text);
            if (textView != null) {
                i = R.id.function_virtual_controller_recyclerview;
                RecyclerView recyclerView = (RecyclerView) ViewBindings.findChildViewById(view, R.id.function_virtual_controller_recyclerview);
                if (recyclerView != null) {
                    i = R.id.function_virtual_key_show;
                    RelativeLayout relativeLayout = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_virtual_key_show);
                    if (relativeLayout != null) {
                        i = R.id.function_virtual_key_show_checkbox;
                        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.function_virtual_key_show_checkbox);
                        if (imageView != null) {
                            i = R.id.function_virtual_key_show_text;
                            TextView textView2 = (TextView) ViewBindings.findChildViewById(view, R.id.function_virtual_key_show_text);
                            if (textView2 != null) {
                                i = R.id.function_virtual_open_suggestion;
                                RelativeLayout relativeLayout2 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_virtual_open_suggestion);
                                if (relativeLayout2 != null) {
                                    i = R.id.function_virtual_open_suggestion_checkbox;
                                    ImageView imageView2 = (ImageView) ViewBindings.findChildViewById(view, R.id.function_virtual_open_suggestion_checkbox);
                                    if (imageView2 != null) {
                                        i = R.id.function_virtual_open_suggestion_text;
                                        TextView textView3 = (TextView) ViewBindings.findChildViewById(view, R.id.function_virtual_open_suggestion_text);
                                        if (textView3 != null) {
                                            i = R.id.function_virtual_shake_feedback;
                                            RelativeLayout relativeLayout3 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_virtual_shake_feedback);
                                            if (relativeLayout3 != null) {
                                                i = R.id.function_virtual_shake_feedback_checkbox;
                                                ImageView imageView3 = (ImageView) ViewBindings.findChildViewById(view, R.id.function_virtual_shake_feedback_checkbox);
                                                if (imageView3 != null) {
                                                    i = R.id.function_virtual_shake_feedback_text;
                                                    TextView textView4 = (TextView) ViewBindings.findChildViewById(view, R.id.function_virtual_shake_feedback_text);
                                                    if (textView4 != null) {
                                                        i = R.id.function_virtual_video_course_checkbox;
                                                        ImageView imageView4 = (ImageView) ViewBindings.findChildViewById(view, R.id.function_virtual_video_course_checkbox);
                                                        if (imageView4 != null) {
                                                            i = R.id.function_virtual_video_course_layout;
                                                            RelativeLayout relativeLayout4 = (RelativeLayout) ViewBindings.findChildViewById(view, R.id.function_virtual_video_course_layout);
                                                            if (relativeLayout4 != null) {
                                                                i = R.id.function_virtual_video_course_text;
                                                                TextView textView5 = (TextView) ViewBindings.findChildViewById(view, R.id.function_virtual_video_course_text);
                                                                if (textView5 != null) {
                                                                    LinearLayout linearLayout2 = (LinearLayout) view;
                                                                    i = R.id.virtual_setting_image;
                                                                    ImageView imageView5 = (ImageView) ViewBindings.findChildViewById(view, R.id.virtual_setting_image);
                                                                    if (imageView5 != null) {
                                                                        i = R.id.virtual_setting_image_btn_delete;
                                                                        ImageView imageView6 = (ImageView) ViewBindings.findChildViewById(view, R.id.virtual_setting_image_btn_delete);
                                                                        if (imageView6 != null) {
                                                                            i = R.id.virtual_setting_image_btn_rename;
                                                                            ImageView imageView7 = (ImageView) ViewBindings.findChildViewById(view, R.id.virtual_setting_image_btn_rename);
                                                                            if (imageView7 != null) {
                                                                                return new FunctionVirtualControllerLayoutBinding(linearLayout2, linearLayout, textView, recyclerView, relativeLayout, imageView, textView2, relativeLayout2, imageView2, textView3, relativeLayout3, imageView3, textView4, imageView4, relativeLayout4, textView5, linearLayout2, imageView5, imageView6, imageView7);
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

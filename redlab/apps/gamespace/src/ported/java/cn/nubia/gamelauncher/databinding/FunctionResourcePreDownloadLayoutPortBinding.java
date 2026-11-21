package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class FunctionResourcePreDownloadLayoutPortBinding implements ViewBinding {
    public final RelativeLayout functionResourcePreDownloadLayout;
    public final ImageView resourcePreDownloadCheckbox;
    public final TextView resourcePreDownloadText;
    private final RelativeLayout rootView;

    private FunctionResourcePreDownloadLayoutPortBinding(RelativeLayout relativeLayout, RelativeLayout relativeLayout2, ImageView imageView, TextView textView) {
        this.rootView = relativeLayout;
        this.functionResourcePreDownloadLayout = relativeLayout2;
        this.resourcePreDownloadCheckbox = imageView;
        this.resourcePreDownloadText = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public RelativeLayout getRoot() {
        return this.rootView;
    }

    public static FunctionResourcePreDownloadLayoutPortBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static FunctionResourcePreDownloadLayoutPortBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.function_resource_pre_download_layout_port, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static FunctionResourcePreDownloadLayoutPortBinding bind(View view) {
        RelativeLayout relativeLayout = (RelativeLayout) view;
        int i = R.id.resource_pre_download_checkbox;
        ImageView imageView = (ImageView) ViewBindings.findChildViewById(view, R.id.resource_pre_download_checkbox);
        if (imageView != null) {
            i = R.id.resource_pre_download_text;
            TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.resource_pre_download_text);
            if (textView != null) {
                return new FunctionResourcePreDownloadLayoutPortBinding(relativeLayout, relativeLayout, imageView, textView);
            }
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(i)));
    }
}

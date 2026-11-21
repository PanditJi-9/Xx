package dev.redlab.gamespace.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import dev.redlab.gamespace.R;

/* loaded from: classes.dex */
public final class ChatAssistantContentItemBinding implements ViewBinding {
    public final TextView chatAssistantContentTitle;
    private final LinearLayout rootView;

    private ChatAssistantContentItemBinding(LinearLayout linearLayout, TextView textView) {
        this.rootView = linearLayout;
        this.chatAssistantContentTitle = textView;
    }

    @Override // androidx.viewbinding.ViewBinding
    public LinearLayout getRoot() {
        return this.rootView;
    }

    public static ChatAssistantContentItemBinding inflate(LayoutInflater layoutInflater) {
        return inflate(layoutInflater, null, false);
    }

    public static ChatAssistantContentItemBinding inflate(LayoutInflater layoutInflater, ViewGroup viewGroup, boolean z) {
        View inflate = layoutInflater.inflate(R.layout.chat_assistant_content_item, viewGroup, false);
        if (z) {
            viewGroup.addView(inflate);
        }
        return bind(inflate);
    }

    public static ChatAssistantContentItemBinding bind(View view) {
        TextView textView = (TextView) ViewBindings.findChildViewById(view, R.id.chat_assistant_content_title);
        if (textView != null) {
            return new ChatAssistantContentItemBinding((LinearLayout) view, textView);
        }
        throw new NullPointerException("Missing required view with ID: ".concat(view.getResources().getResourceName(R.id.chat_assistant_content_title)));
    }
}

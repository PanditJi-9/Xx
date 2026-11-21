package dev.redlab.gamespace.view;

import android.content.Context;
import android.view.View;

/* loaded from: classes2.dex */
public class TestView extends View {
    public TestView(Context context) {
        super(context);
    }

    @Override // android.view.View
    public void setVisibility(int i) {
        super.setVisibility(i);
        setVisibility(4);
    }
}

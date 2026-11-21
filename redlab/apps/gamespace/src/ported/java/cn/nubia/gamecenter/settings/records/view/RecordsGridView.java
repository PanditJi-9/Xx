package cn.nubia.gamecenter.settings.records.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.GridView;

/* loaded from: classes.dex */
public class RecordsGridView extends GridView {
    @Override // android.widget.AbsListView, android.view.ViewGroup
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override // android.widget.AbsListView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        return true;
    }

    public RecordsGridView(Context context) {
        super(context);
    }

    public RecordsGridView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }
}

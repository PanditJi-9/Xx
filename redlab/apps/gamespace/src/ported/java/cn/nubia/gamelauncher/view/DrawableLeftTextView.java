package dev.redlab.gamespace.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewParent;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import cn.nubia.globalsearch.GlobalSearchConstants;

/* loaded from: classes2.dex */
public class DrawableLeftTextView extends TextView {
    public DrawableLeftTextView(Context context) {
        super(context);
    }

    public DrawableLeftTextView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public DrawableLeftTextView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.widget.TextView, android.view.View
    protected void onDraw(Canvas canvas) {
        float measureText;
        Drawable[] compoundDrawables = getCompoundDrawables();
        if (compoundDrawables != null) {
            Drawable drawable = compoundDrawables[0];
            int paddingLeft = getPaddingLeft() + getPaddingRight();
            if (getLayout() != null) {
                measureText = getLayout().getLineWidth(0);
            } else {
                measureText = (getPaint() == null || getText() == null) ? 100.0f : getPaint().measureText(getText().toString().trim());
            }
            if (drawable == null) {
                Log.d(GlobalSearchConstants.NAME, "onDraw() getWidth() : " + getWidth() + ", bodyWidth : " + measureText + ", padding : " + paddingLeft + ", text : " + ((Object) getText()));
                canvas.translate(((getWidth() - measureText) - paddingLeft) / 2.0f, 0.0f);
            }
            super.onDraw(canvas);
        }
    }

    private int getParentWidth() {
        ViewParent parent = getParent();
        if (parent instanceof ConstraintLayout) {
            ConstraintLayout constraintLayout = (ConstraintLayout) parent;
            Log.d(GlobalSearchConstants.NAME, "getParentWidth() getWidth() : " + constraintLayout.getWidth());
            return constraintLayout.getWidth();
        }
        return getWidth();
    }
}

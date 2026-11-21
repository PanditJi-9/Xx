package dev.redlab.gamespace.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;

/* loaded from: classes2.dex */
public class AlphaGroup extends Group {
    @Override // androidx.constraintlayout.widget.ConstraintHelper
    public void updatePreLayout(ConstraintLayout constraintLayout) {
    }

    public AlphaGroup(Context context) {
        super(context);
    }

    public AlphaGroup(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public AlphaGroup(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // android.view.View
    public void setAlpha(float f) {
        super.setAlpha(f);
        ViewParent parent = getParent();
        if (parent instanceof ConstraintLayout) {
            ConstraintLayout constraintLayout = (ConstraintLayout) parent;
            for (int i = 0; i < this.mCount; i++) {
                View viewById = constraintLayout.getViewById(this.mIds[i]);
                if (viewById != null) {
                    viewById.setAlpha(f);
                }
            }
        }
    }
}

package dev.redlab.gamespace.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import dev.redlab.gamespace.R;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes2.dex */
public class LargeGameTabView extends ConstraintLayout implements View.OnClickListener {
    private int mColorDefault;
    private OnTabChangeListener mListener;
    private float mSizeSelected;
    private float mSizeUnselected;
    private ArrayList<TextView> mTabs;

    public interface OnTabChangeListener {
        void onTabChanged(int i);
    }

    public LargeGameTabView(Context context) {
        this(context, null);
    }

    public LargeGameTabView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mTabs = new ArrayList<>();
        this.mColorDefault = getResources().getColor(R.color.forty_percent_white, null);
        this.mSizeSelected = getResources().getDimensionPixelSize(R.dimen.tab_selected_size);
        this.mSizeUnselected = getResources().getDimensionPixelSize(R.dimen.tab_unselected_size);
        initChild(context);
    }

    private void initChild(Context context) {
        LayoutInflater.from(context).inflate(R.layout.large_tab_layout, this);
        this.mTabs.add((TextView) findViewById(R.id.xbox));
        this.mTabs.add((TextView) findViewById(R.id.steam));
        this.mTabs.add((TextView) findViewById(R.id.station));
        this.mTabs.add((TextView) findViewById(R.id.epic));
        Iterator<TextView> it = this.mTabs.iterator();
        while (it.hasNext()) {
            it.next().setOnClickListener(this);
        }
        selectedTab(this.mTabs.get(0));
    }

    @Override // android.view.View.OnClickListener
    public void onClick(View view) {
        selectedTab(view);
        OnTabChangeListener onTabChangeListener = this.mListener;
        if (onTabChangeListener == null) {
            return;
        }
        onTabChangeListener.onTabChanged(view.getId());
    }

    private void selectedTab(View view) {
        Iterator<TextView> it = this.mTabs.iterator();
        while (it.hasNext()) {
            TextView next = it.next();
            updateTextView(next, next.getId() == view.getId());
        }
    }

    private void updateTextView(TextView textView, boolean z) {
        textView.setTextColor(z ? -1 : this.mColorDefault);
        textView.setTextSize(0, z ? this.mSizeSelected : this.mSizeUnselected);
        textView.getPaint().setFakeBoldText(z);
    }

    public void setOnTabChangeListener(OnTabChangeListener onTabChangeListener) {
        this.mListener = onTabChangeListener;
    }
}

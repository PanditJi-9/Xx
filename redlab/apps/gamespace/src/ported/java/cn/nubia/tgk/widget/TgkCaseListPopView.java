package cn.nubia.tgk.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dev.redlab.gamespace.R;

/* loaded from: classes2.dex */
public class TgkCaseListPopView extends RelativeLayout {
    private static final String TAG = "TgkCaseListPopView";
    private LinearLayoutManager mLinearLayoutManager;
    private OnClickListener mListener;
    private RecyclerView mRecyclerView;
    private View mRootView;
    private View.OnClickListener mViewListener;

    public interface OnClickListener {
        void onClick(View view);
    }

    public TgkCaseListPopView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public TgkCaseListPopView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mViewListener = new View.OnClickListener() { // from class: cn.nubia.tgk.widget.TgkCaseListPopView.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                TgkCaseListPopView.this.mListener.onClick(TgkCaseListPopView.this.mRootView);
            }
        };
        Log.d(TAG, "in TgkCaseListPopView");
        initView(context, attributeSet);
    }

    private void initView(Context context, AttributeSet attributeSet) {
        this.mRootView = ((LayoutInflater) getContext().getSystemService("layout_inflater")).inflate(R.layout.tgk_pop_case_list_view_layout, (ViewGroup) this, true);
        this.mRecyclerView = (RecyclerView) findViewById(R.id.tgk_case_rcy_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        this.mLinearLayoutManager = linearLayoutManager;
        this.mRecyclerView.setLayoutManager(linearLayoutManager);
        Log.d(TAG, "in initView");
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
        this.mRecyclerView.setAdapter(adapter);
    }

    public void scrollToPositionWithOffset(int i) {
        this.mLinearLayoutManager.scrollToPositionWithOffset(i, 0);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.mListener = onClickListener;
    }
}

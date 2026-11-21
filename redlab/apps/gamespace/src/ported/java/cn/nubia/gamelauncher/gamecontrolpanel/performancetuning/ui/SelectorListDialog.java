package dev.redlab.gamespace.gamecontrolpanel.performancetuning.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import dev.redlab.gamespace.R;
import java.util.List;

/* loaded from: classes.dex */
public class SelectorListDialog {
    private int mBackgroundResId;
    private Context mContext;
    private int mNormalTextColor;
    private PopupWindow.OnDismissListener mOnDismissListener;
    private PopupWindow mPopupWindow;
    private SelectItemListener mSelectItemListener;
    private int mSelectedTextColor;
    private List<SelectorItem> mSelectorItemList;
    private int mTextViewLayout;

    public interface SelectItemListener {
        void onSelectedItem(String str);
    }

    public static class SelectorItem {
        public boolean mSelected;
        public String mText;
    }

    public SelectorListDialog(Context context) {
        this.mContext = context;
    }

    public void setTextViewLayout(int i) {
        this.mTextViewLayout = i;
    }

    public void setBackground(int i) {
        this.mBackgroundResId = i;
    }

    public void setNormalTextColor(int i) {
        this.mNormalTextColor = i;
    }

    public void setSelectedTextColor(int i) {
        this.mSelectedTextColor = i;
    }

    public void setDataList(List<SelectorItem> list) {
        this.mSelectorItemList = list;
    }

    public void setSelectItemListener(SelectItemListener selectItemListener) {
        this.mSelectItemListener = selectItemListener;
    }

    public void setOnDismissListener(PopupWindow.OnDismissListener onDismissListener) {
        this.mOnDismissListener = onDismissListener;
    }

    public void showPopupWindow(View view, int i) {
        this.mPopupWindow = new PopupWindow(this.mContext);
        LayoutInflater from = LayoutInflater.from(this.mContext);
        RelativeLayout relativeLayout = (RelativeLayout) from.inflate(R.layout.custom_perf_dialog_selector_list, (ViewGroup) null);
        RecyclerView recyclerView = (RecyclerView) relativeLayout.findViewById(R.id.rv_content);
        recyclerView.setBackground(this.mContext.getDrawable(this.mBackgroundResId));
        initRecyclerView(recyclerView, from);
        this.mPopupWindow.setContentView(relativeLayout);
        this.mPopupWindow.setWidth(view.getWidth());
        this.mPopupWindow.setHeight(i);
        this.mPopupWindow.setTouchable(true);
        this.mPopupWindow.setFocusable(true);
        this.mPopupWindow.setBackgroundDrawable(null);
        this.mPopupWindow.setOnDismissListener(this.mOnDismissListener);
        this.mPopupWindow.showAsDropDown(view, 0, 0);
    }

    private void initRecyclerView(RecyclerView recyclerView, LayoutInflater layoutInflater) {
        SelectorAdapter selectorAdapter = new SelectorAdapter(layoutInflater);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext(), 1, false));
        recyclerView.setAdapter(selectorAdapter);
        for (int i = 0; i < this.mSelectorItemList.size(); i++) {
            if (this.mSelectorItemList.get(i).mSelected) {
                recyclerView.scrollToPosition(i);
                return;
            }
        }
    }

    private class SelectorAdapter extends RecyclerView.Adapter<SelectorViewHolder> {
        private LayoutInflater mInflater;

        public SelectorAdapter(LayoutInflater layoutInflater) {
            this.mInflater = layoutInflater;
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public SelectorViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new SelectorViewHolder((TextView) this.mInflater.inflate(SelectorListDialog.this.mTextViewLayout, viewGroup, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(SelectorViewHolder selectorViewHolder, int i) {
            final SelectorItem selectorItem = (SelectorItem) SelectorListDialog.this.mSelectorItemList.get(i);
            selectorViewHolder.mTextItem.setText(selectorItem.mText);
            if (selectorItem.mSelected) {
                selectorViewHolder.mTextItem.setTextColor(SelectorListDialog.this.mContext.getColor(SelectorListDialog.this.mSelectedTextColor));
            } else {
                selectorViewHolder.mTextItem.setTextColor(SelectorListDialog.this.mContext.getColor(SelectorListDialog.this.mNormalTextColor));
            }
            selectorViewHolder.mTextItem.setOnClickListener(new View.OnClickListener() { // from class: dev.redlab.gamespace.gamecontrolpanel.performancetuning.ui.SelectorListDialog.SelectorAdapter.1
                @Override // android.view.View.OnClickListener
                public void onClick(View view) {
                    SelectorListDialog.this.mPopupWindow.dismiss();
                    SelectorListDialog.this.mSelectItemListener.onSelectedItem(selectorItem.mText);
                }
            });
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            return SelectorListDialog.this.mSelectorItemList.size();
        }
    }

    private static class SelectorViewHolder extends RecyclerView.ViewHolder {
        private TextView mTextItem;

        public SelectorViewHolder(View view) {
            super(view);
            this.mTextItem = (TextView) view;
        }
    }
}

package cn.nubia.gamecenter.settings.summary;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.summary.SummaryPageHelper;
import cn.nubia.gamecenter.settings.summary.SummaryRankHelper;
import cn.nubia.gamecenter.settings.summary.entities.GameAppInfo;
import cn.nubia.gamecenter.settings.utils.LogUtil;

/* loaded from: classes.dex */
public class SummaryRankHelper extends SummaryPageHelper {
    private static final String TAG = "SummaryRankHelper";
    private RankAdpter m_adapter;
    private View m_emptyView;
    private RecyclerView m_listView;

    SummaryRankHelper(View view, SummaryPageHelper.Callback callback, int i) {
        super(view, callback, i);
    }

    @Override // cn.nubia.gamecenter.settings.summary.SummaryPageHelper
    public void update(SummaryDataHelper summaryDataHelper) {
        updateList(summaryDataHelper);
    }

    private void updateList(SummaryDataHelper summaryDataHelper) {
        updateShowState(summaryDataHelper.getAppCount() == 0);
        if (getAdapter() != null) {
            getAdapter().setData(summaryDataHelper);
        }
    }

    private void updateShowState(boolean z) {
        if (getEmptyView() != null) {
            getEmptyView().setVisibility(z ? 0 : 8);
        }
        if (getListView() != null) {
            getListView().setVisibility(z ? 8 : 0);
        }
    }

    private RankAdpter getAdapter() {
        if (this.m_adapter == null && getListView() != null) {
            this.m_adapter = new RankAdpter();
            getListView().setAdapter(this.m_adapter);
        }
        return this.m_adapter;
    }

    private RecyclerView getListView() {
        if (this.m_listView == null) {
            this.m_listView = (RecyclerView) this.m_root.findViewById(R.id.rank_list);
            this.m_listView.setLayoutManager(new LinearLayoutManager(this.m_listView.getContext(), 1, false));
        }
        return this.m_listView;
    }

    private View getEmptyView() {
        if (this.m_emptyView == null) {
            this.m_emptyView = this.m_root.findViewById(R.id.rank_empty);
        }
        return this.m_emptyView;
    }

    class RankAdpter extends RecyclerView.Adapter {
        private static final String ACTION_GAME_CARRER = "cn.nubia.gamecenter.settings.action.GAME_CARRER";
        private SummaryDataHelper m_data;

        RankAdpter() {
        }

        public void setData(SummaryDataHelper summaryDataHelper) {
            this.m_data = summaryDataHelper;
            notifyDataSetChanged();
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.gcs_summary_rank_item, viewGroup, false));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
            SummaryDataHelper summaryDataHelper = this.m_data;
            if (summaryDataHelper == null) {
                return;
            }
            ViewHolder viewHolder2 = (ViewHolder) viewHolder;
            GameAppInfo appItem = summaryDataHelper.getAppItem(i);
            if (viewHolder2 == null || appItem == null) {
                return;
            }
            viewHolder2.rankNumber.setText(String.valueOf(i + 1));
            viewHolder2.rankNumber.setBackgroundResource(getNumberBackground(i));
            viewHolder2.icon.setImageDrawable(appItem.icon);
            viewHolder2.name.setText(appItem.label);
            viewHolder2.time.setText(SummaryRankHelper.this.msToFormatTime(false, (int) appItem.totalTimeInForeground));
            viewHolder2.rankItem.setTag(Integer.valueOf(i));
        }

        @Override // androidx.recyclerview.widget.RecyclerView.Adapter
        public int getItemCount() {
            SummaryDataHelper summaryDataHelper = this.m_data;
            if (summaryDataHelper == null) {
                return 0;
            }
            return summaryDataHelper.getAppCount();
        }

        private int getNumberBackground(int i) {
            return i > 2 ? R.drawable.gcs_summary_rank_number_bg_2 : R.drawable.gcs_summary_rank_number_bg_1;
        }

        /* JADX INFO: Access modifiers changed from: private */
        class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView icon;
            private TextView name;
            private View rankItem;
            private TextView rankNumber;
            private TextView time;

            public ViewHolder(View view) {
                super(view);
                this.rankNumber = (TextView) view.findViewById(R.id.rank_item_number);
                this.icon = (ImageView) view.findViewById(R.id.rank_item_icon);
                this.name = (TextView) view.findViewById(R.id.rank_item_name);
                this.time = (TextView) view.findViewById(R.id.rank_item_time);
                View findViewById = view.findViewById(R.id.rank_item);
                this.rankItem = findViewById;
                findViewById.setOnClickListener(new View.OnClickListener() { // from class: cn.nubia.gamecenter.settings.summary.SummaryRankHelper$RankAdpter$ViewHolder$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view2) {
                        SummaryRankHelper.RankAdpter.ViewHolder.this.m218x17beabf(view2);
                    }
                });
            }

            /* renamed from: lambda$new$0$cn-nubia-gamecenter-settings-summary-SummaryRankHelper$RankAdpter$ViewHolder, reason: not valid java name */
            /* synthetic */ void m218x17beabf(View view) {
                RankAdpter.this.startCarrerActivity(view.getContext(), RankAdpter.this.m_data.getAppItem(((Integer) view.getTag()).intValue()));
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void startCarrerActivity(Context context, GameAppInfo gameAppInfo) {
            LogUtil.i(SummaryRankHelper.TAG, " info= " + gameAppInfo);
            Intent intent = new Intent(ACTION_GAME_CARRER);
            intent.putExtra("label", gameAppInfo.label);
            intent.putExtra("pkgName", gameAppInfo.pkgName);
            context.startActivity(intent);
        }
    }
}

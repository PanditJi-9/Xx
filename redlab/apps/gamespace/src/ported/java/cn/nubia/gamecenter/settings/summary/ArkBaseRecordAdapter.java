package cn.nubia.gamecenter.settings.summary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import cn.nubia.gamecenter.settings.R;
import cn.nubia.gamecenter.settings.summary.entities.GameRecord;
import java.util.List;

/* loaded from: classes.dex */
public class ArkBaseRecordAdapter extends BaseAdapter {
    private static final int RECORD_TIME_DESCRIPTION = -2130706433;
    private static final int RECORD_TIME_DESCRIPTION_HIGHLIGHT = -11628033;
    private final Context mContext;
    private List<GameRecord> mGameRecords;
    private LayoutInflater mInflater;
    private OnIconClickListener mOnIconClickListener;

    public interface OnIconClickListener {
        void onIconClick(int i);
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return i;
    }

    public ArkBaseRecordAdapter(Context context) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override // android.widget.Adapter
    public int getCount() {
        List<GameRecord> list = this.mGameRecords;
        if (list != null) {
            return list.size();
        }
        return 0;
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        List<GameRecord> list = this.mGameRecords;
        if (list == null || list.size() <= i) {
            return null;
        }
        return this.mGameRecords.get(i);
    }

    public void setData(List<GameRecord> list) {
        this.mGameRecords = list;
        notifyDataSetChanged();
    }

    public void setOnIconClickListener(OnIconClickListener onIconClickListener) {
        this.mOnIconClickListener = onIconClickListener;
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = this.mInflater.inflate(R.layout.ark_base_record_item, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.mPkgIcon = (ImageView) view.findViewById(R.id.record_item_icon);
            viewHolder.mPkgLabel = (TextView) view.findViewById(R.id.record_item_label);
            viewHolder.mPkgTime = (TextView) view.findViewById(R.id.record_item_time);
            viewHolder.mPkgIcon.setTag(Integer.valueOf(i));
            viewHolder.mPkgIcon.setOnClickListener(new View.OnClickListener() { // from class: cn.nubia.gamecenter.settings.summary.ArkBaseRecordAdapter.1
                @Override // android.view.View.OnClickListener
                public void onClick(View view2) {
                    if (ArkBaseRecordAdapter.this.mOnIconClickListener != null) {
                        ArkBaseRecordAdapter.this.mOnIconClickListener.onIconClick(((Integer) ((ImageView) view2).getTag()).intValue());
                    }
                }
            });
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
            viewHolder.mPkgIcon.setTag(Integer.valueOf(i));
            viewHolder.mPkgIcon.setOnClickListener(new View.OnClickListener() { // from class: cn.nubia.gamecenter.settings.summary.ArkBaseRecordAdapter.2
                @Override // android.view.View.OnClickListener
                public void onClick(View view2) {
                    if (ArkBaseRecordAdapter.this.mOnIconClickListener != null) {
                        ArkBaseRecordAdapter.this.mOnIconClickListener.onIconClick(((Integer) ((ImageView) view2).getTag()).intValue());
                    }
                }
            });
        }
        GameRecord gameRecord = (GameRecord) getItem(i);
        if (viewHolder != null && gameRecord != null) {
            viewHolder.mPkgIcon.setImageDrawable(gameRecord.icon);
            viewHolder.mPkgLabel.setText(gameRecord.label);
            int millisToHour = ArkBaseHelper.millisToHour(gameRecord.totalTimeInForeground);
            String string = this.mContext.getString(R.string.arkbase_record_description);
            if (millisToHour >= 1) {
                string = String.format(this.mContext.getString(R.string.arkbase_record_time_description, Integer.valueOf(millisToHour)), new Object[0]);
            }
            if (millisToHour >= 100) {
                viewHolder.mPkgTime.setTextColor(RECORD_TIME_DESCRIPTION_HIGHLIGHT);
            } else {
                viewHolder.mPkgTime.setTextColor(RECORD_TIME_DESCRIPTION);
            }
            viewHolder.mPkgTime.setText(string);
        }
        return view;
    }

    private class ViewHolder {
        private ImageView mPkgIcon;
        private TextView mPkgLabel;
        private TextView mPkgTime;

        private ViewHolder() {
        }
    }
}

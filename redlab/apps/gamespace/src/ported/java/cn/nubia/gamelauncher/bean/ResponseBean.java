package dev.redlab.gamespace.bean;

import dev.redlab.gamespace.util.LogUtil;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class ResponseBean {
    private ArrayList<GameItemBean> mGameItemBean = new ArrayList<>();
    private int mStateCode;
    private String mStateMsg;

    public ResponseBean(JSONObject jSONObject, int i) {
        try {
            this.mStateCode = jSONObject.getInt("StateCode");
            if (i == 200) {
                JSONArray jSONArray = jSONObject.getJSONArray("Data");
                for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                    this.mGameItemBean.add(new GameItemBean((JSONObject) jSONArray.get(i2)));
                }
                return;
            }
            this.mGameItemBean.add(new GameItemBean(jSONObject.getJSONObject("Data")));
        } catch (Exception e) {
            LogUtil.e("ResponseBean", "ResponseBean init Error!!");
            throw new RuntimeException(e);
        }
    }

    public int getStateCode() {
        return this.mStateCode;
    }

    public void setStateCode(int i) {
        this.mStateCode = i;
    }

    public String getStateMsg() {
        return this.mStateMsg;
    }

    public void setStateMsg(String str) {
        this.mStateMsg = str;
    }

    public ArrayList<GameItemBean> getGameItemBean() {
        return this.mGameItemBean;
    }

    public void setGameItemBean(ArrayList<GameItemBean> arrayList) {
        this.mGameItemBean = arrayList;
    }

    public String toString() {
        return "ResponseBean{StateCode=" + this.mStateCode + ", StateMsg='" + this.mStateMsg + "', GameItemBean=" + this.mGameItemBean + '}';
    }
}

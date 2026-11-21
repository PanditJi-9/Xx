package dev.redlab.gamespace.bean;

import dev.redlab.gamespace.util.LogUtil;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

/* loaded from: classes.dex */
public class CheckStateResponse {
    private static final String JSON_DATA = "Data";
    private static final String JSON_STATECODE = "StateCode";
    private ArrayList<CheckStateBean> data = new ArrayList<>();
    private int stateCode;
    private String stateMsg;

    public CheckStateResponse(JSONObject jSONObject) {
        try {
            this.stateCode = jSONObject.getInt(JSON_STATECODE);
            JSONArray jSONArray = jSONObject.getJSONArray(JSON_DATA);
            for (int i = 0; i < jSONArray.length(); i++) {
                this.data.add(new CheckStateBean((JSONObject) jSONArray.get(i)));
            }
        } catch (Exception unused) {
            LogUtil.e("ResponseBean", "ResponseBean init Error!!");
        }
    }

    public int getStateCode() {
        return this.stateCode;
    }

    public void setStateCode(int i) {
        this.stateCode = i;
    }

    public String getStateMsg() {
        return this.stateMsg;
    }

    public void setStateMsg(String str) {
        this.stateMsg = str;
    }

    public ArrayList<CheckStateBean> getData() {
        return this.data;
    }

    public void setData(ArrayList<CheckStateBean> arrayList) {
        this.data = arrayList;
    }

    public String toString() {
        return "CheckStateResponse{stateCode=" + this.stateCode + ", stateMsg='" + this.stateMsg + "', data=" + this.data + '}';
    }
}

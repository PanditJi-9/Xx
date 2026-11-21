package dev.redlab.gamespace.gamecontrolpanel.superresolution;

import java.util.HashMap;

/* loaded from: classes.dex */
public class ItemData {
    private String mDescription;
    private HashMap mHashMap;
    private boolean mSelected;
    private String mSuperGear;

    public String getSuperGear() {
        return this.mSuperGear;
    }

    public void setSuperGear(String str) {
        this.mSuperGear = str;
    }

    public HashMap getHashMap() {
        return this.mHashMap;
    }

    public void setHashMap(HashMap hashMap) {
        this.mHashMap = hashMap;
    }

    public boolean isSelected() {
        return this.mSelected;
    }

    public void setSelected(boolean z) {
        this.mSelected = z;
    }

    public String getDescription() {
        return this.mDescription;
    }

    public void setDescription(String str) {
        this.mDescription = str;
    }
}

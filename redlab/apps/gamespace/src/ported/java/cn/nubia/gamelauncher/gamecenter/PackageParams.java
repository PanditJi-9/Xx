package dev.redlab.gamespace.gamecenter;

import dev.redlab.gamespace.util.HTTPUtils;
import java.util.ArrayList;
import java.util.HashMap;

/* loaded from: classes.dex */
public class PackageParams {
    private static final String KEY_APP_ID = "AppId";
    private static final String KEY_PACKAGE_NAME = "PackageName";
    private static final String KEY_PACKAGE_NAMES = "PackageNames";
    private static final String KEY_PAGE_NO = "PageNo";
    private static final String KEY_PAGE_SIZE = "PageSize";
    private static final String KEY_SIGN = "Sign";
    private static final String KEY_TIME = "Time";
    private static final String KEY_TOPIC_ID = "TopicId";
    public static final int RECOMMEND_GAME_SIZE = 3;
    public static final int TYPE_PACKAGES_DATA = 300;
    public static final int TYPE_PACKAGE_LIST_PARAMS = 200;
    public static final int TYPE_PACKAGE_PARAMS = 100;
    private HashMap<String, String> result = new HashMap<>();

    public PackageParams() {
    }

    public PackageParams(String str) {
        createParams(str);
    }

    public PackageParams(ArrayList<String> arrayList) {
        creatListParams(arrayList);
    }

    public HashMap<String, String> getParams() {
        return this.result;
    }

    private void createParams(String str) {
        this.result.clear();
        long currentTimeMillis = System.currentTimeMillis() / 1000;
        this.result.put(KEY_PACKAGE_NAME, str);
        this.result.put(KEY_TIME, String.valueOf(currentTimeMillis));
        HashMap<String, String> hashMap = this.result;
        hashMap.put(KEY_SIGN, HTTPUtils.getDigestSign(HTTPUtils.sortParms(hashMap)));
    }

    private void creatListParams(ArrayList<String> arrayList) {
        Builder packageList = new Builder(200).setPackageList(arrayList);
        this.result.clear();
        long currentTimeMillis = System.currentTimeMillis() / 1000;
        this.result.put(KEY_PACKAGE_NAMES, packageList.getPackageNameList());
        this.result.put(KEY_TIME, String.valueOf(currentTimeMillis));
        HashMap<String, String> hashMap = this.result;
        hashMap.put(KEY_SIGN, HTTPUtils.getDigestSign(HTTPUtils.sortParms(hashMap)));
    }

    public HashMap<String, String> getTopicSoftListParams(int i) {
        this.result.clear();
        long currentTimeMillis = System.currentTimeMillis() / 1000;
        this.result.put(KEY_APP_ID, HTTPUtils.APP_ID);
        this.result.put(KEY_TOPIC_ID, HTTPUtils.TOPIC_ID);
        this.result.put(KEY_PAGE_NO, "1");
        this.result.put(KEY_PAGE_SIZE, String.valueOf(i));
        this.result.put(KEY_TIME, String.valueOf(currentTimeMillis));
        HashMap<String, String> hashMap = this.result;
        hashMap.put(KEY_SIGN, HTTPUtils.getDigestSign(HTTPUtils.sortParms(hashMap), HTTPUtils.SECRET));
        return this.result;
    }

    public HashMap<String, String> checkTopicSoft(ArrayList<String> arrayList) {
        this.result.clear();
        long currentTimeMillis = System.currentTimeMillis() / 1000;
        this.result.put(KEY_APP_ID, HTTPUtils.APP_ID);
        this.result.put(KEY_PACKAGE_NAMES, String.join(",", arrayList));
        this.result.put(KEY_TOPIC_ID, HTTPUtils.TOPIC_ID);
        this.result.put(KEY_TIME, String.valueOf(currentTimeMillis));
        HashMap<String, String> hashMap = this.result;
        hashMap.put(KEY_SIGN, HTTPUtils.getDigestSign(HTTPUtils.sortParms(hashMap), HTTPUtils.SECRET));
        return this.result;
    }

    public static class Builder {
        private String mPackageName;
        private String mPackageNameList;
        private String mSign;
        private long mTime = -1;
        private int mType;

        public Builder(int i) {
            this.mType = i;
        }

        public Builder setPackageName(String str) {
            this.mPackageName = str;
            return this;
        }

        public Builder setTime(long j) {
            this.mTime = j;
            return this;
        }

        public Builder setSign(String str) {
            this.mSign = str;
            return this;
        }

        public Builder setPackageList(ArrayList<String> arrayList) {
            this.mPackageNameList = getListString(arrayList);
            return this;
        }

        public String getPackageNameList() {
            return this.mPackageNameList;
        }

        public String toString() {
            checkInfos();
            int i = this.mType;
            if (i == 100) {
                return getString(PackageParams.KEY_PACKAGE_NAME, this.mPackageName) + "&" + getString(PackageParams.KEY_TIME, String.valueOf(this.mTime)) + "&" + getString(PackageParams.KEY_SIGN, this.mSign);
            }
            if (i != 200) {
                return "";
            }
            return getString(PackageParams.KEY_PACKAGE_NAMES, this.mPackageNameList) + "&" + getString(PackageParams.KEY_TIME, String.valueOf(this.mTime)) + "&" + getString(PackageParams.KEY_SIGN, this.mSign);
        }

        private String getListString(ArrayList<String> arrayList) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < arrayList.size(); i++) {
                sb.append("{\"PackageName\":");
                sb.append("\"" + arrayList.get(i) + "\"}");
                if (i < arrayList.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            return sb.toString();
        }

        private String getString(String str, String str2) {
            return str + ApiSignUtil.CONNECTOR_TAG + str2;
        }

        private void checkInfos() {
            int i = this.mType;
            if (i == -1) {
                throw new RuntimeException("PackageParams type error!!");
            }
            if (i == 100) {
                if (this.mPackageName.isEmpty() || this.mSign.isEmpty() || this.mTime == -1) {
                    throw new RuntimeException("PackageParams error!!");
                }
                return;
            }
            if (i == 200) {
                if (this.mPackageNameList.isEmpty() || this.mSign.isEmpty() || this.mTime == -1) {
                    throw new RuntimeException("PackageParams list error!!");
                }
            }
        }
    }
}

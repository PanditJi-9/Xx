package dev.redlab.gamespace.util;

import dev.redlab.gamespace.gamecenter.ApiSignUtil;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/* loaded from: classes2.dex */
public class HTTPUtils {
    public static final String APP_ID = "10004";
    private static final String CHECK_TOPIC_SOFT = "/ExtTopic/ZTE/CheckTopicSoft";
    private static final String GET_SOFT_BY_PACKAGE_NAME = "/RedMagic/V3/GetSoftByPackageName";
    private static final String GET_SOFT_LIST_BY_PACKAGE_NAMES = "/RedMagic/V3/GetSoftListByPackageNames";
    private static final String GET_TOPIC_SOFT_LIST = "/ExtTopic/ZTE/GetTopicSoftList";
    public static final String SECRET = "rZ0EcSnZ1E6UO2kfbMuQ";
    public static final String SECURITY_KEY = "GjWj8pc4bYq19NDkQ86fd6MtlW650Tki";
    public static final String TOPIC_ID = "901";

    public static String sortParms(HashMap hashMap) {
        TreeMap treeMap = new TreeMap();
        treeMap.putAll(hashMap);
        Set<String> keySet = treeMap.keySet();
        StringBuffer stringBuffer = new StringBuffer();
        for (String str : keySet) {
            Object obj = treeMap.get(str);
            if (obj instanceof List) {
                List list = (List) obj;
                Collections.sort(list);
                Iterator it = list.iterator();
                while (it.hasNext()) {
                    stringBuffer.append(str).append(ApiSignUtil.CONNECTOR_TAG).append(String.valueOf(it.next()));
                }
            } else {
                stringBuffer.append(str).append(ApiSignUtil.CONNECTOR_TAG).append(treeMap.get(str));
            }
        }
        return stringBuffer.toString();
    }

    public static String getDigestSign(String str) {
        return getDigestSign(str, SECURITY_KEY);
    }

    public static String getDigestSign(String str, String str2) {
        if (str == null) {
            throw new UnsupportedOperationException();
        }
        try {
            String str3 = str + str2;
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(str3.getBytes("utf-8"));
            return bytes2Hex(messageDigest.digest());
        } catch (Exception unused) {
            return null;
        }
    }

    private static String bytes2Hex(byte[] bArr) {
        String str = "";
        for (byte b : bArr) {
            String hexString = Integer.toHexString(b & 255);
            if (hexString.length() == 1) {
                str = str + "0";
            }
            str = str + hexString;
        }
        return str;
    }

    public static String getSoftByPackageName() {
        return getDomainUrl() + GET_SOFT_BY_PACKAGE_NAME;
    }

    public static String getSoftListByPackageNames() {
        return getDomainUrl() + GET_SOFT_LIST_BY_PACKAGE_NAMES;
    }

    public static String getTopicSoftList() {
        return getDomainUrl() + GET_TOPIC_SOFT_LIST;
    }

    public static String checkTopicSoft() {
        return getDomainUrl() + CHECK_TOPIC_SOFT;
    }

    public static String getDomainUrl() {
        return "";
    }
}

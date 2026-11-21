package dev.redlab.gamespace.gamecenter;

import com.google.common.base.Ascii;
import java.security.MessageDigest;

/* loaded from: classes.dex */
public class MD5Util {
    private static final char[] HEX_CHARS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String md5(String str) {
        if (str == null) {
            str = "";
        }
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(str.getBytes("UTF8"));
            byte[] digest = messageDigest.digest();
            char[] cArr = new char[digest.length * 2];
            int i = 0;
            for (byte b : digest) {
                int i2 = i + 1;
                char[] cArr2 = HEX_CHARS;
                cArr[i] = cArr2[(b >>> 4) & 15];
                i += 2;
                cArr[i2] = cArr2[b & Ascii.SI];
            }
            return new String(cArr);
        } catch (Exception unused) {
            return "";
        }
    }

    public static String md5(byte[] bArr) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bArr);
            byte[] digest = messageDigest.digest();
            char[] cArr = new char[digest.length * 2];
            int i = 0;
            for (byte b : digest) {
                int i2 = i + 1;
                char[] cArr2 = HEX_CHARS;
                cArr[i] = cArr2[(b >>> 4) & 15];
                i += 2;
                cArr[i2] = cArr2[b & Ascii.SI];
            }
            return new String(cArr);
        } catch (Exception unused) {
            return "";
        }
    }
}

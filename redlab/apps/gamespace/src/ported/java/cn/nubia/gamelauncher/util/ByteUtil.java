package dev.redlab.gamespace.util;

import com.google.common.base.Ascii;

/* loaded from: classes2.dex */
public class ByteUtil {
    private static char[] HEX_CHAR = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static String byteArray2hexString(byte[] bArr) {
        StringBuilder sb = new StringBuilder();
        if (bArr != null) {
            for (int i = 0; i < bArr.length; i++) {
                sb.append(HEX_CHAR[(bArr[i] & 240) >>> 4]);
                sb.append(HEX_CHAR[bArr[i] & Ascii.SI]);
                if (i < bArr.length - 1) {
                    sb.append(" ");
                }
            }
        }
        return sb.toString();
    }
}

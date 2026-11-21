package cn.nubia.common.util;

import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/* loaded from: classes.dex */
public class FileOperator {
    private static String TAG = "FileOperator";

    public static boolean isFileExists(File file, String str) {
        return new File(file, str).exists();
    }

    public static boolean isFoundSubFileInDir(String str) {
        if (str == null) {
            return false;
        }
        File file = new File(str);
        return file.exists() && file.isDirectory() && file.listFiles().length > 0;
    }

    public static boolean copyDir(String str, String str2, String str3) {
        Log.d(TAG, "copyDir() src = " + str + ", dest = " + str2);
        File file = new File(str);
        if (str2 == null) {
            return false;
        }
        if (!str2.endsWith("/")) {
            str2 = str2 + "/";
        }
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            File file2 = new File(str2);
            if (!file2.exists()) {
                file2.mkdirs();
            }
            for (int i = 0; i < listFiles.length; i++) {
                if (listFiles[i].isDirectory()) {
                    copyDir(listFiles[i].getPath() + "/", str2 + listFiles[i].getName() + "/", str3);
                } else if (listFiles[i].getPath() != null && !listFiles[i].getPath().contains(str3)) {
                    copyFile(listFiles[i].getPath(), str2 + listFiles[i].getName());
                }
            }
            return true;
        }
        copyFileToDir(str, str2);
        return true;
    }

    private static boolean copyFile(String str, String str2) {
        Log.d(TAG, "---->copyFile() src = " + str + ", dest = " + str2);
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            FileOutputStream fileOutputStream = new FileOutputStream(str2);
            byte[] bArr = new byte[1024];
            while (true) {
                int read = fileInputStream.read(bArr);
                if (read > 0) {
                    fileOutputStream.write(bArr, 0, read);
                } else {
                    fileInputStream.close();
                    fileOutputStream.close();
                    return true;
                }
            }
        } catch (Exception unused) {
            return false;
        }
    }

    public static boolean copyFileToDir(String str, String str2) {
        Log.d(TAG, "---->copyFileToDir() src = " + str + ", dest = " + str2);
        File file = new File(str2);
        if (!file.exists()) {
            file.mkdir();
        }
        String str3 = str2 + new File(str).getName();
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            FileOutputStream fileOutputStream = new FileOutputStream(str3);
            byte[] bArr = new byte[1024];
            while (true) {
                int read = fileInputStream.read(bArr);
                if (read > 0) {
                    fileOutputStream.write(bArr, 0, read);
                } else {
                    fileInputStream.close();
                    fileOutputStream.close();
                    return true;
                }
            }
        } catch (Exception unused) {
            return false;
        }
    }

    public static boolean moveFile(String str, String str2) {
        Log.d(TAG, "moveFile() src = " + str + ", dest = " + str2);
        if (!copyDir(str, str2, null)) {
            return false;
        }
        deleteFile(new File(str));
        return true;
    }

    public static void deleteFile(File file) {
        Log.d(TAG, "deleteFile() delFile = " + file);
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                deleteFile(file2);
            }
            return;
        }
        file.delete();
    }

    public static void deleteDir(String str) {
        Log.d(TAG, "deleteFile() srcDir = " + str);
        File file = new File(str);
        if (file.exists()) {
            deleteFile(file);
        }
    }
}

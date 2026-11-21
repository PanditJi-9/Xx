package cn.nubia.common.helper;

import android.graphics.Bitmap;
import android.util.LruCache;

/* loaded from: classes.dex */
public class ImageCache {
    private static final int KB = 1024;
    private LruCache<String, Bitmap> mCache;

    private static class ImageCacheHolder {
        public static final ImageCache INSTANCE = new ImageCache();

        private ImageCacheHolder() {
        }
    }

    public static ImageCache getInstance() {
        return ImageCacheHolder.INSTANCE;
    }

    private ImageCache() {
        this.mCache = new LruCache<String, Bitmap>(((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8) { // from class: cn.nubia.common.helper.ImageCache.1
            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.util.LruCache
            public int sizeOf(String str, Bitmap bitmap) {
                return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
            }
        };
    }

    public void put(String str, Bitmap bitmap) {
        if (str == null || bitmap == null) {
            return;
        }
        this.mCache.put(str, bitmap);
    }

    public void remove(String str) {
        if (str == null) {
            return;
        }
        this.mCache.remove(str);
    }

    public Bitmap get(String str) {
        return this.mCache.get(str);
    }

    public void clear() {
        this.mCache.evictAll();
    }
}

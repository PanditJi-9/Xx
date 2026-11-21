package cn.nubia.volley.toolbox;

import android.os.Handler;
import android.os.Looper;
import cn.nubia.volley.Cache;
import cn.nubia.volley.NetworkResponse;
import cn.nubia.volley.Request;
import cn.nubia.volley.Response;

/* loaded from: classes2.dex */
public class ClearCacheRequest extends Request<Object> {
    private final Cache mCache;
    private final Runnable mCallback;

    @Override // cn.nubia.volley.Request
    protected void deliverResponse(Object obj) {
    }

    @Override // cn.nubia.volley.Request
    protected Response<Object> parseNetworkResponse(NetworkResponse networkResponse) {
        return null;
    }

    public ClearCacheRequest(Cache cache, Runnable runnable) {
        super(0, null, null);
        this.mCache = cache;
        this.mCallback = runnable;
    }

    @Override // cn.nubia.volley.Request
    public boolean isCanceled() {
        this.mCache.clear();
        if (this.mCallback == null) {
            return true;
        }
        new Handler(Looper.getMainLooper()).postAtFrontOfQueue(this.mCallback);
        return true;
    }

    @Override // cn.nubia.volley.Request
    public Request.Priority getPriority() {
        return Request.Priority.IMMEDIATE;
    }
}

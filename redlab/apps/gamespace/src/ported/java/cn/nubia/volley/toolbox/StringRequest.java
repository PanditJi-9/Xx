package cn.nubia.volley.toolbox;

import cn.nubia.volley.NetworkResponse;
import cn.nubia.volley.Request;
import cn.nubia.volley.Response;
import java.io.UnsupportedEncodingException;

/* loaded from: classes2.dex */
public class StringRequest extends Request<String> {
    private Response.Listener<String> mListener;
    private final Object mLock;

    public StringRequest(int i, String str, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(i, str, errorListener);
        this.mLock = new Object();
        this.mListener = listener;
    }

    public StringRequest(String str, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        this(0, str, listener, errorListener);
    }

    @Override // cn.nubia.volley.Request
    public void cancel() {
        super.cancel();
        synchronized (this.mLock) {
            this.mListener = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // cn.nubia.volley.Request
    public void deliverResponse(String str) {
        Response.Listener<String> listener;
        synchronized (this.mLock) {
            listener = this.mListener;
        }
        if (listener != null) {
            listener.onResponse(str);
        }
    }

    @Override // cn.nubia.volley.Request
    protected Response<String> parseNetworkResponse(NetworkResponse networkResponse) {
        String str;
        try {
            str = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
        } catch (UnsupportedEncodingException unused) {
            str = new String(networkResponse.data);
        }
        return Response.success(str, HttpHeaderParser.parseCacheHeaders(networkResponse));
    }
}

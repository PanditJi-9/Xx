package cn.nubia.volley.toolbox;

import cn.nubia.volley.NetworkResponse;
import cn.nubia.volley.Request;
import cn.nubia.volley.Response;
import cn.nubia.volley.VolleyLog;
import java.io.UnsupportedEncodingException;

/* loaded from: classes2.dex */
public abstract class JsonRequest<T> extends Request<T> {
    protected static final String PROTOCOL_CHARSET = "utf-8";
    private static final String PROTOCOL_CONTENT_TYPE = String.format("application/json; charset=%s", PROTOCOL_CHARSET);
    private Response.Listener<T> mListener;
    private final Object mLock;
    private final String mRequestBody;

    @Override // cn.nubia.volley.Request
    protected abstract Response<T> parseNetworkResponse(NetworkResponse networkResponse);

    @Deprecated
    public JsonRequest(String str, String str2, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        this(-1, str, str2, listener, errorListener);
    }

    public JsonRequest(int i, String str, String str2, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(i, str, errorListener);
        this.mLock = new Object();
        this.mListener = listener;
        this.mRequestBody = str2;
    }

    @Override // cn.nubia.volley.Request
    public void cancel() {
        super.cancel();
        synchronized (this.mLock) {
            this.mListener = null;
        }
    }

    @Override // cn.nubia.volley.Request
    protected void deliverResponse(T t) {
        Response.Listener<T> listener;
        synchronized (this.mLock) {
            listener = this.mListener;
        }
        if (listener != null) {
            listener.onResponse(t);
        }
    }

    @Override // cn.nubia.volley.Request
    @Deprecated
    public String getPostBodyContentType() {
        return getBodyContentType();
    }

    @Override // cn.nubia.volley.Request
    @Deprecated
    public byte[] getPostBody() {
        return getBody();
    }

    @Override // cn.nubia.volley.Request
    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    @Override // cn.nubia.volley.Request
    public byte[] getBody() {
        try {
            String str = this.mRequestBody;
            if (str == null) {
                return null;
            }
            return str.getBytes(PROTOCOL_CHARSET);
        } catch (UnsupportedEncodingException unused) {
            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", this.mRequestBody, PROTOCOL_CHARSET);
            return null;
        }
    }
}

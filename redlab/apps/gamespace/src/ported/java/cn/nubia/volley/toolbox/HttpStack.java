package cn.nubia.volley.toolbox;

import cn.nubia.volley.AuthFailureError;
import cn.nubia.volley.Request;
import java.io.IOException;
import java.util.Map;

@Deprecated
/* loaded from: classes2.dex */
public interface HttpStack {
    org.apache.http.HttpResponse performRequest(Request<?> request, Map<String, String> map) throws IOException, AuthFailureError;
}

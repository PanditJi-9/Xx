package cn.nubia.volley.toolbox;

import cn.nubia.volley.Cache;

/* loaded from: classes2.dex */
public class NoCache implements Cache {
    @Override // cn.nubia.volley.Cache
    public void clear() {
    }

    @Override // cn.nubia.volley.Cache
    public Cache.Entry get(String str) {
        return null;
    }

    @Override // cn.nubia.volley.Cache
    public void initialize() {
    }

    @Override // cn.nubia.volley.Cache
    public void invalidate(String str, boolean z) {
    }

    @Override // cn.nubia.volley.Cache
    public void put(String str, Cache.Entry entry) {
    }

    @Override // cn.nubia.volley.Cache
    public void remove(String str) {
    }
}

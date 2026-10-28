/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package de.robv.android.xposed;

import java.lang.reflect.Member;

/** Stub: compile-only. The real class is supplied by the Xposed framework (LSPosed) at runtime. */
public abstract class XC_MethodHook extends XCallback {
    public XC_MethodHook() {
        throw new UnsupportedOperationException("Stub!");
    }

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        throw new UnsupportedOperationException("Stub!");
    }

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        throw new UnsupportedOperationException("Stub!");
    }

    public static class MethodHookParam extends XCallback.Param {
        public Member method;
        public Object thisObject;
        public Object[] args;

        public Object getResult() {
            throw new UnsupportedOperationException("Stub!");
        }

        public void setResult(Object result) {
            throw new UnsupportedOperationException("Stub!");
        }
    }

    public class Unhook {
    }
}

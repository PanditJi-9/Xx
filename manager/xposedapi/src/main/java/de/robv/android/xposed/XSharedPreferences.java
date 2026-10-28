/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package de.robv.android.xposed;

/**
 * Stub: compile-only. The real class (which also implements android.content.SharedPreferences) is
 * supplied by the Xposed framework (LSPosed) at runtime; only the members the IMEI hook calls are declared.
 */
public final class XSharedPreferences {
    public XSharedPreferences(String packageName, String prefFileName) {
        throw new UnsupportedOperationException("Stub!");
    }

    public synchronized void reload() {
        throw new UnsupportedOperationException("Stub!");
    }

    public synchronized boolean hasFileChanged() {
        throw new UnsupportedOperationException("Stub!");
    }

    public String getString(String key, String defValue) {
        throw new UnsupportedOperationException("Stub!");
    }
}

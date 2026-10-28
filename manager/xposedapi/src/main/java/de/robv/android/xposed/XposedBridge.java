/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package de.robv.android.xposed;

/** Stub: compile-only. The real class is supplied by the Xposed framework (LSPosed) at runtime. */
public final class XposedBridge {
    private XposedBridge() {
    }

    public static synchronized void log(String text) {
        throw new UnsupportedOperationException("Stub!");
    }

    public static synchronized void log(Throwable t) {
        throw new UnsupportedOperationException("Stub!");
    }
}

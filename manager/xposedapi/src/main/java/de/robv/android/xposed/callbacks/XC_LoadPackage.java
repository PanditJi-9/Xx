/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package de.robv.android.xposed.callbacks;

import de.robv.android.xposed.XCallback;

/** Stub: compile-only. The real class is supplied by the Xposed framework (LSPosed) at runtime. */
public abstract class XC_LoadPackage extends XCallback {
    public static final class LoadPackageParam extends XCallback.Param {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public boolean isFirstApplication;
    }
}

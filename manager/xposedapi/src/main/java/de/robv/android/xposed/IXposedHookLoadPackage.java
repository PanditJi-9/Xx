/*
 * Copyright (C) 2026 Nader Magdy. All rights reserved.
 * Proprietary and confidential — not licensed for use, copying, or distribution
 * without prior written permission from the copyright holder.
 */
package de.robv.android.xposed;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Stub: compile-only. The real class is supplied by the Xposed framework (LSPosed) at runtime. */
public interface IXposedHookLoadPackage extends IXposedMod {
    void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
}

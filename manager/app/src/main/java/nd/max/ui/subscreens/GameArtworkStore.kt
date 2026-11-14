/* Copyright (C) 2026 Nader Magdy. All rights reserved. */
package nd.max.ui.subscreens

import android.content.Context
import android.net.Uri

/** Persists per-game artwork selections without changing the installed game's package or data. */
internal object GameArtworkStore {
    private const val PREFS = "gamespace_artwork_v1"

    fun loadAll(context: Context): Map<String, String> = runCatching {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
            .mapNotNull { (key, value) -> (value as? String)?.let { key to it } }
            .toMap()
    }.getOrDefault(emptyMap())

    fun save(context: Context, packageName: String, uri: Uri): Boolean = runCatching {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(packageName, uri.toString()).commit()
    }.getOrDefault(false)

    fun remove(context: Context, packageName: String): Boolean = runCatching {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(packageName).commit()
    }.getOrDefault(false)
}

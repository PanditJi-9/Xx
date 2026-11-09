/*
 * Copyright (C) 2026-2027 Zexshia
 * Licensed under the Apache License, Version 2.0 (see LICENSE).
 */

package nd.max.ui.subscreens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nd.max.core.platform.GameSessionStore
import nd.max.core.platform.StoredGameSession

/**
 * سجلّ الجلسات المحفوظة. القراءة على IO لأن الملف يُقرأ عند فتح الشاشة، والكتابة
 * (حذف/مسح) تعيد القراءة بعده — فلا تعرض الواجهة قائمةً سبقت فعلها.
 */
class GameSessionsViewModel : ViewModel() {

    private val mutable = MutableStateFlow<List<StoredGameSession>>(emptyList())
    val sessions = mutable.asStateFlow()

    private val loadingState = MutableStateFlow(true)
    val loading = loadingState.asStateFlow()

    fun refresh(context: Context) {
        val app = context.applicationContext
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { GameSessionStore.load(app) }
            mutable.value = stored
            loadingState.value = false
        }
    }

    fun delete(context: Context, id: String) {
        val app = context.applicationContext
        viewModelScope.launch {
            withContext(Dispatchers.IO) { GameSessionStore.delete(app, id) }
            mutable.value = withContext(Dispatchers.IO) { GameSessionStore.load(app) }
        }
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        viewModelScope.launch {
            withContext(Dispatchers.IO) { GameSessionStore.clear(app) }
            mutable.value = emptyList()
        }
    }
}

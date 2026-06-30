package com.gobe.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.System
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val rows: List<Pair<System, List<Game>>> = emptyList(),
    val loading: Boolean = true,
)

class HomeViewModel(private val repo: LibraryRepository, defaultPath: String) : ViewModel() {
    // True while a scan is in flight. Drives the "loading" UI so a fresh launch shows
    // "Escaneando…" instead of the empty state until the first scan completes.
    private val scanning = MutableStateFlow(true)

    val state: StateFlow<HomeState> = combine(repo.observeGames(), scanning) { games, isScanning ->
        val rows = System.entries
            .map { sys -> sys to games.filter { it.system == sys } }
            .filter { it.second.isNotEmpty() }
        // Only show the loading state when we have nothing to display yet; once games
        // exist we render rows even if a later rescan is still running.
        HomeState(rows = rows, loading = isScanning && rows.isEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    init {
        viewModelScope.launch {
            scanning.value = true
            repo.ensureDefaultFolder(defaultPath)
            repo.rescan()
            scanning.value = false
        }
    }
}

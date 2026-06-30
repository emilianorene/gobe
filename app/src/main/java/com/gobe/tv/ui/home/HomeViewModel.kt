package com.gobe.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.System
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeState(
    val rows: List<Pair<System, List<Game>>> = emptyList(),
    val loading: Boolean = true,
)

class HomeViewModel(private val repo: LibraryRepository, defaultPath: String) : ViewModel() {
    val state: StateFlow<HomeState> = repo.observeGames()
        .map { games ->
            val rows = System.entries
                .map { sys -> sys to games.filter { it.system == sys } }
                .filter { it.second.isNotEmpty() }
            HomeState(rows = rows, loading = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    init {
        viewModelScope.launch {
            repo.ensureDefaultFolder(defaultPath)
            repo.rescan()
        }
    }
}

package com.gobe.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.Game
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val continuePlaying: List<Game> = emptyList(),
    val loading: Boolean = true,
)

class HomeViewModel(private val repo: LibraryRepository, defaultPath: String) : ViewModel() {
    private val scanning = MutableStateFlow(true)

    val state: StateFlow<HomeState> =
        combine(repo.observeContinuePlaying(), scanning) { cont, scan ->
            HomeState(continuePlaying = cont, loading = scan && cont.isEmpty())
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

package com.gobe.tv.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.System
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeState(
    val continuePlaying: List<Game> = emptyList(),
    val games: List<Game> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: LibraryRepository, defaultPath: String) : ViewModel() {
    private val scanning = MutableStateFlow(true)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _selectedSystem = MutableStateFlow<System?>(null)
    val selectedSystem: StateFlow<System?> = _selectedSystem
    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre

    fun setQuery(q: String) { _query.value = q }
    fun setSystem(s: System?) { _selectedSystem.value = s }
    fun setGenre(g: String?) { _selectedGenre.value = g }

    val genres: StateFlow<List<String>> =
        repo.genres().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filtered: Flow<List<Game>> =
        combine(_query, _selectedSystem, _selectedGenre) { q, s, g -> Triple(q, s, g) }
            .flatMapLatest { (q, s, g) -> repo.searchGames(q, s, g) }

    val state: StateFlow<HomeState> =
        combine(repo.observeContinuePlaying(), filtered, scanning) { cont, games, scan ->
            HomeState(continuePlaying = cont, games = games, loading = scan && games.isEmpty() && cont.isEmpty())
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

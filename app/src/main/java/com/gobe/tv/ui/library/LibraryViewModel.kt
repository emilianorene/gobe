package com.gobe.tv.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.Game
import com.gobe.tv.domain.SortMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(private val repo: LibraryRepository, section: LibrarySection) : ViewModel() {
    private val base = sectionFilter(section)

    private val _genre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _genre
    private val _sort = MutableStateFlow(SortMode.RECOMMENDED)
    val sortMode: StateFlow<SortMode> = _sort

    fun setGenre(g: String?) { _genre.value = g }
    fun cycleSort() { _sort.value = SortMode.entries[(_sort.value.ordinal + 1) % SortMode.entries.size] }

    // Only genre + sort are reactive; the base filter is fixed by the section.
    val games: StateFlow<List<Game>> =
        combine(_genre, _sort) { g, s -> g to s }
            .flatMapLatest { (g, s) ->
                repo.searchGames(base.query, base.system, g, base.recommendedOnly, base.favoritesOnly, s)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Genres present in THIS section (derived from the section's games, ignoring the genre chip).
    val genres: StateFlow<List<String>> =
        repo.searchGames(base.query, base.system, null, base.recommendedOnly, base.favoritesOnly, SortMode.TITLE)
            .map { list -> list.mapNotNull { it.genre }.filter { it.isNotBlank() }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

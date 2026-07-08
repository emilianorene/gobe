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

    private val _filter = MutableStateFlow(CollectionFilter.ALL)
    val collectionFilter: StateFlow<CollectionFilter> = _filter

    fun setGenre(g: String?) { _genre.value = g }
    fun cycleSort() { _sort.value = SortMode.entries[(_sort.value.ordinal + 1) % SortMode.entries.size] }
    fun setFilter(f: CollectionFilter) { _filter.value = f }

    // Genre + sort + collection filter are reactive; system/query stay fixed by the section.
    val games: StateFlow<List<Game>> =
        combine(_genre, _sort, _filter) { g, s, f -> Triple(g, s, f) }
            .flatMapLatest { (g, s, f) ->
                val flags = collectionFlags(f)
                repo.searchGames(
                    base.query, base.system, g,
                    base.recommendedOnly || flags.recommendedOnly,
                    base.favoritesOnly || flags.favoritesOnly, s,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Genres present in THIS section (derived from the section's games, ignoring the genre chip).
    val genres: StateFlow<List<String>> =
        repo.searchGames(base.query, base.system, null, base.recommendedOnly, base.favoritesOnly, SortMode.TITLE)
            .map { list -> list.mapNotNull { it.genre }.filter { it.isNotBlank() }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

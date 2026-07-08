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
    val consoles: List<ConsoleEntry> = emptyList(),
    val focusedSystem: System? = null,
    val continueForFocused: List<Game> = emptyList(),
    val loading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: LibraryRepository, defaultPath: String) : ViewModel() {
    private val scanning = MutableStateFlow(true)

    /** User-chosen focused console; null means "follow defaultFocus". */
    private val focusOverride = MutableStateFlow<System?>(null)

    private val visible: StateFlow<List<ConsoleEntry>> =
        repo.observeCountsBySystem()
            .map { counts -> visibleConsoles(counts) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Effective focus = override if still visible, else the first visible console. */
    private val focused: StateFlow<System?> =
        combine(visible, focusOverride) { v, override ->
            val stillValid = override != null && v.any { it.system == override }
            if (stillValid) override else defaultFocus(v)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val continueForFocused: StateFlow<List<Game>> =
        focused.flatMapLatest { s ->
            if (s == null) flowOf(emptyList())
            else flow { emit(emptyList()); emitAll(repo.observeContinuePlayingBySystem(s)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<HomeState> =
        combine(visible, focused, continueForFocused, scanning) { v, f, cont, scan ->
            HomeState(consoles = v, focusedSystem = f, continueForFocused = cont, loading = scan && v.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    /** Move the focused console left/right (clamped, no wrap). */
    fun move(delta: Int) {
        focusOverride.value = moveFocus(visible.value, focused.value, delta)
    }

    init {
        viewModelScope.launch {
            scanning.value = true
            repo.ensureDefaultFolder(defaultPath)
            repo.rescan()
            scanning.value = false
        }
    }
}

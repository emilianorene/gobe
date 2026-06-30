package com.gobe.tv.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gobe.tv.data.LibraryRepository
import com.gobe.tv.domain.RomFolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoldersViewModel(private val repo: LibraryRepository) : ViewModel() {
    val folders = repo.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun add(path: String) = viewModelScope.launch { repo.addFolder(path); repo.rescan() }
    fun remove(folder: RomFolder) = viewModelScope.launch { repo.removeFolder(folder.id); repo.rescan() }
    fun toggle(folder: RomFolder) = viewModelScope.launch {
        repo.setFolderEnabled(folder.id, !folder.enabled); repo.rescan()
    }
}

package com.photoviewer.samba.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.photoviewer.samba.data.model.SambaConfig
import com.photoviewer.samba.data.model.SambaItem
import com.photoviewer.samba.data.repository.SambaRepository
import com.photoviewer.samba.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class BrowseState {
    object Idle : BrowseState()
    object Loading : BrowseState()
    data class Success(val items: List<SambaItem>, val path: String) : BrowseState()
    data class Error(val message: String) : BrowseState()
}

data class ViewerState(
    val images: List<SambaItem>,
    val currentIndex: Int
) {
    val current get() = images[currentIndex]
    val hasPrev  get() = currentIndex > 0
    val hasNext  get() = currentIndex < images.lastIndex
}

class MainViewModel(
    private val settingsRepo: SettingsRepository,
    private val sambaRepo: SambaRepository
) : ViewModel() {

    val config: StateFlow<SambaConfig> = settingsRepo.sambaConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SambaConfig())

    private val _navStack    = MutableStateFlow<List<String>>(emptyList())
    val navStack: StateFlow<List<String>> = _navStack.asStateFlow()

    private val _browseState = MutableStateFlow<BrowseState>(BrowseState.Idle)
    val browseState: StateFlow<BrowseState> = _browseState.asStateFlow()

    private val _viewerState = MutableStateFlow<ViewerState?>(null)
    val viewerState: StateFlow<ViewerState?> = _viewerState.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    fun openSettings()  { _showSettings.value = true }
    fun closeSettings() { _showSettings.value = false }

    fun saveConfig(config: SambaConfig) {
        viewModelScope.launch {
            settingsRepo.saveConfig(config)
            _showSettings.value = false
            _navStack.value = emptyList()
            _browseState.value = BrowseState.Idle
        }
    }

    fun loadRoot() {
        _navStack.value = emptyList()
        loadDirectory("")
    }

    fun openDirectory(item: SambaItem) {
        _navStack.value = _navStack.value + item.path
        loadDirectory(item.path)
    }

    fun navigateUp(): Boolean {
        val stack = _navStack.value
        if (stack.isEmpty()) return false
        val newStack = stack.dropLast(1)
        _navStack.value = newStack
        loadDirectory(newStack.lastOrNull() ?: "")
        return true
    }

    private fun loadDirectory(path: String) {
        viewModelScope.launch {
            _browseState.value = BrowseState.Loading
            val cfg = config.value
            if (!cfg.isValid()) {
                _browseState.value = BrowseState.Error("Configuration réseau incomplète.\nVeuillez renseigner les paramètres.")
                return@launch
            }
            sambaRepo.listDirectory(cfg, path)
                .onSuccess { _browseState.value = BrowseState.Success(it, path) }
                .onFailure { _browseState.value = BrowseState.Error(it.message ?: "Erreur de connexion") }
        }
    }

    fun refresh() = loadDirectory(_navStack.value.lastOrNull() ?: "")

    fun openViewer(images: List<SambaItem>, startIndex: Int) {
        _viewerState.value = ViewerState(images, startIndex)
    }

    fun closeViewer() { _viewerState.value = null }

    fun viewerNext() {
        _viewerState.value?.let { if (it.hasNext) _viewerState.value = it.copy(currentIndex = it.currentIndex + 1) }
    }

    fun viewerPrev() {
        _viewerState.value?.let { if (it.hasPrev) _viewerState.value = it.copy(currentIndex = it.currentIndex - 1) }
    }
}

class MainViewModelFactory(
    private val settingsRepo: SettingsRepository,
    private val sambaRepo: SambaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) =
        MainViewModel(settingsRepo, sambaRepo) as T
}

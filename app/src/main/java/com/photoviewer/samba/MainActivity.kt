package com.photoviewer.samba

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import com.photoviewer.samba.di.AppContainer
import com.photoviewer.samba.ui.MainViewModel
import com.photoviewer.samba.ui.MainViewModelFactory
import com.photoviewer.samba.ui.screens.BrowseScreen
import com.photoviewer.samba.ui.screens.SettingsScreen
import com.photoviewer.samba.ui.screens.ViewerScreen
import com.photoviewer.samba.ui.theme.SambaPhotoViewerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(AppContainer.settingsRepository, AppContainer.sambaRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    viewModel.viewerState.value != null -> viewModel.closeViewer()
                    viewModel.showSettings.value        -> viewModel.closeSettings()
                    !viewModel.navigateUp()             -> finish()
                }
            }
        })

        setContent {
            SambaPhotoViewerTheme {
                val config       by viewModel.config.collectAsState()
                val browseState  by viewModel.browseState.collectAsState()
                val navStack     by viewModel.navStack.collectAsState()
                val viewerState  by viewModel.viewerState.collectAsState()
                val showSettings by viewModel.showSettings.collectAsState()

                when {
                    viewerState != null -> ViewerScreen(
                        state = viewerState!!, config = config,
                        onClose = viewModel::closeViewer, onNext = viewModel::viewerNext, onPrev = viewModel::viewerPrev
                    )
                    showSettings -> SettingsScreen(
                        currentConfig = config, onSave = viewModel::saveConfig, onClose = viewModel::closeSettings
                    )
                    else -> BrowseScreen(
                        browseState = browseState, config = config, navStack = navStack,
                        onOpenDirectory = viewModel::openDirectory, onOpenPhoto = viewModel::openViewer,
                        onNavigateUp = { viewModel.navigateUp() }, onRefresh = viewModel::refresh,
                        onOpenSettings = viewModel::openSettings, onLoadRoot = viewModel::loadRoot
                    )
                }
            }
        }
    }
}

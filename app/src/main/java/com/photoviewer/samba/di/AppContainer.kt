package com.photoviewer.samba.di

import android.app.Application
import android.content.Context
import coil.ImageLoader
import com.photoviewer.samba.data.repository.SambaRepository
import com.photoviewer.samba.data.repository.SettingsRepository
import com.photoviewer.samba.data.repository.buildSmbImageLoader

object AppContainer {
    private lateinit var appContext: Context

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val sambaRepository: SambaRepository by lazy { SambaRepository() }
    val smbImageLoader: ImageLoader by lazy { buildSmbImageLoader(appContext, sambaRepository) }

    fun init(app: Application) { appContext = app.applicationContext }
}

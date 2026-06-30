package com.photoviewer.samba

import android.app.Application
import com.photoviewer.samba.di.AppContainer

class SambaPhotoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.init(this)
    }
}

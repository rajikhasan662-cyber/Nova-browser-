package com.example

import android.app.Application
import android.util.Log
import com.example.adblock.AdBlockFilterEngine
import com.example.data.db.NovaDatabase
import com.example.extensions.ExtensionManager
import com.example.security.SecureVaultManager
import com.example.settings.BrowserPreferences

class NovaApplication : Application() {

    lateinit var database: NovaDatabase
        private set

    lateinit var adBlockEngine: AdBlockFilterEngine
        private set

    lateinit var extensionManager: ExtensionManager
        private set

    lateinit var secureVaultManager: SecureVaultManager
        private set

    lateinit var browserPreferences: BrowserPreferences
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            database = NovaDatabase.getInstance(this)
            adBlockEngine = AdBlockFilterEngine()
            extensionManager = ExtensionManager(this)
            secureVaultManager = SecureVaultManager()
            browserPreferences = BrowserPreferences(this)
        } catch (e: Throwable) {
            Log.e("NovaApplication", "Error during app initialization: ${e.message}", e)
        }
    }

    companion object {
        lateinit var instance: NovaApplication
            private set
    }
}

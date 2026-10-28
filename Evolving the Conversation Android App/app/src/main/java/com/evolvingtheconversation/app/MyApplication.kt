package com.evolvingtheconversation.app

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        Firebase.appCheck.installAppCheckProviderFactory(factory)

        // Turn on OneSignal (the "mailman" that delivers push notifications)
        OneSignal.initWithContext(this, "3c1a2fd9-5f38-4022-9fc4-cafa4d3d8ce9")

        // NEW: requestPermission needs to run inside a coroutine —
        // this line creates a small "task runner" and asks inside it.
        CoroutineScope(Dispatchers.Main).launch {
            OneSignal.Notifications.requestPermission(true)
        }
    }
}
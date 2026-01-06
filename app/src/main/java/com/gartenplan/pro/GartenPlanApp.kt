package com.gartenplan.pro

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for GartenPlan Pro
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 */
@HiltAndroidApp
class GartenPlanApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-level components here
    }
}

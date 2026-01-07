package com.gartenplan.pro

import android.app.Application
import com.gartenplan.pro.data.local.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for GartenPlan Pro
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection
 */
@HiltAndroidApp
class GartenPlanApp : Application() {

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Seed database with plant data on first launch
        applicationScope.launch {
            databaseSeeder.seedIfEmpty()
        }
    }
}
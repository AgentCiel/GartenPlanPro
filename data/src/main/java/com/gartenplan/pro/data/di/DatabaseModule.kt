package com.gartenplan.pro.data.di

import android.content.Context
import androidx.room.Room
import com.gartenplan.pro.data.local.GartenPlanDatabase
import com.gartenplan.pro.data.local.dao.CompostDao
import com.gartenplan.pro.data.local.dao.GardenDao
import com.gartenplan.pro.data.local.dao.PlantDao
import com.gartenplan.pro.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): GartenPlanDatabase {
        return Room.databaseBuilder(
            context,
            GartenPlanDatabase::class.java,
            GartenPlanDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production!
            .build()
    }

    @Provides
    @Singleton
    fun providePlantDao(database: GartenPlanDatabase): PlantDao {
        return database.plantDao()
    }

    @Provides
    @Singleton
    fun provideGardenDao(database: GartenPlanDatabase): GardenDao {
        return database.gardenDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: GartenPlanDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideCompostDao(database: GartenPlanDatabase): CompostDao {
        return database.compostDao()
    }
}

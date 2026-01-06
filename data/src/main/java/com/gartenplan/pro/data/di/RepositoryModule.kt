package com.gartenplan.pro.data.di

import com.gartenplan.pro.data.repository.CompostRepositoryImpl
import com.gartenplan.pro.data.repository.GardenRepositoryImpl
import com.gartenplan.pro.data.repository.PlantRepositoryImpl
import com.gartenplan.pro.data.repository.TaskRepositoryImpl
import com.gartenplan.pro.domain.repository.CompostRepository
import com.gartenplan.pro.domain.repository.GardenRepository
import com.gartenplan.pro.domain.repository.PlantRepository
import com.gartenplan.pro.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository implementations to interfaces
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlantRepository(
        plantRepositoryImpl: PlantRepositoryImpl
    ): PlantRepository

    @Binds
    @Singleton
    abstract fun bindGardenRepository(
        gardenRepositoryImpl: GardenRepositoryImpl
    ): GardenRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindCompostRepository(
        compostRepositoryImpl: CompostRepositoryImpl
    ): CompostRepository
}

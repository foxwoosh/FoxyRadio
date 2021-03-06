package com.foxwoosh.radio.di.modules

import com.foxwoosh.radio.domain.interactors.settings.ISettingsInteractor
import com.foxwoosh.radio.domain.interactors.settings.SettingsInteractor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ServiceComponent::class, ViewModelComponent::class)
abstract class InteractorsModule {

    @Binds
    abstract fun provideSettingsInteractor(
        settingsInteractor: SettingsInteractor
    ): ISettingsInteractor
}
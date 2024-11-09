package com.github.antonkonyshev.tryst.di

import com.github.antonkonyshev.tryst.data.GeolocationServiceImpl
import com.github.antonkonyshev.tryst.domain.GeolocationService
import com.github.antonkonyshev.tryst.presentation.map.MapViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val geolocationBindingModule = module {
    single<GeolocationService> { GeolocationServiceImpl(androidContext()) }
}
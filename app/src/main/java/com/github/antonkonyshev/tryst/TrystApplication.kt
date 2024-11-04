package com.github.antonkonyshev.tryst.data

import android.app.Application
import com.github.antonkonyshev.tryst.BuildConfig.MAPKIT_API_KEY
import com.github.antonkonyshev.tryst.di.geolocationBindingModule
import com.github.antonkonyshev.tryst.di.networkModule
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class TrystApplication : Application(), KoinComponent {

    override fun onCreate() {
        instance = this
        super.onCreate()

        startKoin {
            androidContext(this@TrystApplication)
            modules(geolocationBindingModule, networkModule)
        }

        MapKitFactory.setApiKey(MAPKIT_API_KEY)
    }

    companion object {
        @Volatile
        lateinit var instance: TrystApplication
            private set

        val _currentLocation: MutableStateFlow<Point> = MutableStateFlow(
            Point(58.58, 49.62)
        )
        val currentLocation = _currentLocation.asStateFlow()

        const val geolocationWorkerName = "TrystGeolocationWorker"
    }
}
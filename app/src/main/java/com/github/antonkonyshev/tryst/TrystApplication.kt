package com.github.antonkonyshev.tryst.data

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import com.github.antonkonyshev.tryst.BuildConfig.MAPKIT_API_KEY
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TrystApplication : Application() {

    override fun onCreate() {
        instance = this
        super.onCreate()
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
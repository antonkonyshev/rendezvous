package com.github.antonkonyshev.tryst.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.antonkonyshev.tryst.data.TrystApplication
import com.github.antonkonyshev.tryst.domain.GeolocationService
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MapViewModel() : ViewModel(), KoinComponent {
    val currentLocation = TrystApplication._currentLocation.asStateFlow()
    val users = TrystApplication._users.asStateFlow()

    val _zoom = MutableStateFlow<Float>(16f)
    val zoom = _zoom.asStateFlow()

    val _target = MutableStateFlow<Point?>(currentLocation.value)
    val target = _target.asStateFlow()

    val _changedPlacemark = MutableSharedFlow<String>()
    val changedPlacemark = _changedPlacemark.asSharedFlow()

    init {
        startGeolocationWorker()
    }

    fun startGeolocationWorker() {
        viewModelScope.launch(Dispatchers.IO) {
            get<GeolocationService>().startWorker()
        }
    }
}
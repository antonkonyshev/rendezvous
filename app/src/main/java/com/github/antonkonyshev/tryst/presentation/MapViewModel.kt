package com.github.antonkonyshev.tryst.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.antonkonyshev.tryst.domain.GeolocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MapViewModel() : ViewModel(), KoinComponent {
    init {
        startGeolocationWorker()
    }

    fun startGeolocationWorker() {
        viewModelScope.launch(Dispatchers.IO) {
            get<GeolocationService>().startWorker()
        }
    }

    override fun onCleared() {
        super.onCleared()
        get<GeolocationService>().stopWorker()
    }
}
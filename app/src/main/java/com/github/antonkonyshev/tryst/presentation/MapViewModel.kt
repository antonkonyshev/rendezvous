package com.github.antonkonyshev.tryst.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.antonkonyshev.tryst.data.GeolocationWorker
import com.github.antonkonyshev.tryst.data.TrystApplication
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    init {
        startGeolocationWorker()
    }

    fun startGeolocationWorker() {
        val workManager = WorkManager.getInstance(TrystApplication.instance.applicationContext)
        viewModelScope.launch {
            workManager
                .getWorkInfosForUniqueWorkFlow(TrystApplication.geolocationWorkerName)
                .collect { workInfos ->
                    if (workInfos?.find { !it.state.isFinished } == null) {
                        workManager.enqueueUniqueWork(
                            TrystApplication.geolocationWorkerName,
                            ExistingWorkPolicy.REPLACE,
                            OneTimeWorkRequestBuilder<GeolocationWorker>().build()
                        )
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val workManager = WorkManager.getInstance(TrystApplication.instance.applicationContext)
        workManager.cancelUniqueWork(TrystApplication.geolocationWorkerName)
    }
}
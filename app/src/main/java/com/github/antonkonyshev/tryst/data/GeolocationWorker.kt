package com.github.antonkonyshev.tryst.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.yandex.mapkit.geometry.Point
import java.util.concurrent.TimeUnit

class GeolocationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    private val locationClient = LocationServices.getFusedLocationProviderClient(appContext)

    override suspend fun doWork(): Result {
        if (ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueueUniqueWork(
            TrystApplication.geolocationWorkerName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<GeolocationWorker>()
                .setInitialDelay(15L, TimeUnit.SECONDS).build()
        )

        locationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token
        ).addOnSuccessListener { location ->
            TrystApplication._currentLocation.value = Point(location.latitude, location.longitude)
            Log.d(TAG, "Current location ${location.latitude} ${location.longitude}")
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "TrystGeolocationWorker"
    }
}
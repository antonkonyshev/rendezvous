package com.github.antonkonyshev.tryst.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.antonkonyshev.tryst.R
import com.github.antonkonyshev.tryst.domain.GeolocationDataOwner
import com.github.antonkonyshev.tryst.domain.GeolocationService
import com.github.antonkonyshev.tryst.domain.LocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

class GeolocationWorker(
    private val appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {
    private val locationClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val running = true
    private val locationRepository: LocationRepository by inject()

    override suspend fun doWork(): Result {
        if (ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        setForeground(createForegroundInfo())

        while (running) {
            val scope = CoroutineScope(currentCoroutineContext())

            try {
                locationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token
                ).addOnSuccessListener { location ->
                    if(location == null) {
                        return@addOnSuccessListener
                    }
                    TrystApplication._currentLocation.value =
                        Point(location.latitude, location.longitude)
                    Log.d(TAG, "Current location ${location.latitude} ${location.longitude}")

                    scope.launch(Dispatchers.IO) {
                        locationRepository.saveLocation(location)
                    }
                }
            } catch (err: Exception) {
                Log.d(TAG, "Error on current location update: ${err.toString()}")
            }

            try {
                scope.launch(Dispatchers.IO) {
                    TrystApplication._users.value = locationRepository.getUsers()
                }
            } catch (err: Exception) {
                Log.d(TAG, "Error on users locations update: ${err.toString()}")
            }

            delay(15000L)
        }
        return Result.success()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val cancelIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        val notification = NotificationCompat
            .Builder(applicationContext, TrystApplication.geolocationWorkerName)
            .setContentTitle(applicationContext.getString(R.string.sharing_geolocation_using_tryst))
            .setTicker(applicationContext.getString(R.string.sharing_geolocation_using_tryst))
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                applicationContext.getString(R.string.stop), cancelIntent
            )
            .build()
        return ForegroundInfo(0, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val channel = NotificationChannel(
            TrystApplication.geolocationWorkerName,
            applicationContext.getString(R.string.geolocation_service),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "TrystGeolocationWorker"
    }
}

class GeolocationServiceImpl(private val appContext: Context) : GeolocationService {
    override suspend fun startWorker() {
        val workManager = WorkManager.getInstance(appContext)
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

    override fun stopWorker() {
        val workManager = WorkManager.getInstance(TrystApplication.instance.applicationContext)
        workManager.cancelUniqueWork(TrystApplication.geolocationWorkerName)
    }
}
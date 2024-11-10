package com.github.antonkonyshev.tryst

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.location.Location
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.github.antonkonyshev.tryst.data.GeolocationWorker
import com.github.antonkonyshev.tryst.data.GistRepositoryImpl
import com.github.antonkonyshev.tryst.data.TrystApplication
import com.github.antonkonyshev.tryst.domain.GeolocationService
import com.github.antonkonyshev.tryst.domain.LocationRepository
import com.github.antonkonyshev.tryst.domain.User
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.mock.declare
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import java.util.Date

@RunWith(RobolectricTestRunner::class)
class GeolocationWorkerTest : KoinTest {
    private lateinit var context: Context
    private lateinit var worker: GeolocationWorker
    private lateinit var locationRepositoryMock: LocationRepository

    @Before
    fun setUp() = runBlocking {
        val workManagerConfig = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, workManagerConfig)
        locationRepositoryMock = Mockito.mock(GistRepositoryImpl::class.java)
        `when`(locationRepositoryMock.getUsers()).thenReturn(
            setOf(
                User("123", "First", 50.1, 40.2, Date().time),
                User("234", "Second", 51.1, 41.2, Date().time),
            )
        )
        declare {
            module {
                single<LocationRepository> { locationRepositoryMock }
            }
        }
        worker = TestListenableWorkerBuilder<GeolocationWorker>(context).build()
    }

    @After
    fun tearDown() = runBlocking {
        stopKoin()
    }

    @Test
    fun testCurrentLocation() = runBlocking {
        worker.updateCurrentLocation(Location("").apply {
            latitude = 50.1
            longitude = 40.2
        })
        assertThat(TrystApplication._currentLocation.value.latitude, `is`(50.1))
        assertThat(TrystApplication._currentLocation.value.longitude, `is`(40.2))
    }

    @Test
    fun testNotification() = runBlocking {
        worker.createNotification()
        worker.updateNotificationState()
        val notificationManager = context.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
        assertThat(notificationManager.activeNotifications.size, `is`(1))
        val notification = notificationManager.activeNotifications.first()
        assertThat(notification.id, `is`(worker.notificationId))
        assertTrue(notification.isOngoing)
        assertEquals(notificationManager.notificationChannels.size, 1)
        val channel = notificationManager.notificationChannels.first()
        assertEquals(channel.id, TrystApplication.geolocationWorkerName)
        worker.updateNotificationState()
        assertEquals(notificationManager.activeNotifications.size, 1)
        notificationManager.cancel(notification.id)
        assertEquals(notificationManager.activeNotifications.size, 0)
        worker.updateNotificationState()
        assertEquals(notificationManager.activeNotifications.size, 1)
    }

    @Test
    fun testDoWork() = runBlocking {
        worker.scheduleStop()
        worker.doWork()
        assertEquals(TrystApplication._users.value.size, 2)
        assertTrue(TrystApplication._users.value.any { it.uid == "123" })
        val notificationManager = context.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager
        assertEquals(notificationManager.activeNotifications.size, 0)
    }

    @Test
    fun testStartGeolocationService() = runBlocking {
        TODO()
        get<GeolocationService>().startWorker()
    }
}
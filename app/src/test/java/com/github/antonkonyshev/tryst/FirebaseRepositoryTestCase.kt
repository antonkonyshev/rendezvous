package com.github.antonkonyshev.tryst

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.github.antonkonyshev.tryst.data.FirebaseRepositoryImpl
import com.github.antonkonyshev.tryst.di.geolocationBindingModule
import com.github.antonkonyshev.tryst.di.networkModule
import com.github.antonkonyshev.tryst.domain.LocationRepository
import com.github.antonkonyshev.tryst.domain.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.mock.declare
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import java.util.Date
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class FirebaseRepositoryTestCase : KoinTest {
    private lateinit var authMock: FirebaseAuth
    private lateinit var storageMock: FirebaseFirestore
    private lateinit var currentUserMock: FirebaseUser
    private lateinit var collectionRefMock: CollectionReference
    private lateinit var context: Context

    private val user1 = User(
        "testing-test", "Testing Test", 10.0, 10.0, Date().time,
    )
    private val user2 = User(
        "another-test", "Another Test", 20.0, 20.0, Date().time,
    )
    private val user3 = User(
        "third-test", "Third Test", 30.0, 30.0, Date().time,
    )

    @Before
    fun setUp() = runBlocking {
        currentUserMock = Mockito.mock(FirebaseUser::class.java)
        authMock = Mockito.mock(FirebaseAuth::class.java)
        collectionRefMock = Mockito.mock(CollectionReference::class.java)
        storageMock = Mockito.mock(FirebaseFirestore::class.java)
        context = ApplicationProvider.getApplicationContext()

        doReturn("testing-test").`when`(currentUserMock).uid
        doReturn(currentUserMock).`when`(authMock).currentUser
        doReturn(collectionRefMock).`when`(storageMock).collection("location")

        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger()
                androidContext(context)
                modules(networkModule, geolocationBindingModule)
                allowOverride(true)
            }
        }
        declare {
            loadKoinModules(module {
                single<FirebaseAuth> { authMock }
                single<FirebaseFirestore> { storageMock }
                single<LocationRepository> { FirebaseRepositoryImpl(context) }
            })
        }
    }

    @After
    fun tearDown() = runBlocking {
        stopKoin()
    }

    @Test
    fun testSaveLocation() = runBlocking {
        val repository = FirebaseRepositoryImpl(context)
        verify(storageMock, times(0)).collection("location")
        val location = Location("").apply {
            latitude = 50.1
            longitude = 40.2
        }
        repository.saveLocation(location)
        verify(storageMock, times(1)).collection("location")
        assertEquals(50.1, location.latitude)
    }

    @Test
    fun testGetUsers() = runBlocking {
        val repository = FirebaseRepositoryImpl(context)
        verify(storageMock, times(0)).collection("location")
        val users = repository.getUsers()
        verify(storageMock, times(1)).collection("location")
        assertEquals(0, users.size)
    }
}
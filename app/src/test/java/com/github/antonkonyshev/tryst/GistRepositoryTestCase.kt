package com.github.antonkonyshev.tryst

import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.github.antonkonyshev.tryst.data.GistApiSchema
import com.github.antonkonyshev.tryst.data.GistRepositoryImpl
import com.github.antonkonyshev.tryst.data.TrystApplication
import com.github.antonkonyshev.tryst.di.geolocationBindingModule
import com.github.antonkonyshev.tryst.di.networkModule
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class GistRepositoryTestCase : KoinTest {
    private val server = MockWebServer()
    private val schema = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(server.url("").toString())
        .client(
            OkHttpClient.Builder().apply {
                addInterceptor { chain ->
                    var request = chain.request()
                    request = request.newBuilder().url(
                        request.url.newBuilder().port(server.port).build()
                    ).build()
                    return@addInterceptor chain.proceed(request)
                }
            }.build()
        ).build().create(GistApiSchema::class.java)
    private lateinit var context: Context
    private lateinit var client: GistRepositoryImpl

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger()
                androidContext(context)
                modules(networkModule, geolocationBindingModule)
            }
        }
        client = GistRepositoryImpl(schema, context)
    }

    @After
    fun tearDown() = runBlocking {
        stopKoin()
    }

    @Test
    fun testSaveLocation() = runBlocking {
        val location = Location("").apply {
            latitude = 50.1
            longitude = 40.2
        }

        server.enqueue(
            MockResponse().addHeader("Content-Type", "application/vnd.github+json")
                .setResponseCode(200)
        )
        client.saveLocation(location)

        val uid = context.getSharedPreferences(TrystApplication.geolocationWorkerName, 0)
            .getString("uid", "uid")
        val request = server.takeRequest()
        val payload = request.body.readUtf8()
        assertTrue(payload.contains("""\"latitude\":50.1""", ignoreCase = false))
        assertTrue(payload.contains("""\"longitude\":40.2""", ignoreCase = false))
        assertTrue(payload.contains(""""files"""", ignoreCase = false))
        assertTrue(payload.contains(""""content"""", ignoreCase = false))
        assertTrue(payload.contains("""\"name\":""", ignoreCase = false))
        assertTrue(payload.contains("""\"timestamp\":""", ignoreCase = false))
        assertTrue(payload.contains(uid!!, ignoreCase = false))
        assertEquals(request.getHeader("Content-Type"), "application/json; charset=utf-8")
    }

    @Test
    fun testSaveLocation_invalidResponse() = runBlocking {
        val location = Location("").apply {
            latitude = 51.1
            longitude = 42.2
        }

        server.enqueue(
            MockResponse().setResponseCode(403)
        )
        client.saveLocation(location)

        val payload = server.takeRequest().body.readUtf8()
        assertTrue(payload.contains("""\"latitude\":51.1""", ignoreCase = false))
        assertTrue(payload.contains("""\"longitude\":42.2""", ignoreCase = false))
    }

    @Test
    fun testGetUsers() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/vnd.github+json")
                .setBody(
                    """{"url":"https://api.github.com/gists/blablabla","id":"321","files":""" +
                            """{"123":{"filename":"123","content":""" +
                            """"{\"name\":\"First\",\"latitude\":50.1,""" +
                            """\"longitude\":40.2,\"timestamp\":${Date().time}}"""" +
                            """},"234":{"filename":"234","content":""" +
                            """"{\"name\":\"Second\",\"latitude\":51.1,\"longitude\":41.2,""" +
                            """\"timestamp\":${Date().time}}"}},"forks":[]}"""
                )
        )

        val users = client.getUsers()
        assertEquals(2, users.size)
        assertTrue(users.any { it.uid == "123" })
        assertTrue(users.any { it.uid == "234" })
        val first = users.find { it.uid == "123" }
        val second = users.find { it.uid == "234" }
        assertEquals("First", first!!.name)
        assertEquals("Second", second!!.name)
        assertEquals(50.1, first.latitude)
        assertEquals(40.2, first.longitude)
        assertEquals(51.1, second.latitude)
        assertEquals(41.2, second.longitude)
    }

    @Test
    fun testGetUsers_invalidResponseCode() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(403)
        )

        val users = client.getUsers()
        assertEquals(0, users.size)
    }

    @Test
    fun testGetUsers_invalidResponseBody() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("Something")
        )

        val users = client.getUsers()
        assertEquals(0, users.size)
    }
}
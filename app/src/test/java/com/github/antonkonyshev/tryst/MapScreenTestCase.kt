package com.github.antonkonyshev.tryst

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.antonkonyshev.tryst.data.GeolocationServiceImpl
import com.github.antonkonyshev.tryst.di.geolocationBindingModule
import com.github.antonkonyshev.tryst.di.networkModule
import com.github.antonkonyshev.tryst.domain.GeolocationService
import com.github.antonkonyshev.tryst.presentation.map.MapControls
import com.github.antonkonyshev.tryst.presentation.map.MapViewModel
import com.github.antonkonyshev.tryst.ui.theme.TrystTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
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
import org.mockito.kotlin.doNothing
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MapScreenTestCase : KoinTest {
    private lateinit var context: Context
    private lateinit var geolocationServiceMock: GeolocationService

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() = runBlocking {
        geolocationServiceMock = Mockito.mock(GeolocationServiceImpl::class.java)
        context = ApplicationProvider.getApplicationContext()
        doNothing().`when`(geolocationServiceMock).startWorker()
        doNothing().`when`(geolocationServiceMock).stopWorker()
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
                single<GeolocationService> { geolocationServiceMock }
            })
        }
    }

    @After
    fun tearDown() = runBlocking {
        stopKoin()
    }

    @Test
    fun testMapControls() = runBlocking {
        val viewModel = MapViewModel()
        with(composeRule) {
            setContent {
                TrystTheme {
                    MapControls(viewModel = viewModel)
                }
            }

            assertEquals(16f, viewModel.zoom.value)
            onNodeWithContentDescription("Zoom Out").performClick()
            assertEquals(15.4, Math.round(viewModel.zoom.value * 10.0) / 10.0)
            onNodeWithContentDescription("Zoom Out").performClick()
            assertEquals(14.8, Math.round(viewModel.zoom.value * 10.0) / 10.0)
            onNodeWithContentDescription("Zoom In").performClick()
            assertEquals(15.4, Math.round(viewModel.zoom.value * 10.0) / 10.0)
            onNodeWithContentDescription("Zoom In").performClick()
            assertEquals(16f, viewModel.zoom.value)
        }
    }
}
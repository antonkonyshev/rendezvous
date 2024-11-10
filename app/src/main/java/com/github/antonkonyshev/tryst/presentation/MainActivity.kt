package com.github.antonkonyshev.tryst.presentation

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.github.antonkonyshev.tryst.domain.GeolocationService
import com.github.antonkonyshev.tryst.presentation.navigation.TrystNavHost
import com.github.antonkonyshev.tryst.ui.theme.TrystTheme
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class MainActivity : AppCompatActivity() {
    private val _eventBus = MutableSharedFlow<UiEvent>()
    val eventBus = _eventBus.asSharedFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val notificationPermissionRequest = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (ActivityCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val locationPermissionRequest = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                if (
                    permissions.getOrDefault(
                        Manifest.permission.ACCESS_FINE_LOCATION, false
                    ) ||
                    permissions.getOrDefault(
                        Manifest.permission.ACCESS_COARSE_LOCATION, false
                    )
                ) {
                    lifecycleScope.launch {
                        get<GeolocationService>().startWorker()
                    }
                }
            }

            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        MapKitFactory.initialize(this)

        setContent {
            TrystTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    TrystNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    fun emitUiEvent(id: String, extras: String = "") {
        lifecycleScope.launch {
            _eventBus.emit(UiEvent(id, extras))
        }
    }

    fun emitUiEvent(uiEvent: UiEvent) {
        lifecycleScope.launch {
            _eventBus.emit(uiEvent)
        }
    }
}

data class UiEvent(val id: String, val extra: String = "")

fun Context.getActivity(): MainActivity? = when (this) {
    is MainActivity -> this
    is ContextWrapper -> baseContext.getActivity() as MainActivity
    else -> null
}

@Composable
fun <T> Flow<T>.collectAsEffect(
    context: CoroutineContext = EmptyCoroutineContext,
    block: (T) -> Unit
) {
    LaunchedEffect(key1 = Unit) {
        onEach(block).flowOn(context).launchIn(this)
    }
}
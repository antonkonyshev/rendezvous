package com.github.antonkonyshev.tryst.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.antonkonyshev.tryst.R
import com.github.antonkonyshev.tryst.data.TrystApplication
import com.github.antonkonyshev.tryst.ui.theme.TrystTheme
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
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
                    val viewModel: MapViewModel by viewModels()
                    viewModel.startGeolocationWorker()
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
                    MapScreen()
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
}

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val ctx = LocalContext.current
    val mapView = remember {
        val mapView = MapView(ctx)
        mapView.mapWindow.map.move(
            CameraPosition(
                TrystApplication.currentLocation.value, 16f,
                0.0F, 0.0F
            ), Animation(Animation.Type.SMOOTH, 1f), null
        )
        mapView.mapWindow.map.mapObjects.addPlacemark().apply {
            geometry = TrystApplication.currentLocation.value
            setIcon(ImageProvider.fromResource(ctx, R.mipmap.ic_launcher))
        }
        mapView
    }

    Box {
        AndroidView(
            factory = { mapView },
            onRelease = {
                mapView.onStop()
            },
        )
        LaunchedEffect(key1 = Unit) {
            var area = mapView.mapWindow.map.mapObjects.addCircle(
                Circle(
                    TrystApplication.currentLocation.value,
                    15f
                )
            )
            viewModel.viewModelScope.launch {
                TrystApplication.currentLocation.collect { location ->
                    mapView.mapWindow.map.mapObjects.remove(area)
                    area = mapView.mapWindow.map.mapObjects.addCircle(Circle(location, 25f))
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 0.dp, top = 0.dp, end = 15.dp, bottom = 30.dp)
        ) {
            FloatingActionButton(
                onClick = { /*TODO*/ },
            ) {
                Icon(imageVector = Icons.Outlined.KeyboardArrowUp, contentDescription = "Zoom Out")
            }

            FloatingActionButton(
                onClick = { /*TODO*/ },
                modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 0.dp)
            ) {
                Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = "Zoom In")
            }
        }
    }
}
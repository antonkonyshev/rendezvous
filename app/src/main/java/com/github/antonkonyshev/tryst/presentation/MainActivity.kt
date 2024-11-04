package com.github.antonkonyshev.tryst.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.antonkonyshev.tryst.ui.theme.TrystTheme
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.mapview.MapView
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
    Box {
        Map()

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 0.dp, top = 0.dp, end = 15.dp, bottom = 30.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel._zoom.value -= 0.6f },
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "Zoom Out"
                )
            }

            FloatingActionButton(
                onClick = { viewModel._zoom.value += 0.6f },
                modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Zoom In"
                )
            }

            Row(
                modifier = Modifier.padding(start = 0.dp, top = 24.dp, end = 0.dp, bottom = 0.dp)
            ) {
                viewModel.users.collectAsStateWithLifecycle().value.forEach { user ->
                    FloatingActionButton(
                        onClick = { /*TODO*/ },
                        modifier = Modifier.padding(12.dp, 0.dp, 0.dp, 0.dp)
                    ) {
                        Text(
                            text = if (user.name.length > 6) user.name.substring(0, 5).uppercase()
                            else user.name.uppercase()
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { viewModel._target.value = viewModel.currentLocation.value },
                    modifier = Modifier.padding(24.dp, 0.dp, 0.dp, 0.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = "Center")
                }
            }
        }
    }
}

@Composable
fun Map(
    viewModel: MapViewModel = viewModel(),
    zoom: Float = viewModel.zoom.collectAsStateWithLifecycle().value,
    target: Point = viewModel.target.collectAsStateWithLifecycle().value
) {
    val ctx = LocalContext.current
    val mapView = remember { MapView(ctx) }

    AndroidView(
        factory = { mapView },
        onRelease = {
            mapView.onStop()
        },
    )

    LaunchedEffect(key1 = zoom.toString()) {
        mapView.mapWindow.map.move(
            CameraPosition(
                mapView.mapWindow.map.cameraPosition.target, zoom, 0.0F, 0.0F
            ), Animation(Animation.Type.SMOOTH, 0.25f), null
        )
    }

    LaunchedEffect(key1 = target.latitude.toString(), key2 = target.longitude.toString()) {
        mapView.mapWindow.map.move(
            CameraPosition(
                target, mapView.mapWindow.map.cameraPosition.zoom,
                0.0F, 0.0F
            ), Animation(Animation.Type.SMOOTH, 0.25f), null
        )
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.viewModelScope.launch {
            var area: MapObject? = null
            viewModel.currentLocation.collect { location ->
                if (area != null) {
                    mapView.mapWindow.map.mapObjects.remove(area!!)
                }
                area = mapView.mapWindow.map.mapObjects.addCircle(Circle(location, 25f))
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.viewModelScope.launch {
            val userMarks = HashMap<String, MapObject>()
            Log.e("COMPOSABLE", "TEST")
            viewModel.users.collect { users ->
                users.forEach { user ->
                    val userMark = userMarks.get(user.uid)
                    if (userMark != null) {
                        mapView.mapWindow.map.mapObjects.remove(userMark)
                    }
                    userMarks[user.uid] = mapView.mapWindow.map.mapObjects.addCircle(
                        Circle(Point(user.latitude, user.longitude), 25f)
                    )
                }
            }
        }
    }
}
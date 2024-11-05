package com.github.antonkonyshev.tryst.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Remove
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
import com.github.antonkonyshev.tryst.R
import com.github.antonkonyshev.tryst.ui.theme.TrystTheme
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Circle
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.MapObject
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.min

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

        val ctx = LocalContext.current
        val pickMedia = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val srcBitmap = BitmapFactory.decodeStream(ctx.contentResolver.openInputStream(uri))
                val targetWidth = min(srcBitmap.width, srcBitmap.height)
                val resultBitmap = Bitmap.createBitmap(
                    targetWidth, targetWidth, Bitmap.Config.ARGB_8888
                ).apply {
                    Canvas(this).apply {
                        val paint = Paint().apply {
                            isAntiAlias = true
                        }
                        drawOval(
                            RectF(
                                Rect(
                                    10, 10, targetWidth - 10, targetWidth - 10
                                )
                            ), paint
                        )
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        drawBitmap(
                            srcBitmap,
                            ((targetWidth - srcBitmap.width) / 2).toFloat(),
                            ((targetWidth - srcBitmap.height) / 2).toFloat(),
                            paint
                        )
                        drawOval(RectF(
                            Rect(
                                5, 5, targetWidth - 5, targetWidth - 5
                            )
                        ), Paint().apply {
                            isAntiAlias = true
                            style = Paint.Style.STROKE
                            strokeWidth = 10f
                            color = Color.DKGRAY
                        })
                    }
                    srcBitmap.recycle()
                }

                File(ctx.filesDir, "own.jpg").apply {
                    outputStream().apply {
                        resultBitmap.compress(
                            Bitmap.CompressFormat.PNG, 90, this
                        )
                        flush()
                    }.close()
                    ctx.getSharedPreferences("avatars", 0).edit().putString("own", path).commit()
                }
            }
        }
        FloatingActionButton(
            onClick = {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(start = 0.dp, top = 40.dp, end = 15.dp, bottom = 0.dp)
        ) {
            Icon(imageVector = Icons.Outlined.Menu, contentDescription = "Menu")
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 0.dp, top = 0.dp, end = 15.dp, bottom = 30.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel._zoom.value -= 0.6f },
            ) {
                Icon(imageVector = Icons.Outlined.Remove, contentDescription = "Zoom Out")
            }

            FloatingActionButton(
                onClick = { viewModel._zoom.value += 0.6f },
                modifier = Modifier.padding(0.dp, 12.dp, 0.dp, 0.dp)
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Zoom In")
            }

            Row(
                modifier = Modifier.padding(start = 0.dp, top = 24.dp, end = 0.dp, bottom = 0.dp)
            ) {
                viewModel.users.collectAsStateWithLifecycle().value.forEach { user ->
                    FloatingActionButton(
                        onClick = {
                            viewModel._target.value = Point(user.latitude, user.longitude)
                        },
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
                    Icon(imageVector = Icons.Outlined.Adjust, contentDescription = "Center")
                }
            }
        }
    }
}

@Composable
fun Map(
    viewModel: MapViewModel = viewModel(),
    zoom: Float = viewModel.zoom.collectAsStateWithLifecycle().value,
    target: Point? = viewModel.target.collectAsStateWithLifecycle().value
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

    LaunchedEffect(key1 = target?.latitude.toString(), key2 = target?.longitude.toString()) {
        if (target != null) {
            mapView.mapWindow.map.move(
                CameraPosition(
                    target, mapView.mapWindow.map.cameraPosition.zoom,
                    0.0F, 0.0F
                ), Animation(Animation.Type.SMOOTH, 0.25f), null
            )
            viewModel._target.value = null
        }
    }

    LaunchedEffect(key1 = Unit) {
        mapView.mapWindow.map.move(
            CameraPosition(
                viewModel.currentLocation.value, zoom, 0.0f, 0.0f
            ), Animation(Animation.Type.SMOOTH, 0.25f), null
        )
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.viewModelScope.launch {
            var area: PlacemarkMapObject? = null
            viewModel.currentLocation.collect { location ->
                if (area == null) {
                    area = mapView.mapWindow.map.mapObjects.addPlacemark {
                        val avatarPath =
                            ctx.getSharedPreferences("avatars", 0).getString("own", null)
                        if (avatarPath == null) {
                            it.setIcon(
                                ImageProvider.fromResource(
                                    ctx,
                                    com.yandex.maps.mobile.R.drawable.search_layer_pin_selected_default
                                )
                            )
                        } else {
                            val avatarProvider = ImageProvider.fromFile(avatarPath)
                            val scale = avatarProvider.image.height / 150f
                            it.setIcon(
                                ImageProvider.fromFile(avatarPath),
                                IconStyle().setScale(1f / scale)
                            )
                        }
                        it.geometry = location
                        it.setText(
                            ctx.getString(R.string.you),
                            TextStyle().setPlacement(TextStyle.Placement.RIGHT)
                        )
                    }
                } else {
                    area!!.geometry = location
                }
//                if (area != null) {
//                    mapView.mapWindow.map.mapObjects.remove(area!!)
//                }
//                area = mapView.mapWindow.map.mapObjects.addCircle(Circle(location, 25f))
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
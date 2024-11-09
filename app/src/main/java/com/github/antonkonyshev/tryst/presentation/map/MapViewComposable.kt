package com.github.antonkonyshev.tryst.presentation.map

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.antonkonyshev.tryst.R
import com.github.antonkonyshev.tryst.data.TrystApplication
import com.github.antonkonyshev.tryst.domain.User
import com.yandex.mapkit.Animation
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import java.util.Date

@Composable
fun Map(
    viewModel: MapViewModel = viewModel(),
    zoom: Float = viewModel.zoom.collectAsStateWithLifecycle().value,
    target: Point? = viewModel.target.collectAsStateWithLifecycle().value
) {
    val ctx = LocalContext.current
    val mapView = remember { MapView(ctx) }
    var ownPlacemark: PlacemarkMapObject? = null
    val userPlacemarks = HashMap<String, PlacemarkMapObject>()

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
        mapView.invalidate()
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.currentLocation.collect { location ->
            ownPlacemark = updatePlacemark(ctx, mapView, ownPlacemark, location, null)
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.users.collect { users ->
            users.forEach { user ->
                userPlacemarks[user.uid] = updatePlacemark(
                    ctx,
                    mapView,
                    userPlacemarks.get(user.uid),
                    Point(user.latitude, user.longitude),
                    user
                )
            }
        }
    }

    LaunchedEffect(key1 = Unit) {
        viewModel.changedPlacemark.collect { uid ->
            if (uid == "own") {
                if (ownPlacemark != null) {
                    mapView.mapWindow.map.mapObjects.remove(ownPlacemark!!)
                    ownPlacemark = null
                    TrystApplication._currentLocation.value = Point(
                        viewModel.currentLocation.value.latitude,
                        viewModel.currentLocation.value.longitude
                    )
                } else {
                    val placemark = userPlacemarks.get(uid)
                    if (placemark != null) {
                        mapView.mapWindow.map.mapObjects.remove(placemark)
                        userPlacemarks.remove(uid)
                        TrystApplication._users.value = viewModel.users.value.toSet()
                    }
                }
            }
        }
    }
}

fun updatePlacemark(
    ctx: Context,
    mapView: MapView,
    existingPlacemark: PlacemarkMapObject?,
    location: Point,
    user: User?
): PlacemarkMapObject {
    return when (existingPlacemark) {
        null -> mapView.mapWindow.map.mapObjects.addPlacemark {
            val avatarPath =
                ctx.getSharedPreferences("avatars", 0).getString(user?.uid ?: "own", null)
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
            it.setTextStyle(TextStyle().setPlacement(TextStyle.Placement.RIGHT))
            it.setText(
                when (user?.name) {
                    null -> ctx.getSharedPreferences("avatars", 0).getString(
                        "name", ctx.getString(R.string.you)
                    )!!

                    else -> user.name
                }
            )
        }

        else -> existingPlacemark.apply { geometry = location }
    }.apply {
        if (user != null) {
            updatePlacemarkText(this, user)
        }
    }
}

fun updatePlacemarkText(placemark: PlacemarkMapObject, user: User) {
    val formatter = RelativeDateTimeFormatter.getInstance()
    val diff = (Date().time - user.timestamp) / 1000.0
    val timeAgo = when {
        diff > 86400 -> formatter.format(
            (diff / 86400.0).toInt().toDouble(),
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.DAYS
        )

        diff > 3600 -> formatter.format(
            (diff / 3600.0).toInt().toDouble(),
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.HOURS
        )

        diff > 60 -> formatter.format(
            (diff / 60.0).toInt().toDouble(),
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.MINUTES
        )

        else -> formatter.format(
            diff.toInt().toDouble(),
            RelativeDateTimeFormatter.Direction.LAST,
            RelativeDateTimeFormatter.RelativeUnit.SECONDS
        )
    }
    placemark.setText("${user.name}\n${timeAgo}")
}
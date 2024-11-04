package com.github.antonkonyshev.tryst.domain

import com.yandex.mapkit.geometry.Point
import kotlinx.coroutines.flow.MutableStateFlow

interface GeolocationDataOwner {
    val _currentLocation: MutableStateFlow<Point>
    val _users: MutableStateFlow<Set<User>>
}
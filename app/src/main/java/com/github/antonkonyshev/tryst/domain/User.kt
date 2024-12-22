package com.github.antonkonyshev.tryst.domain

class User(
    var uid: String = "",
    var name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = 0L,
    var group: String = "Guest",
)

package com.github.antonkonyshev.tryst.domain

import android.location.Location

interface LocationRepository {
    suspend fun getUsers(): List<String>
    suspend fun getUser(id: String): String?
    suspend fun saveLocation(location: Location)
}
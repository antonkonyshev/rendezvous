package com.github.antonkonyshev.tryst.domain

import android.location.Location

interface LocationRepository {
    suspend fun getUsers(): Set<User>
    suspend fun saveLocation(location: Location)
}
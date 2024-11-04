package com.github.antonkonyshev.tryst.domain

interface GeolocationService {
    suspend fun startWorker()
    fun stopWorker()
}
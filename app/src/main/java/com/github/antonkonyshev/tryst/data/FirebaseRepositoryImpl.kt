package com.github.antonkonyshev.tryst.data

import android.content.Context
import android.location.Location
import android.util.Log
import com.github.antonkonyshev.tryst.domain.LocationRepository
import com.github.antonkonyshev.tryst.domain.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import org.koin.core.component.KoinComponent
import java.util.Date

class FirebaseRepositoryImpl(private val appContext: Context) : LocationRepository, KoinComponent {
    private val auth = Firebase.auth
    private val storage = Firebase.firestore

    private suspend fun authenticate() {
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
            } catch (err: Exception) {
                Log.e(TAG, "Error on firebase authentication: ${err.toString()}")
            }
        }
    }

    override suspend fun getUsers(): Set<User> {
        authenticate()
        try {
            return storage.collection(LOCATIONS_COLLECTION).whereEqualTo(
                "group",
                appContext.getSharedPreferences("avatars", 0).getString("group", "Guest")!!
            ).get().await().map { userDocument ->
                if (userDocument.id == auth.currentUser?.uid) {
                    return@map null
                }
                try {
                    return@map UserMapper.mapDocumentToDomain(userDocument.id, userDocument)
                } catch (err: Exception) {
                    Log.e(
                        TAG, "Error on firestore document mapping to user: ${err.toString()}"
                    )
                    return@map null
                }
            }.filterNotNull().toSet()
        } catch (err: Exception) {
            Log.e(
                TAG, "Error on users locations reading from the firestore: ${err.toString()}"
            )
            return setOf()
        }
    }

    override suspend fun saveLocation(location: Location) {
        authenticate()
        try {
            val userDocument = UserMapper.mapDomainToDocument(
                User(
                    uid = auth.currentUser!!.uid,
                    name = appContext.getSharedPreferences("avatars", 0)
                        .getString("name", "Guest")!!,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = Date().time,
                    group = appContext.getSharedPreferences("avatars", 0)
                        .getString("group", "Guest")!!,
                )
            )
            storage.collection(LOCATIONS_COLLECTION).document(auth.currentUser!!.uid)
                .set(userDocument).await()
        } catch (err: Exception) {
            Log.e(TAG, "Error on user location saving to the firestore: ${err.toString()}")
        }
    }

    companion object {
        private const val TAG = "FirebaseRepositoryImpl"
        private const val LOCATIONS_COLLECTION = "location"
    }
}
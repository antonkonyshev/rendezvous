package com.github.antonkonyshev.tryst.data

import android.content.Context
import android.location.Location
import android.util.Log
import com.github.antonkonyshev.tryst.BuildConfig.GIST_ID
import com.github.antonkonyshev.tryst.domain.LocationRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.Date
import java.util.UUID

interface GistApiSchema {
    @GET("")
    suspend fun getUsers(): List<String>

    @GET("{gistId}")
    suspend fun getUser(@Path("gistId") gistId: String): String

    @PATCH("{gistId}")
    suspend fun updateUser(@Path("gistId") gistId: String, @Body payload: RequestBody)

    @POST("")
    suspend fun createUser()
}

class GistRepositoryImpl(private val api: GistApiSchema, private val appContext: Context) :
    LocationRepository {
    private val uid: String by lazy {
        val prefs = appContext.getSharedPreferences(TrystApplication.geolocationWorkerName, 0)
        var uid = prefs.getString("uid", null)
        if (uid == null) {
            uid = UUID.randomUUID().toString()
            prefs.edit().putString("uid", uid).commit()
        }
        return@lazy uid
    }

    override suspend fun getUsers(): List<String> {
        return emptyList()
    }

    override suspend fun getUser(id: String): String? {
        return null
    }

    override suspend fun saveLocation(location: Location) {
        try {
            api.updateUser(GIST_ID, JSONObject().apply {
                put("files", JSONObject().apply {
                    put(uid, JSONObject().apply {
                        put("content", JSONObject().apply {
                            put("latitude", location.latitude)
                            put("longitude", location.longitude)
                            put("timestamp", Date().time)
                        }.toString())
                    })
                })
            }.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            Log.d(TAG, "Location saved to the gist")
        } catch (ex: Exception) {
            Log.e(TAG, "Error on location saving to the gist: ${ex.toString()}")
        }
    }

    companion object {
        private const val TAG = "TrystGistRepository"
    }
}

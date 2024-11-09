package com.github.antonkonyshev.tryst.data

import android.content.Context
import android.location.Location
import android.util.Log
import com.github.antonkonyshev.tryst.BuildConfig.GIST_ID
import com.github.antonkonyshev.tryst.domain.LocationRepository
import com.github.antonkonyshev.tryst.domain.User
import com.google.gson.GsonBuilder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import java.util.Date
import java.util.UUID

interface GistApiSchema {
    @GET("{gistId}")
    suspend fun getUsers(@Path("gistId") gistId: String): Response<GistFiles>

    @PATCH("{gistId}")
    suspend fun updateUser(@Path("gistId") gistId: String, @Body payload: RequestBody)
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
    private val name: String by lazy {
        appContext.getSharedPreferences("avatars", 0).getString("name", "Guest")!!
    }

    override suspend fun getUsers(): Set<User> {
        var users = mutableSetOf<User>()
        try {
            val gson = GsonBuilder().create()
            api.getUsers(GIST_ID).body()?.files?.forEach { gistUid, gistFile ->
                try {
                    if (gistUid == uid)
                        return@forEach
                    users.add(gson.fromJson(gistFile.content, User::class.java).apply {
                        this@apply.uid = gistUid
                    })
                } catch (err: Exception) {
                    Log.e(TAG, "Error on user gist data parsing: ${err.toString()}")
                }
            }
        } catch (err: Exception) {
            Log.e(TAG, "Error on gist data parsing: ${err.toString()}")
        }
        return users
    }

    override suspend fun saveLocation(location: Location) {
        try {
            api.updateUser(GIST_ID, JSONObject().apply {
                put("files", JSONObject().apply {
                    put(uid, JSONObject().apply {
                        put("content", JSONObject().apply {
                            put("name", name)
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

data class GistFile(var content: String)
data class GistFiles(var files: HashMap<String, GistFile>)
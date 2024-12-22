package com.github.antonkonyshev.tryst.data

import com.github.antonkonyshev.tryst.domain.User
import com.google.firebase.firestore.QueryDocumentSnapshot

object UserMapper {
    fun mapDomainToData(user: User): HashMap<String, Any?> {
        return mapDomainToDocument(user).apply {
            set("uid", user.uid)
        }
    }

    fun mapDomainToDocument(user: User): HashMap<String, Any?> {
        return hashMapOf(
            "name" to user.name,
            "latitude" to user.latitude,
            "longitude" to user.longitude,
            "timestamp" to user.timestamp,
            "group" to user.group,
        )
    }

    fun mapDocumentToDomain(uid: String, data: QueryDocumentSnapshot): User {
        return User(
            uid = uid, name = data.getString("name") ?: "Guest",
            latitude = data.getDouble("latitude")!!,
            longitude = data.getDouble("longitude")!!,
            timestamp = data.getLong("timestamp")!!,
            group = data.getString("group") ?: "Guest",
        )
    }
}
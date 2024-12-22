package com.github.antonkonyshev.tryst.di

import com.github.antonkonyshev.tryst.BuildConfig.GITHUB_TOKEN
import com.github.antonkonyshev.tryst.data.FirebaseRepositoryImpl
import com.github.antonkonyshev.tryst.data.GistApiSchema
import com.github.antonkonyshev.tryst.data.GistRepositoryImpl
import com.github.antonkonyshev.tryst.domain.LocationRepository
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val networkModule = module {
    single {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/gists/")
            .client(
                OkHttpClient.Builder()
//                    .addNetworkInterceptor(HttpLoggingInterceptor().apply {
//                        setLevel(HttpLoggingInterceptor.Level.BODY)
//                    })
                    .addNetworkInterceptor(object : Interceptor {
                        override fun intercept(chain: Interceptor.Chain): Response {
                            return chain.proceed(
                                chain.request().newBuilder()
                                    .addHeader("Accept", "application/vnd.github+json")
                                    .addHeader("Authorization", "Bearer ${GITHUB_TOKEN}")
                                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                                    .build()
                            )
                        }
                    }).build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GistApiSchema::class.java)
    }

//    single<LocationRepository> { GistRepositoryImpl(get(), androidContext()) }

    single<LocationRepository> { FirebaseRepositoryImpl(androidContext()) }
}
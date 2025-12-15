package com.example.bostonbound

import com.squareup.moshi.Moshi
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

interface ApiService {

    @POST("/plan")
    suspend fun planTrip(@Body request: PlanRequest): PlanResponse

    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000"

        fun create(): ApiService {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}

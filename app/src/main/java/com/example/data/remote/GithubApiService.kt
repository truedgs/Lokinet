package com.example.data.remote

import com.example.model.RemoteConfig
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface GithubApiService {
    @GET
    suspend fun getRemoteConfig(@Url url: String): RemoteConfig

    @GET
    suspend fun getPremiumHwids(@Url url: String): ResponseBody
}

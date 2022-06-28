package com.example.messengerservicedemo.api.network

import com.example.messengerservicedemo.App
import com.example.messengerservicedemo.response.DailyResponse
import com.example.messengerservicedemo.response.RealtimeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:38
 * 描述 : 描述
 */

interface WeatherService {

    @GET("v2.6/${App.WeatherToken}/{lng},{lat}/realtime")
    fun getRealtimeWeather(@Path("lng") lng: String, @Path("lat") lat: String): Call<RealtimeResponse>

    @GET("v2.6/${App.WeatherToken}/{lng},{lat}/daily")
    fun getDailyWeather(@Path("lng") lng: String, @Path("lat") lat: String, @Query("dailysteps") dailySteps: Int =8): Call<DailyResponse>

}
package com.example.messengerservicedemo.api.network

import com.example.messengerservicedemo.App
import com.example.messengerservicedemo.response.PlaceResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:37
 * 描述 : 描述
 */

interface PlaceService{
    @GET("v2/place?token=${App.WeatherToken}&lang=zh_CN")
    fun searchPlaces(@Query("query") query: String): Call<PlaceResponse>
}
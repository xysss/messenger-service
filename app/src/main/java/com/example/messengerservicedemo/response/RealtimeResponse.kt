package com.example.messengerservicedemo.response

import com.google.gson.annotations.SerializedName

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:02
 * 描述 : 描述
 */

class RealtimeResponse(val status: String, val result: Result) {

    class Result(val alert :Alert,val realtime: Realtime)

    class Realtime(val skycon: String, val temperature: Float, val humidity :Float,val cloudrate :Float, val visibility:Float,
                   @SerializedName("air_quality") val airQuality: AirQuality,
                   @SerializedName("wind") val wind: Wind,
                   @SerializedName("precipitation") val precipitation: Precipitation,
                   @SerializedName("life_index") val lifeIndex: LifeIndex
    )

    class Alert(val content: List<Content>)

    class Content(val code: String)

    class AirQuality(val aqi: AQI, val pm25 :Float,val pm10 :Float,val o3: Float,
                     val so2: Float,val no2: Float,val co: Float)

    class Wind(val speed: Float,val direction :Float)

    class AQI(val chn: Float)

    class Nearest()

    class Ultraviolet(val index :Int,val desc :String)

    class Local(val intensity :Float)

    class LifeIndex(@SerializedName("ultraviolet")val ultraviolet :Ultraviolet)

    class Precipitation( @SerializedName("local") val local: Local, @SerializedName("nearest") val nearest: Nearest)

}
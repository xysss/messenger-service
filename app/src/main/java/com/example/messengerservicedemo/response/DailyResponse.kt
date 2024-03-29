package com.example.messengerservicedemo.response

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:04
 * 描述 : 描述
 */

class DailyResponse(val status: String, val result: Result) {

    class Result(val daily: Daily)

    class Daily(val astro :List<Astro>,val precipitation : List<Precipitation>,val temperature: List<Temperature>, val skycon: List<Skycon>, @SerializedName("life_index") val lifeIndex: LifeIndex)

    class Astro(val sunrise: Sunrise, val sunset: Sunset)

    class Sunrise(val time: String)

    class Sunset(val time: String)

    class Temperature(val max: Float, val min: Float)

    class Precipitation(val probability: Float)

    class Skycon(val value: String, val date: Date)

    class LifeIndex(val coldRisk: List<LifeDescription>, val carWashing: List<LifeDescription>, val ultraviolet: List<LifeDescription>, val dressing: List<LifeDescription>)

    class LifeDescription(val desc: String,val index: Int)

}
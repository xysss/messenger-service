package com.example.messengerservicedemo.response

import com.google.gson.annotations.SerializedName

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:01
 * 描述 : 描述
 */
class PlaceResponse(val status: String, val places: List<Place>)
//SerializedName  注解对应json 返回字段名称
class Place(val name: String, val location: Location, @SerializedName("formatted_address") val address: String)

class Location(val lng: String, val lat: String)

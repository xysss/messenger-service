package com.example.messengerservicedemo.repository

import android.content.Context
import androidx.core.content.edit
import com.example.messengerservicedemo.response.Location
import com.example.messengerservicedemo.response.Place
import com.google.gson.Gson
import me.hgj.mvvmhelper.base.appContext

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:34
 * 描述 : 描述
 */

object PlaceDao {

    fun savePlace(place: Place) {
        sharedPreferences().edit {
            putString("place", Gson().toJson(place))
        }
    }

    fun getSavedPlace(): Place {
        val placeJson = sharedPreferences().getString("place", "")
        if(placeJson=="") {
            val location = Location("", "")
            return Place("地区", location, "")
        }
        return Gson().fromJson(placeJson, Place::class.java)
    }

    fun isPlaceSaved() = sharedPreferences().contains("place")

    private fun sharedPreferences() =
        appContext.getSharedPreferences("sunny_weather", Context.MODE_PRIVATE)

}
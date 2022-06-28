package com.example.messengerservicedemo.response

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:03
 * 描述 : 描述
 */

@SuppressLint("ParcelCreator")
@Parcelize
data class SearchResponse(var id: Int,
                          var link: String,
                          var name: String,
                          var order: Int,
                          var visible: Int) : Parcelable

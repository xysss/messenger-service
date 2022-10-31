package com.example.messengerservicedemo.response

/**
 * 作者 : xys
 * 时间 : 2022-10-31 14:36
 * 描述 : 描述
 */
data class AppVersionResponse(
    var version : String,
    var appUrl : String,
    var force : String,
    var type : String
)
package com.example.messengerservicedemo.response

/**
 * 作者 : xys
 * 时间 : 2022-06-27 16:48
 * 描述 : 玩Android 服务器返回的数据基类
 */

data class ApiResponse<T>(
    var data: T,
    var errorCode: Int = -1,
    var errorMsg: String = ""
)
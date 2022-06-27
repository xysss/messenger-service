package com.example.messengerservicedemo.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * 作者 : xys
 * 时间 : 2022-06-27 16:47
 * 描述 : 描述
 */

class HeadInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
        //模拟了2个公共参数
        //builder.addHeader("token", "token123456").build()
        //builder.addHeader("device", "Android").build()
        return chain.proceed(builder.build())
    }

}
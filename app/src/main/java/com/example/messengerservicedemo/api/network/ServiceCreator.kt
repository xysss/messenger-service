package com.example.messengerservicedemo.api.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:38
 * 描述 : 描述
 */

object ServiceCreator{  //单例
    //inline 的工作原理就是将内联函数的函数体复制到调用处实现内联。
    private const val BASE_URL="https://api.caiyunapp.com/"
    private val retrofit= Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    fun <T> create(serviceClass: Class<T>):T= retrofit.create(serviceClass)

    inline fun <reified  T> create(): T= create(T::class.java)

}
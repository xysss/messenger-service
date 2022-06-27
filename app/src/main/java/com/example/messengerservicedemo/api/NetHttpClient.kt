package com.example.messengerservicedemo.api

import me.hgj.mvvmhelper.base.appContext
import me.hgj.mvvmhelper.net.interception.LogInterceptor
import okhttp3.OkHttpClient
import rxhttp.wrapper.cookie.CookieStore
import rxhttp.wrapper.ssl.HttpsUtils
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 作者 : xys
 * 时间 : 2022-06-27 16:43
 * 描述 : 描述
 */

object NetHttpClient {
    fun getDefaultOkHttpClient():  OkHttpClient.Builder {
        val sslParams = HttpsUtils.getSslSocketFactory()
        return OkHttpClient.Builder()
            //使用CookieStore对象磁盘缓存,自动管理cookie 玩安卓自动登录验证
            .cookieJar(CookieStore(File(appContext.externalCacheDir, "RxHttpCookie")))
            .connectTimeout(15, TimeUnit.SECONDS)//读取连接超时时间 15秒
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HeadInterceptor())//自定义头部参数拦截器
            .addInterceptor(LogInterceptor())//添加Log拦截器
            .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager) //添加信任证书
            .hostnameVerifier { hostname, session -> true } //忽略host验证
    }
}
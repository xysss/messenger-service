package com.example.messengerservicedemo.api

import rxhttp.wrapper.annotation.DefaultDomain

/**
 * 作者 : xys
 * 时间 : 2022-06-27 16:47
 * 描述 : 描述
 */

object NetUrl {

    // 服务器请求成功的 Code值
    const val SUCCESS_CODE = 0

    @DefaultDomain //设置为默认域名
    const val DEV_URL = "https://wanandroid.com/"

    //登录
    const val LOGIN = "user/login"
    //获取首页列表数据
    const val HOME_LIST = "article/list/%1\$d/json"
    //项目分类标题
    const val PROJECT_TITLE_LIST = "project/tree/json"
    //项目列表数据
    const val Project_Data = "project/list/%1\$d/json"

    const val UPLOAD_URL = "http://t.xinhuo.com/index.php/Api/Pic/uploadPic"

    var DOWNLOAD_URL = "http://app.htvision.com.cn/app_release/CR1600II/3.0.106/CR1600-20210723-3.0.106.apk"

    /**
     * 公众号分类
     */
    const val Public_Title = "wxarticle/chapters/json"

    /**
     * 获取公众号数据
     */
    const val Public_Data = "wxarticle/list/%1\$d/%1\$d/json"
}
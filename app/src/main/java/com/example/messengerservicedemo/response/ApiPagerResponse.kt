package com.example.messengerservicedemo.response

import me.hgj.mvvmhelper.entity.BasePage

/**
 * 作者 : xys
 * 时间 : 2022-06-27 16:49
 * 描述 : 玩Android 服务器返回的列表数据基类
 */

data class ApiPagerResponse<T>(
    var datas: ArrayList<T>,
    var curPage: Int,
    var offset: Int,
    var over: Boolean,
    var pageCount: Int,
    var size: Int,
    var total: Int
) : BasePage<T>() {
    override fun getPageData() = datas
    override fun isRefresh() = offset == 0
    override fun isEmpty() = datas.isEmpty()
    override fun hasMore() = !over
}
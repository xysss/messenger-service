package com.example.messengerservicedemo.serialport.listener

import com.example.messengerservicedemo.serialport.model.SystemStateModel

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:04
 * 描述 : 读取系统信息
 */
interface OnReadSystemStateListener {
    /**
     * 结果
     *
     * @param systemStateModel 系统结果
     */
    fun onResult(systemStateModel: SystemStateModel)
}
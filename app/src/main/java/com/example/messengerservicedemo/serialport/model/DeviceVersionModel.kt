package com.example.messengerservicedemo.serialport.model

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:05
 * 描述 : 硬件版本信息
 */
data class DeviceVersionModel(
    val serialNumber: String?,
    val hardwareAmount: String?,
    val hardwareAppAmount: String?,
)
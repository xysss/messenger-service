package com.example.messengerservicedemo.serialport.model

/**
 * 作者 : xys
 * 时间 : 2022-07-01 15:50
 * 描述 : 描述
 */
data class SensorModel (
    val senId: String,  //传感器索引号 0-8
    val senType: String,  //暂时不用
    val version: String,
    val name: String,
    val unit: String,
    val reserv: String,
    val mw: String,
    val fullScale: String,
    val sensibility: String
    )
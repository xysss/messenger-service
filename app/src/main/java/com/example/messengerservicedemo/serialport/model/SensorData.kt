package com.example.messengerservicedemo.serialport.model

/**
 * 作者 : xys
 * 时间 : 2022-07-04 17:00
 * 描述 : 描述
 */
class SensorData (
    val senId: String,  //传感器索引号 0-8
    val senState: String,
    val sensorValue: String,
    val senOverFlow: String,
    val senDecimalLen: String,
    val senTempState: String,
    val senTempValue: String,
    val senHumidityState: String,
    val senHumidityValue: String
    )
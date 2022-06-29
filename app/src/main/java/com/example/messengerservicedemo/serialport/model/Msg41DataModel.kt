package com.example.messengerservicedemo.serialport.model

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:05
 * 描述 : 描述
 */

data class Msg41DataModel(
    val sensorStatus: String,
    val voc: String,
    val dust: String,
    val temp: String,
    val dumity: String,
    val nfcMode1: NfcModel,
    val nfcMode2: NfcModel,
    val nfcMode3: NfcModel,
    val workPattern: String,
    val electricalMachinery: String,
    val disinfectionFunction: String,
    val bhState: String,
    val infraredState: String,
    val deviceState: String,
)
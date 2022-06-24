package com.example.messengerservicedemo.ext

/**
 * 作者　: xys
 * 时间　: 2021/09/27
 * 描述　:
 */
object ValueKey {
    const val MMKV_APP_KEY = "app_mmkv_storage"

    const val isFirst = "app_is_first_open"

    //设备信息
    const val deviceHardwareVersion = "deviceHardwareVersion"
    const val deviceSoftwareVersion = "deviceSoftwareVersion"
    const val deviceBattery = "deviceBattery"
    const val deviceFreeMemory = "deviceFreeMemory"
    const val deviceRecordSum = "deviceRecordSum"
    const val deviceAlarmSum = "deviceAlarmSum"
    const val deviceCurrentRunningTime = "deviceCurrentRunningTime"
    const val deviceCurrentAlarmNumber = "deviceCurrentAlarmNumber"
    const val deviceCumulativeRunningTime = "deviceCumulativeRunningTime"
    const val deviceDensityMax = "deviceDensityMax"
    const val deviceDensityMin = "deviceDensityMin"
    const val deviceTwaNumber = "deviceTwaNumber"
    const val deviceSteLNumber = "deviceSteLNumber"
    const val deviceId = "deviceId"

    //蓝牙
    const val SERVICE_UUID = "service_uuid"  //服务 UUID
    const val DESCRIPTOR_UUID = "descriptor_uuid"  //描述 UUID
    const val CHARACTERISTIC_WRITE_UUID = "characteristic_write_uuid"  //特征（特性）写入 UUID
    const val CHARACTERISTIC_INDICATE_UUID = "characteristic_indicate_uuid"  //特征（特性）表示 UUID
    const val NULL_NAME = "nullName"  //是否过滤设备名称为Null的设备
    const val RSSI = "rssi"  //过滤信号强度值

    //物质库
    const val matterSum = "matterSum"  //物质库个数

    //MQTT
    const val recTopicValue = "recTopicValue" //接收主题
    const val sendTopicValue = "sendTopicValue" //发送主题
}
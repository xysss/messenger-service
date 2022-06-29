package com.example.messengerservicedemo.serialport.sender

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:07
 * 描述 : 发送指令接口
 */

interface Sender {
    /**
     * 主板：检测
     * @return true 发送成功
     */
    fun sendStartDetect(): ByteArray

    /**
     * 主板：检测版本号
     * @return true 发送成功
     */
    fun sendReadVersion(): ByteArray


    fun sendTest(): ByteArray
}
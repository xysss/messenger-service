package com.example.messengerservicedemo.serialport

import com.example.messengerservicedemo.serialport.sender.AdapterSender
import com.example.messengerservicedemo.serialport.sender.Sender

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:00
 * 描述 : 串口数据发送工具管理初始化
 */

object SenderManager {
    private var senderMap = hashMapOf<Int, Sender>()

    /**
     * 获取发送者
     */
    @JvmOverloads
    fun getSender(type: Int = 0): Sender {
        if (senderMap[type] == null) {
            senderMap[type] = createSender(type)
        }
        return senderMap[type]!!
    }

    /**
     * 创建发送者
     *
     * @param type
     * @return
     */
    private fun createSender(type: Int): Sender {
        return when (type) {
            0, 1 -> AdapterSender()
            else -> AdapterSender()
        }
    }
}

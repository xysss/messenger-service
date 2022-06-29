package com.example.messengerservicedemo.serialport.sender

import com.example.messengerservicedemo.serialport.commond.SerialCommandProtocol.onCmdCheckDeviceStatusInfo
import com.example.messengerservicedemo.serialport.commond.SerialCommandProtocol.onCmdReadVersionStatus
import com.example.messengerservicedemo.serialport.commond.SerialCommandProtocol.test

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:07
 * 描述 : 发送指令实现
 */
class AdapterSender : Sender {

    override fun sendStartDetect(): ByteArray {
        return onCmdCheckDeviceStatusInfo()
    }

    override fun sendReadVersion(): ByteArray {
        return onCmdReadVersionStatus()
    }

    override fun sendTest(): ByteArray {
        return test()
    }
}
package com.example.messengerservicedemo.serialport.proxy

import android.util.Log
import com.example.messengerservicedemo.BuildConfig
import com.example.messengerservicedemo.util.DataConvertUtil
import com.serial.port.kit.core.SerialPortFinder
import com.serial.port.manage.SerialPortKit
import com.serial.port.manage.SerialPortManager
import me.hgj.mvvmhelper.base.appContext

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:06
 * 描述 : 串口代理
 */

class SerialPortProxy {

    companion object {
        private const val TAG = "SerialPortProxy"
    }

    private lateinit var serialPortManager: SerialPortManager

    private var isInitSuccess = false

    private val isInit
        get() = isInitSuccess

    private fun initSdk() {
        searchAllDevices()
        serialPortManager = SerialPortKit.newBuilder(appContext)
            // 设备地址
            .path("/dev/ttyS1")
            // 波特率
            .baudRate(115200)
            // Byte数组最大接收内存
            .maxSize(1024)
            // 发送失败重试次数
            .retryCount(2)
            // 发送一次指令，最多接收几次设备发送的数据，局部接收次数优先级高
            .receiveMaxCount(1)
            // 是否按照 maxSize 内存进行接收
            .isReceiveMaxSize(false)
            // 是否显示吐司
            .isShowToast(true)
            // 是否Debug模式，Debug模式会输出Log
            .debug(BuildConfig.DEBUG)
            // 是否自定义校验下位机发送的数据正确性，把校验好的Byte数组装入WrapReceiverData
            .isCustom(true, DataConvertUtil.customProtocol())
            // 校验发送指令与接收指令的地址位，相同则为一次正常的通讯
            .addressCheckCall(DataConvertUtil.addressCheckCall())
            .build()
            .get()
        isInitSuccess = true
    }

    private fun reInitSdk() {
        if (!isInit) {
            initSdk()
        }
    }

    fun searchAllDevices() {
        try {
            val serialPortFinder = SerialPortFinder()
            serialPortFinder.allDevices.forEach {
                Log.d(TAG, "搜索到的串口信息为: $it")
            }
        } catch (e: Exception) {
            Log.d(TAG, "initSerialPort: ", e)
        }
    }

    val portManager: SerialPortManager
        get() {
            reInitSdk()
            return serialPortManager
        }
}
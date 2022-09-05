package com.example.messengerservicedemo.serialport

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.messengerservicedemo.ext.logFlag
import com.example.messengerservicedemo.serialport.commond.SerialCommandProtocol
import com.example.messengerservicedemo.serialport.proxy.SerialPortProxy
import com.example.messengerservicedemo.util.ByteUtils
import com.serial.port.kit.core.common.TypeConversion
import com.serial.port.manage.SerialPortManager
import com.serial.port.manage.data.WrapReceiverData
import com.serial.port.manage.data.WrapSendData
import com.serial.port.manage.listener.OnDataReceiverListener
import com.swallowsonny.convertextlibrary.toHexString
import com.swallowsonny.convertextlibrary.writeInt16LE
import me.hgj.mvvmhelper.ext.logE
import okhttp3.internal.toHexString
import java.util.ArrayList
import kotlin.experimental.and

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:00
 * 描述 : 工具指令管理 只有使用的时候才会进行初始化
 */

object SerialPortHelper {

    private const val TAG = "SerialPortManager"

    private val mHandler = Handler(Looper.getMainLooper())
    private val mProxy = SerialPortProxy()


    private val transSendCodingList = ArrayList<Byte>()
    private lateinit var transSendCodingBytes: ByteArray


    /**
     * 暴露SDK
     */
    val portManager: SerialPortManager
        get() = mProxy.portManager

    /**
     * 内部使用，默认开启串口
     */
    private val serialPortManager: SerialPortManager
        get() {
            // 默认开启串口
            if (!portManager.isOpenDevice) {
                portManager.open()
            }
            return portManager
        }


    /**
     * 读取设备版本信息
     *
     * @param listener 监听回调
     */
    fun readVersion() {
        val sends: ByteArray = transSendCoding(SerialCommandProtocol.onCmdReadVersionStatus())
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 300, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun getSensorInfo() {
        val sends: ByteArray =transSendCoding(SerialCommandProtocol.getSensorInfoReq())
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 300, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun sendNetState(bytes: ByteArray) {
        val sends: ByteArray = transSendCoding(SerialCommandProtocol.sendNetStateReq(bytes))
        "发送网络状态!Service：${sends.toHexString()}".logE(logFlag)
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 300, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "发送网络状态onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun setDeviceSensorState(bytes: ByteArray) {
        val sends: ByteArray =transSendCoding(SerialCommandProtocol.setDeviceSensorDataReq(bytes))
        "停止主动上报!Service：${sends.toHexString()}".logE(logFlag)
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 300, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "停止主动上报 onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun sendBeginUpdate(bytes: ByteArray) {
        val sends: ByteArray = transSendCoding(SerialCommandProtocol.sendBeginUpdateReq(bytes))
        "开始固件更新请求 Service：${sends.toHexString()}".logE(logFlag)
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 300, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "开始固件更新请求 onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun sendUpdate(bytes: ByteArray,allLength: Int,dataLength: Int) {
        val allLengthByte= ByteArray(2)
        allLengthByte.writeInt16LE(allLength)

        val dataLengthByte= ByteArray(2)
        dataLengthByte.writeInt16LE(dataLength)

        val updateHeadByte=ByteArray(7)
        updateHeadByte[0]=0x55.toByte()
        updateHeadByte[1]=allLengthByte[1]
        updateHeadByte[2]=allLengthByte[0]
        updateHeadByte[3]=0x00.toByte()
        updateHeadByte[4]=0x02.toByte()
        updateHeadByte[5]=dataLengthByte[1]
        updateHeadByte[6]=dataLengthByte[0]

        val sends: ByteArray = transSendCoding(SerialCommandProtocol.sendUpdateReq(updateHeadByte+bytes))
        "分包固件更新请求 Service：${sends.toHexString()}".logE(logFlag)
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 300, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "分包固件更新请求 onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun sendEndUpdate(bytes: ByteArray) {
        val sends: ByteArray = transSendCoding(SerialCommandProtocol.sendEndUpdateReq(bytes))
        "结束固件更新请求 Service：${sends.toHexString()}".logE(logFlag)
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 300, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "结束固件更新请求 onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun sendWeatherData(bytes: ByteArray) {
        val sends: ByteArray = transSendCoding(SerialCommandProtocol.putWeatherData(bytes))

        "发送天气数据：${sends.toHexString()} : ${sends.toHexString(false).length}".logE(logFlag)
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 3000, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "发送天气数据 onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun sendTime(bytes: ByteArray) {
        val sends: ByteArray = transSendCoding(SerialCommandProtocol.putTime(bytes))

        "发送时间：${sends.toHexString()}".logE(logFlag)
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 3000, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "发送时间 onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    fun getSensorData() {
        val sends: ByteArray = transSendCoding(SerialCommandProtocol.getSensorDaraReq())
        val isSuccess: Boolean = serialPortManager.send(
            WrapSendData(sends, 3000, 300, 1),
            object : OnDataReceiverListener {
                override fun onSuccess(data: WrapReceiverData) {
                    val buffer: ByteArray = data.data
                }
                override fun onFailed(wrapSendData: WrapSendData, msg: String) {
                    "onFailed: $msg".logE(logFlag)
                }
                override fun onTimeOut() {
                    "onTimeOut: 发送数据或者接收数据超时".logE(logFlag)
                }
            })
        printLog(isSuccess, sends)
    }

    /**
     * 检测回调数据是否符合要求
     *
     * @param buffer 回调数据
     * @return true 符合要求 false 数据命令未通过校验
     */
    private fun checkCallData(buffer: ByteArray): Boolean {
        val tempData = TypeConversion.bytes2HexString(buffer)
        Log.i(TAG, "receive serialPort data ：$tempData")
        return buffer[0] == SerialCommandProtocol.baseStart[0] && SerialCommandProtocol.checkHex(
            buffer
        )
    }

    /**
     * 打印发送数据Log
     *
     * @param isSuccess 是否成功
     * @param bytes     数据
     */
    private fun printLog(isSuccess: Boolean, bytes: ByteArray) {
        val tempData = TypeConversion.bytes2HexString(bytes)
        "buildControllerProtocol:" + tempData + "，结果=" + if (isSuccess) "发送成功" else "发送失败".logE(logFlag)
    }

    /**
     * 切换到主线程
     *
     * @param runnable Runnable
     */
    private fun runOnUiThread(runnable: Runnable) {
        mHandler.post(runnable)
    }

    private fun transSendCoding(bytes: ByteArray): ByteArray {
        bytes.let {
            var i = 1
            if (it[0] == ByteUtils.FRAME_START) {
                transSendCodingList.clear()
                transSendCodingList.add(it[0])
            }
            while (i < it.size) {
                //校验开头
                //开始转码
                when {
                    it[i] == ByteUtils.FRAME_START -> {
                        transSendCodingList.add(ByteUtils.FRAME_FF)
                        transSendCodingList.add(ByteUtils.FRAME_00)
                    }
                    it[i] == ByteUtils.FRAME_FF -> {
                        transSendCodingList.add(ByteUtils.FRAME_FF)
                        transSendCodingList.add(ByteUtils.FRAME_FF)
                    }
                    else -> transSendCodingList.add(it[i])
                }
                i++
            }
        }

        transSendCodingList.let {
            transSendCodingBytes = ByteArray(it.size)
            for (k in transSendCodingBytes.indices) {
                transSendCodingBytes[k] = it[k]
            }
        }
        return transSendCodingBytes
    }
}
package com.example.messengerservicedemo.serialport.commond

import com.example.messengerservicedemo.util.ByteUtils
import com.example.messengerservicedemo.util.Crc8
import com.serial.port.manage.command.protocol.BaseProtocol
import com.swallowsonny.convertextlibrary.writeInt16LE
import com.swallowsonny.convertextlibrary.writeInt8

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:03
 * 描述 : 命令池
 */

object SerialCommandProtocol : BaseProtocol() {
    var baseStart = byteArrayOf(0x55.toByte())
    var baseEnd = byteArrayOf(0x23.toByte())

    /**
     * 系统状态参数读取,检测机器状态信息
     */
    var systemState = byteArrayOf(0xA1.toByte())
    var deviceInfo = byteArrayOf(0x00.toByte(), 0xB5.toByte())

    /**
     * 读取主板版本号
     */
    private var readVersion = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x00.toByte(),
        0x02.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    //设置工作模式
    private var setWorkModel = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0A.toByte(),
        0x00.toByte(),
        0x4E.toByte(),
        0x00.toByte(),
        0x01.toByte()
    )

    private var getSensorInfoByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x00.toByte(),
        0x08.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    //获取设备净化功能请求
    var getDevicePurifyReq = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x00.toByte(),
        0x72.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    //设置设备净化功能请求
    var setDevicePurifyReq = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0D.toByte(),
        0x00.toByte(),
        0x56.toByte(),
        0x00.toByte(),
        0x04.toByte()
    )

    //设置设备消毒功能请求
    var setDeviceDisinfectReq = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0A.toByte(),
        0x00.toByte(),
        0x50.toByte(),
        0x00.toByte(),
        0x01.toByte()
    )


    /**
     * 升级指令
     */
    var upgrade = byteArrayOf(0xAA.toByte())
    var readyForUpgrade = byteArrayOf(0x01.toByte(), 0x00.toByte(), 0xAB.toByte())

    var testCommond = byteArrayOf(
        0x55.toByte(), 0x00.toByte(), 0x0A.toByte(), 0x09.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x23.toByte()
    )

    /**
     * 检查机器运行状态信息
     *
     * @return 0xAA 0xA1 0x00 0xB5
     */
    fun onCmdCheckDeviceStatusInfo(): ByteArray {
        return buildControllerProtocol(
            baseStart,
            systemState,
            deviceInfo
        )
    }

    fun test(): ByteArray {
        return buildControllerProtocol(
            testCommond
        )
    }

    /**
     * 获取主板版本号
     *
     * @return 0xAA 0xA9 0x00 0xAD
     */
    fun onCmdReadVersionStatus(): ByteArray {
        return this.readVersion + Crc8.cal_crc8_t(
            this.readVersion,
            this.readVersion.size
        ) + ByteUtils.FRAME_END
    }


    fun onCmdSetWorkModel(byte: Byte): ByteArray {
        val resultByte = setWorkModel + byte
        return resultByte + Crc8.cal_crc8_t(
            resultByte,
            resultByte.size
        ) + ByteUtils.FRAME_END
    }

    fun getSensorInfoReq(): ByteArray {
        return this.getSensorInfoByte + Crc8.cal_crc8_t(
            this.getDevicePurifyReq,
            this.getDevicePurifyReq.size
        ) + ByteUtils.FRAME_END
    }

    fun onCmdSetDevicePurifyReq(timing: Int, speed: Int): ByteArray {
        val timingByteArray = ByteArray(2)
        timingByteArray.writeInt16LE(timing)
        val speedByteArray = ByteArray(1)
        speedByteArray.writeInt8(speed)

        val resultByte = setDevicePurifyReq + timingByteArray + speedByteArray + ByteUtils.Msg00

        return resultByte + Crc8.cal_crc8_t(
            resultByte,
            resultByte.size
        ) + ByteUtils.FRAME_END
    }

    fun onCmdSetDeviceDisinfectReq(byte: Byte): ByteArray {
        val resultByte = setDeviceDisinfectReq + byte
        return resultByte + Crc8.cal_crc8_t(
            resultByte,
            resultByte.size
        ) + ByteUtils.FRAME_END
    }

    /**
     * 准备进入升级模式
     *
     * @return 0xAA 0xAA 0x01 0x00 0xAB
     */
    fun onCmdReadyForUpgrade(): ByteArray {
        return buildControllerProtocol(
            baseStart,
            upgrade,
            readyForUpgrade
        )
    }

    /**
     * 校验板子发回来的结果集
     *
     * @return
     */
    fun checkHex(ret: ByteArray): Boolean {
        var tempRet = 0
        for (i in 0 until ret.size - 1) {
            tempRet += ret[i]
        }
        return (tempRet.inv() + 1).toByte() == ret[ret.size - 1]
    }
}
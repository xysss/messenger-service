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

    private var getSensorInfoByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x00.toByte(),
        0x08.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    private var getRecdDeviceInfoByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x00.toByte(),
        0xC0.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    private var getNetStateInfoByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0A.toByte(),
        0x00.toByte(),
        0x0C.toByte(),
        0x00.toByte(),
        0x01.toByte()
    )

    private var setDeviceSensorInfoByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0A.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x00.toByte(),
        0x01.toByte()
    )

    private var beginUpdateByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0F.toByte(),
        0x00.toByte(),
        0x01.toByte(),
        0x00.toByte(),
        0x06.toByte()
    )

    private var setUIReqByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0F.toByte(),
        0x00.toByte(),
        0x0D.toByte(),
        0x00.toByte(),
        0x06.toByte()
    )

    private var endUpdateByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0D.toByte(),
        0x00.toByte(),
        0x03.toByte(),
        0x00.toByte(),
        0x04.toByte()
    )

    private var endUpdateUIByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0D.toByte(),
        0x00.toByte(),
        0x0F.toByte(),
        0x00.toByte(),
        0x04.toByte()
    )

    private var getSensorDataByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x09.toByte(),
        0x00.toByte(),
        0x04.toByte(),
        0x00.toByte(),
        0x00.toByte()
    )

    private var getWeatherToByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x5A.toByte(),
        0x00.toByte(),
        0x05.toByte(),
        0x00.toByte(),
        0x51.toByte()
    )

    private var putTimeToByte = byteArrayOf(
        0x55.toByte(),
        0x00.toByte(),
        0x0F.toByte(),
        0x00.toByte(),
        0x07.toByte(),
        0x00.toByte(),
        0x06.toByte()
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

    fun getSensorInfoReq(): ByteArray {
        return this.getSensorInfoByte + Crc8.cal_crc8_t(
            this.getSensorInfoByte,
            this.getSensorInfoByte.size
        ) + ByteUtils.FRAME_END
    }

    fun getRecDDeviceInfoReq(): ByteArray {
        return this.getRecdDeviceInfoByte + Crc8.cal_crc8_t(
            this.getRecdDeviceInfoByte,
            this.getRecdDeviceInfoByte.size
        ) + ByteUtils.FRAME_END
    }

    fun sendNetStateReq(byteArray: ByteArray): ByteArray {
        return this.getNetStateInfoByte+byteArray + Crc8.cal_crc8_t(
            this.getNetStateInfoByte+byteArray,
            this.getNetStateInfoByte.size+byteArray.size
        )+ ByteUtils.FRAME_END
    }

    fun setDeviceSensorDataReq(byteArray: ByteArray): ByteArray {
        return this.setDeviceSensorInfoByte+byteArray + Crc8.cal_crc8_t(
            this.setDeviceSensorInfoByte+byteArray,
            this.setDeviceSensorInfoByte.size+byteArray.size
        )+ ByteUtils.FRAME_END
    }

    fun sendBeginUpdateReq(byteArray: ByteArray): ByteArray {
        return this.beginUpdateByte+byteArray + Crc8.cal_crc8_t(
            this.beginUpdateByte+byteArray,
            this.beginUpdateByte.size+byteArray.size
        )+ ByteUtils.FRAME_END
    }

    fun setUIReq(byteArray: ByteArray): ByteArray {
        return this.setUIReqByte+byteArray + Crc8.cal_crc8_t(
            this.setUIReqByte+byteArray,
            this.setUIReqByte.size+byteArray.size
        )+ ByteUtils.FRAME_END
    }

    fun sendUpdateReq(byteArray: ByteArray): ByteArray {
        return byteArray + Crc8.cal_crc8_t(byteArray, byteArray.size)+ ByteUtils.FRAME_END
    }

    fun sendUIUpdateReq(byteArray: ByteArray): ByteArray {
        return byteArray + Crc8.cal_crc8_t(byteArray, byteArray.size)+ ByteUtils.FRAME_END
    }

    fun sendEndUpdateReq(byteArray: ByteArray): ByteArray {
        return this.endUpdateByte+byteArray + Crc8.cal_crc8_t(
            this.endUpdateByte+byteArray,
            this.endUpdateByte.size+byteArray.size
        )+ ByteUtils.FRAME_END
    }

    fun sendUIEndUpdateReq(byteArray: ByteArray): ByteArray {
        return this.endUpdateUIByte+byteArray + Crc8.cal_crc8_t(
            this.endUpdateUIByte+byteArray,
            this.endUpdateUIByte.size+byteArray.size
        )+ ByteUtils.FRAME_END
    }

    fun getSensorDaraReq(): ByteArray {
        return this.getSensorDataByte + Crc8.cal_crc8_t(
            this.getSensorDataByte,
            this.getSensorDataByte.size
        ) + ByteUtils.FRAME_END
    }

    fun putWeatherData(byteArray: ByteArray): ByteArray {
        return this.getWeatherToByte + byteArray + Crc8.cal_crc8_t(
            this.getWeatherToByte+byteArray,
            this.getWeatherToByte.size+byteArray.size
        ) + ByteUtils.FRAME_END
    }

    fun putTime(byteArray: ByteArray): ByteArray {
        return this.putTimeToByte + byteArray + Crc8.cal_crc8_t(
            this.putTimeToByte+byteArray,
            this.putTimeToByte.size+byteArray.size
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
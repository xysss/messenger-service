package com.example.messengerservicedemo.serialport

import com.example.messengerservicedemo.ext.*
import com.example.messengerservicedemo.serialport.model.SensorData
import com.example.messengerservicedemo.serialport.model.SensorModel
import com.example.messengerservicedemo.util.ByteUtils
import com.example.messengerservicedemo.util.Crc8
import com.swallowsonny.convertextlibrary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.hgj.mvvmhelper.ext.logE

/**
 * 作者 : xys
 * 时间 : 2022-06-29 11:24
 * 描述 : 描述
 */

class ProtocolAnalysis {

    private val transcodingBytesList = ArrayList<Byte>()
    private lateinit var afterBytes: ByteArray
    private val newLengthBytes = ByteArray(2)
    private var newLength = 0
    private var beforeIsFF = false
    private lateinit var recall: ReceiveDataCallBack

    private lateinit var senId: String
    private lateinit var senState: String
    private lateinit var sensorValue: String
    private lateinit var senOverFlow: String
    private lateinit var senDecimalLen: String
    private lateinit var senTempState: String
    private lateinit var senTempValue: String
    private lateinit var senHumidityState: String
    private lateinit var senHumidityValue: String
    private val sensorArray = ArrayList<SensorModel>()

    fun setUiCallback(dataCallback: ReceiveDataCallBack) {
        this.recall = dataCallback
    }

    @Synchronized
    fun addRecLinkedDeque(byte: Byte) {
        if (!recLinkedDeque.offer(byte)) {
            "recLinkedDeque空间已满".logE("xysLog")
        }
    }

    suspend fun startDealMessage() {
        while (true) {
            recLinkedDeque.poll()?.let {
                if (it == ByteUtils.FRAME_START) {
                    transcodingBytesList.clear()
                    transcodingBytesList.add(it)
                } else if (beforeIsFF) {
                    when (it) {
                        ByteUtils.FRAME_FF -> {
                            transcodingBytesList.add(ByteUtils.FRAME_FF)
                        }
                        ByteUtils.FRAME_00 -> {
                            transcodingBytesList.add(ByteUtils.FRAME_START)
                        }
                        else -> {
                            transcodingBytesList.add(ByteUtils.FRAME_FF)
                            transcodingBytesList.add(it)
                        }
                    }
                    beforeIsFF = false
                } else if (!beforeIsFF) {
                    if (it == ByteUtils.FRAME_FF) {
                        beforeIsFF = true
                    } else {
                        beforeIsFF = false
                        transcodingBytesList.add(it)
                    }
                }

                //取协议数据长度
                if (transcodingBytesList.size == 3) {
                    newLengthBytes[0] = transcodingBytesList[1]
                    newLengthBytes[1] = transcodingBytesList[2]
                    newLength = newLengthBytes.readInt16BE()
                    "协议长度: $newLength".logE("xysLog")
                }

                if (transcodingBytesList.size == newLength && transcodingBytesList.size > 9) {
                    transcodingBytesList.let { arrayList ->
                        afterBytes = ByteArray(arrayList.size)
                        for (k in afterBytes.indices) {
                            afterBytes[k] = arrayList[k]
                        }
                    }

                    isRecOK =
                        if (afterBytes[0] == ByteUtils.FRAME_START && afterBytes[afterBytes.size - 1] == ByteUtils.FRAME_END) {
                            //CRC校验
                            if (Crc8.isFrameValid(afterBytes, afterBytes.size)) {
                                analyseMessage(afterBytes)  //分发数据
                                //"协议正确: ${afterBytes.toHexString()}".logE("xysLog")
                                true
                            } else {
                                "CRC校验错误，协议长度: $newLength : ${afterBytes.toHexString()}".logE("xysLog")
                                false
                            }
                        } else {
                            "协议开头结尾不对:  ${afterBytes.toHexString()}".logE("xysLog")
                            false
                        }
                    transcodingBytesList.clear()
                } else if (newLength < 9 && transcodingBytesList.size > 9) { //协议长度不够
                    "解析协议不完整，协议长度: $newLength  解析长度：${transcodingBytesList.size} ,${transcodingBytesList.toHexString()}".logE(
                        "xysLog"
                    )
                    isRecOK = false
                    //BleHelper.retryHistoryMessage(recordCommand,alarmCommand)
                    transcodingBytesList.clear()
                }
            }
        }
    }

    private suspend fun analyseMessage(mBytes: ByteArray?) {
        mBytes?.let {
            when (it[4]) {
                //设备信息
                ByteUtils.Msg03 -> {
                    scope.launch(Dispatchers.IO) {
                        //dealMsg03(it)
                    }
                }
                //传感器信息读取请求
                ByteUtils.Msg88 -> {
                    scope.launch(Dispatchers.IO) {
                        //dealMsg88(it)
                    }
                }

                ByteUtils.Msg84 -> {
                    scope.launch(Dispatchers.IO) {
                        //dealMsg84(it)
                    }
                }

                else -> it[4].toInt().logE("xysLog")
            }
        }
    }

    private fun dealMsg03(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 33) {
                //版本号
                mmkv.putString(
                    ValueKey.deviceHardwareVersion,
                    it[7].toInt().toString() + ":" + it[8].toInt().toString()
                )
                mmkv.putString(
                    ValueKey.deviceSoftwareVersion,
                    it[9].toInt().toString() + ":" + it[10].toInt().toString()
                )
                //设备序列号
                var i = 11
                while (i < it.size)
                    if (it[i] == ByteUtils.FRAME_00) break else i++
                val tempBytes: ByteArray = it.readByteArrayBE(11, i - 11)
                mmkv.putString(ValueKey.deviceId, String(tempBytes))
                "设备信息响应成功: ${String(tempBytes)}".logE("xysLog")
            }
        }
    }

    private fun dealMsg88(mBytes: ByteArray) {
        mBytes.let {
            if (it.size > 10) {
                val sensorNum = it.readByteArrayBE(7 + 0, 2).readInt16LE()
                for (i in 0 until sensorNum) {
                    val sensorId = it[7 + 2 + i * 37].toInt().toString()
                    val sensorType = it.readByteArrayBE(7 + 3 + i * 37, 2).readInt16LE().toString()
                    val sensorVersion =
                        it.readByteArrayBE(7 + 5 + i * 37, 2).readInt16LE().toString()

                    var k = 14
                    while (k < it.size)
                        if (it[k] == ByteUtils.FRAME_00) break else k++
                    val tempBytes: ByteArray = it.readByteArrayBE(14, k - 14)
                    //val name = tempBytes.toAsciiString()
                    val sensorName = String(tempBytes)
                    val sensorUnit: String = when (it[7 + 27 + i * 37].toInt()) {
                        0 -> "PPM"
                        1 -> "vol%"
                        2 -> "LEL%"
                        3 -> "mg/m3"
                        4 -> "PPB"
                        else -> ""
                    }

                    val sensorReserv = it[7 + 28 + i * 37].toInt().toString()
                    val sensorWm = it.readByteArrayBE(7 + 29 + i * 37, 2).readInt16LE().toString()

                    val sensorFullScale =
                        it.readByteArrayBE(7 + 31 + i * 37, 4).readInt32LE().toString()

                    val sensorSensibility =
                        it.readByteArrayBE(7 + 35 + i * 37, 4).readFloatLE().toInt().toString()

                    val sensorModel = SensorModel(
                        sensorId,
                        sensorType,
                        sensorVersion,
                        sensorName,
                        sensorUnit,
                        sensorReserv,
                        sensorWm,
                        sensorFullScale,
                        sensorSensibility
                    )
                    sensorModel.toString().logE(logFlag)
                    sensorArray.add(sensorModel)
                }

            }
        }
    }

    private fun dealMsg84(mBytes: ByteArray) {
        mBytes.let {
            if (it.size > 25) {
                if (it[7] == ByteUtils.Msg26) {
                    senId = it[10].toInt().toString()
                    senState = it[11].toInt().toString()  //0-无故障，1-故障
                    sensorValue = it.readByteArrayBE(12, 2).readInt16LE().toString()
                    senOverFlow = it[14].toInt().toString()  //0-未溢出，1-溢出
                    senDecimalLen = it[15].toInt().toString()  ////小数点后位数
                }
                if (it[16] == ByteUtils.Msg61) {
                    senTempState = it[19].toInt().toString()  //0-无故障，1-故障
                    senTempValue = it[20].toInt().toString()  //温度，有符号单字节整数，单位：摄氏度
                }
                if (it[21] == ByteUtils.Msg62) {
                    senHumidityState = it[24].toInt().toString()  //0-无故障，1-故障
                    senHumidityValue = it[25].toInt().toString()  //湿度，无符号单字节整数，单位：%
                }

                val sensorData = SensorData(
                    senId,
                    senState,
                    sensorValue,
                    senOverFlow,
                    senDecimalLen,
                    senTempState,
                    senTempValue,
                    senHumidityState,
                    senHumidityValue
                )
                sensorData.toString().logE(logFlag)

            }
        }
    }

    interface ReceiveDataCallBack {
        fun onDataReceive(sensorData: SensorData)
    }
}
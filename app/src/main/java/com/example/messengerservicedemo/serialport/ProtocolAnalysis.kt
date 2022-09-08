package com.example.messengerservicedemo.serialport

import ZtlApi.ZtlManager
import com.example.messengerservicedemo.ext.*
import com.example.messengerservicedemo.serialport.model.SensorData
import com.example.messengerservicedemo.serialport.model.SensorModel
import com.example.messengerservicedemo.util.ByteUtils
import com.example.messengerservicedemo.util.Crc8
import com.swallowsonny.convertextlibrary.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    suspend fun reqGpio(){
        while (true) {
            try {
                delay(200)
                //第一个参数传入GPIO值
                //第二个参数传入in(输入)或out(输出)
                val getGpioA5Value = ZtlManager.GetInstance().getGpioValue("GPIO3_A5", "in")  //音量-
                val getGpioA4Value = ZtlManager.GetInstance().getGpioValue("GPIO3_A4", "in") //音量+
                if(getGpioA4Value==0){
                    scope.launch(Dispatchers.Main) {
                        ZtlManager.GetInstance().setRaiseSystemVolume()
                        val systemCurVolume = ZtlManager.GetInstance().systemCurrenVolume
                        "GpioValue:系统当前音量：$systemCurVolume".logE(logFlag)
                    }
                }
                if(getGpioA5Value==0){
                    scope.launch(Dispatchers.Main) {
                        ZtlManager.GetInstance().setLowerSystemVolume()
                        val systemCurVolume = ZtlManager.GetInstance().systemCurrenVolume
                        "GpioValue:系统当前音量：$systemCurVolume".logE(logFlag)
                    }
                }


                //ZtlManager.GetInstance().setRaiseSystemVolume()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
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
                    "协议长度: $newLength".logE("协议长度")
                }
                if (transcodingBytesList.size == newLength && transcodingBytesList.size >= 9) {
                    transcodingBytesList.let { arrayList ->
                        afterBytes = ByteArray(arrayList.size)
                        for (k in afterBytes.indices) {
                            afterBytes[k] = arrayList[k]
                        }
                    }
                    isRecOK = if (afterBytes[0] == ByteUtils.FRAME_START && afterBytes[afterBytes.size - 1] == ByteUtils.FRAME_END) {
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
                    "解析协议不完整，协议长度: $newLength  解析长度：${transcodingBytesList.size} ,${transcodingBytesList.toHexString()}".logE("xysLog")
                    isRecOK = false
                    //BleHelper.retryHistoryMessage(recordCommand,alarmCommand)
                    transcodingBytesList.clear()
                }
            }
        }
    }

    private fun analyseMessage(mBytes: ByteArray?) {
        mBytes?.let {
            when (it[4]) {
                //设备信息
                ByteUtils.MsgC0 -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsgC0(it)
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
                ByteUtils.Msg82 -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg82(it)
                    }
                }
                ByteUtils.Msg8D -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg8D(it)
                    }
                }
                ByteUtils.MsgC3 -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsgC3(it)
                    }
                }
                ByteUtils.Msg8E -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg8E(it)
                    }
                }
                ByteUtils.Msg8F -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg8F(it)
                    }
                }
                else->{}
            }
        }
    }

    private fun dealMsgC0(mBytes: ByteArray) {
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

                recall.initLocation()

            }
        }
    }

    private suspend fun dealMsg82(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 10) {
                if (it[7].toInt()==0){
                    synchronized(this) {
                        sendNum--
                    }
                    "发送映像文件请求,成功 sendNum: $sendNum".logE(logFlag)
                }
                else if (it[7].toInt()==1){
                    "发送映像文件请求,失败 sendNum:$sendNum".logE(logFlag)
                }
                //recall.initLocation()
            }
        }
    }

    private suspend fun dealMsgC3(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 9) {
                "收到C3成功".logE(logFlag)
                baudRate=921600
                isNeedNewInit=true
                val open = SerialPortHelper.portManager.open()
                "串口打开${if (open) "成功" else "失败"}".logE(logFlag)
                isNeedNewInit=false
                sendUIUpdateFile(uIPackageByte)
            }else{
                "收到C3错误".logE(logFlag)
            }
        }
    }

    private suspend fun dealMsg8D(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 9) {
                if (it[7].toInt()==0){
                    "设置STM的UI更新请求 响应成功".logE(logFlag)
                }
                else if (it[7].toInt()==1){
                    "设置STM的UI更新请求 响应失败".logE(logFlag)
                }else if (it[7].toInt()==2){
                    "设置STM的UI更新请求 响应等通知".logE(logFlag)
                }
            }
        }
    }

    private suspend fun dealMsg8E(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 9) {
                if (it[7].toInt()==1){
                    isRec0x01OK=true
                    uiRecNum++
                    "0x01成功".logE(logFlag)
                }else if (it[7].toInt()==5){
                    "0x05成功".logE(logFlag)
                    isRec0x05OK=true
                }
                else{
                    isRec0x01OK=false
                    isRec0x05OK=false
                }
            }
        }
    }

    private suspend fun dealMsg8F(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 9) {
                "发送UI映像文件结束 成功".logE(logFlag)
            }
        }
    }

    private suspend fun sendUIUpdateFile(byteArray: ByteArray){
        var mResultList=ByteArray(518)
        if (byteArray.size>512){
            var offsetIndex=0
            val mList=ByteArray(512)
            var j=0
            for (i in byteArray.indices){
                if (i!=0 && i%512==0){
                    if (i==1024){
                        delay(500)
                    }else{
                        delay(100)
                    }
                    val offSetByteArray= ByteArray(4)
                    offSetByteArray.writeInt32LE((i-512).toLong())
                    val dataLength= ByteArray(2)
                    dataLength.writeInt16LE(512)
                    mResultList=offSetByteArray+dataLength+mList
                    isRec0x01OK=false
                    while(true){
                        SerialPortHelper.sendUIUpdate(mResultList,mResultList.size+9,mResultList.size)
                        "UI分包： 总长度: ${byteArray.size} 发送进度： $i".logE(logFlag)
                        delay(2000)
                    }
                    offsetIndex=i
                    while (!isRec0x01OK){
                        delay(100)
                        "0x01等待中".logE(logFlag)
                    }
                    while (uiRecNum ==8 && !isRec0x05OK){
                        delay(100)
                        "0x05等待中".logE(logFlag)
                    }
                    uiRecNum=0
                    j=0
                    mList[j]=byteArray[i]
                    j++
                }else{
                    mList[j]=byteArray[i]
                    j++
                }
            }
            if (mList.isNotEmpty()){
                val mLastList=ByteArray(j)
                System.arraycopy(mList,0,mLastList,0,mLastList.size)
                val offSetByteArray= ByteArray(4)
                offSetByteArray.writeInt32LE((offsetIndex).toLong())
                val dataLength= ByteArray(2)
                dataLength.writeInt16LE(mLastList.size)
                mResultList=offSetByteArray+dataLength+mLastList

                SerialPortHelper.sendUIUpdate(mResultList,mResultList.size+9,mResultList.size)
                "UI last 总长度: ${byteArray.size} 发送长度： ${mResultList.size} : ${mResultList.toHexString()}".logE(logFlag)
            }
        }else{
            val offSetByteArray= ByteArray(4)
            offSetByteArray.writeInt32LE(0.toLong())
            val dataLength= ByteArray(2)
            dataLength.writeInt16LE(byteArray.size)
            mResultList=offSetByteArray+dataLength+byteArray
            SerialPortHelper.sendUIUpdate(mResultList,mResultList.size+9,mResultList.size)
            "UI不足512： last 总长度: ${byteArray.size} 发送长度： ${mResultList.size} : ${mResultList.toHexString()}".logE(logFlag)
        }

        //发送结束
        sendUIUpdateEnd(uIPackageByte)

        isNeedNewInit=true
        baudRate=115200
        val open = SerialPortHelper.portManager.open()
        isNeedNewInit=false
        delay(1000)
    }

    private fun sendUIUpdateEnd(byteArray: ByteArray){
        var checkSum=0L
        for (k in byteArray.indices){
            checkSum+=byteArray[k].toInt() and 0xff
        }
        "checkSum: $checkSum".logE(logFlag)
        val checkSumByte= ByteArray(4)
        checkSumByte.writeInt32LE(checkSum)
        SerialPortHelper.sendUIEndUpdate(checkSumByte)
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
        fun initLocation()
    }
}
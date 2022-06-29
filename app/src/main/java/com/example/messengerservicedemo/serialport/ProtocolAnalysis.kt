package com.example.messengerservicedemo.serialport

import com.example.messengerservicedemo.ext.*
import com.example.messengerservicedemo.serialport.model.Msg41DataModel
import com.example.messengerservicedemo.serialport.model.NfcModel
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

    private lateinit var sensorStatus: String
    private lateinit var voc: String
    private lateinit var dust: String
    private lateinit var temp: String
    private lateinit var dumity: String
    private lateinit var nfcMode1: NfcModel
    private lateinit var nfcMode2: NfcModel
    private lateinit var nfcMode3: NfcModel
    private lateinit var workPattern: String
    private lateinit var electricalMachinery: String
    private lateinit var disinfectionFunction: String
    private lateinit var bhState: String
    private lateinit var infraredState: String
    private lateinit var deviceState: String

    fun setUiCallback(dataCallback: ReceiveDataCallBack) {
        this.recall = dataCallback
    }

    @Synchronized
    fun addRecLinkedDeque(byte: Byte) {
        if (!recLinkedDeque.offer(byte)){
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

    private suspend fun analyseMessage(mBytes: ByteArray?) {
        mBytes?.let {
            when (it[4]) {
                //设备信息
                ByteUtils.Msg03 -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg03(it)
                    }
                }
                //设置设备工作模式响
                ByteUtils.Msg4F -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg4F(it)
                    }
                }
                //获取设备净化功能响应
                ByteUtils.Msg73 -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg73(it)
                    }
                }
                //设置设备净化数据响应
                ByteUtils.Msg57 -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg57(it)
                    }
                }
                //数据响应，通知
                ByteUtils.Msg41 -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg41(it)
                    }
                }
                //数据响应，通知
                ByteUtils.Msg51 -> {
                    scope.launch(Dispatchers.IO) {
                        dealMsg51(it)
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
                mmkv.putString(ValueKey.deviceHardwareVersion,it[7].toInt().toString()+":"+it[8].toInt().toString())
                mmkv.putString(ValueKey.deviceSoftwareVersion,it[9].toInt().toString()+":"+it[10].toInt().toString())
                //设备序列号
                var i = 11
                while (i < it.size)
                    if (it[i] == ByteUtils.FRAME_00) break else i++
                val tempBytes: ByteArray = it.readByteArrayBE(11, i - 11)
                mmkv.putString(ValueKey.deviceId,String(tempBytes))
                "设备信息响应成功: ${String(tempBytes)}".logE("xysLog")
            }
        }
    }

    private fun dealMsg4F(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 10) {
                if (it[7].toInt()==0)
                    "设备工作模式响应成功".logE("xysLog")
                else if (it[7].toInt()==1){
                    "设备工作模式响应失败".logE("xysLog")
                }
            }
        }
    }

    private fun dealMsg73(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 13) {
                val timing=it.readByteArrayBE(7, 2).readInt32LE().toString()
                val speed=it[9].toInt().toString()
                "设备净化功能响应成功: $timing,$speed".logE("xysLog")
            }
        }
    }

    private fun dealMsg57(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 10) {
                if (it[7].toInt()==0)
                    "设备消毒功能响应成功".logE("xysLog")
                else if (it[7].toInt()==1){
                    "设备消毒功能响应失败".logE("xysLog")
                }
            }
        }
    }

    private fun dealMsg51(mBytes: ByteArray) {
        mBytes.let {
            if (it.size == 10) {
                if (it[7].toInt()==0)
                    "设置设备净化数据响应成功".logE("xysLog")
                else if (it[7].toInt()==1){
                    "设置设备净化数据响应失败".logE("xysLog")
                }
            }
        }
    }

    private fun dealMsg41(mBytes: ByteArray) {
        mBytes.let {
            if (it.size > 77) {
                "实时数据解析成功: ${it.toHexString()}".logE("xysLog")
                //传感器数据
                if (it[7]== ByteUtils.Msg26){
                    sensorStatus=it[10].toInt().toString()
                    voc=it.readByteArrayBE(11, 4).readFloatLE().toInt().toString()
                    dust=String.format("%.2f", it.readByteArrayBE(15, 4).readFloatLE())
                    temp=it.readByteArrayBE(19, 4).readFloatLE().toInt().toString()
                    dumity=it.readByteArrayBE(23, 4).readFloatLE().toInt().toString()
                }
                //三个NFC数据
                if (it[27]== ByteUtils.Msg60){
                    val nfcStatus1=it[30].toInt().toString()
                    val num1=it[31].toInt().toString()
                    val userTime1=it[32].toInt().toString()
                    val reminder1=it[33].toInt().toString()
                    val sn1=it.readByteArrayBE(34, 4).readInt32LE().toString()
                    nfcMode1 = NfcModel(nfcStatus1,num1,userTime1,reminder1,sn1)

                    val nfcStatus2=it[38].toInt().toString()
                    val num2=it[39].toInt().toString()
                    val userTime2=it[40].toInt().toString()
                    val reminder2=it[41].toInt().toString()
                    val sn2=it.readByteArrayBE(42, 4).readInt32LE().toString()
                    nfcMode2 = NfcModel(nfcStatus2,num2,userTime2,reminder2,sn2)

                    val nfcStatus3=it[46].toInt().toString()
                    val num3=it[47].toInt().toString()
                    val userTime3=it[48].toInt().toString()
                    val reminder3=it[49].toInt().toString()
                    val sn3=it.readByteArrayBE(50, 4).readInt32LE().toString()
                    nfcMode3 = NfcModel(nfcStatus3,num3,userTime3,reminder3,sn3)

                }
                //设备工作模式
                if (it[54]== ByteUtils.Msg61){
                    workPattern=it[57].toInt().toString()
                }
                //电机状态
                if (it[58]== ByteUtils.Msg62){
                    electricalMachinery=it[61].toInt().toString()
                }
                //消毒功能
                if (it[62]== ByteUtils.Msg63){
                    disinfectionFunction=it[65].toInt().toString()
                }
                //童锁状态
                if (it[66]== ByteUtils.Msg64){
                    bhState=it[69].toInt().toString()
                }
                //红外遥控配对状态
                if (it[70]== ByteUtils.Msg65){
                    infraredState=it[73].toInt().toString()
                }
                //设备状态
                if (it[74]== ByteUtils.Msg66){
                    deviceState=it[77].toInt().toString()
                }

                val msg41DataModel= Msg41DataModel(sensorStatus,voc,dust,temp,dumity,nfcMode1,nfcMode2,nfcMode3,
                    workPattern,electricalMachinery,disinfectionFunction,bhState,infraredState,deviceState)

                recall.onDataReceive(msg41DataModel)
            }
        }
    }

    interface ReceiveDataCallBack {
        fun onDataReceive(msg41DataModel: Msg41DataModel)
    }
}
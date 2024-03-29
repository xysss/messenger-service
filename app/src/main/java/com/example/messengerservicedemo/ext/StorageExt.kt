package com.example.messengerservicedemo.ext

import android.os.Looper
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import java.util.concurrent.LinkedBlockingQueue


/**
 * 作者　: xys
 * 时间　: 2021/09/27
 * 描述　:
 */

/**
 * 获取MMKV
 */

//val dataRecordDao = AppDatabase.getDatabase().dataRecordDao()
//val dataAlarmDao = AppDatabase.getDatabase().dataAlarmDao()
//val dataMatterDao = AppDatabase.getDatabase().dataMatterDao()

val job= Job()
val scope = CoroutineScope(job)
var isRecOK=true
const val logFlag="xysLog"
const val buglyAppId="91953aa13a"

var sendNum= 0

var isRec8E01OK= false
var isRec8E05OK= false
var uiRecNum= 0
var baudRate=115200

var isNeedNewInit=false

//val recLinkedDeque=LinkedBlockingDeque<ByteArray>(1000000)
val recLinkedDeque= LinkedBlockingQueue<Byte>()  //默认情况下，该阻塞队列的大小为Integer.MAX_VALUE，由于这个数值特别大
val sendLinkedDeque=LinkedBlockingQueue<String>(1000)

val weatherHashMap:HashMap<String,Int> = HashMap()

val mmkv: MMKV by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    MMKV.mmkvWithID(ValueKey.MMKV_APP_KEY)
}

lateinit var uIPackageByte :ByteArray
lateinit var firmwarePackageByte :ByteArray
var binFileDirectory:String =""
var stm32HighVersion:Int=0
var stm32LowVersion:Int=0

fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread.id == Thread.currentThread().id
}
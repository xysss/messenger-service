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

//val recLinkedDeque=LinkedBlockingDeque<ByteArray>(1000000)
val recLinkedDeque= LinkedBlockingQueue<Byte>()  //默认情况下，该阻塞队列的大小为Integer.MAX_VALUE，由于这个数值特别大
val sendLinkedDeque=LinkedBlockingQueue<String>(1000)



val mmkv: MMKV by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    MMKV.mmkvWithID(ValueKey.MMKV_APP_KEY)
}

fun isMainThread(): Boolean {
    return Looper.getMainLooper().thread.id == Thread.currentThread().id
}
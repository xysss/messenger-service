package com.example.messengerservicedemo

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.lifecycle.liveData
import com.amap.api.location.AMapLocation
import com.example.messengerservicedemo.api.network.SunnyWeatherNetwork
import com.example.messengerservicedemo.ext.job
import com.example.messengerservicedemo.ext.logFlag
import com.example.messengerservicedemo.ext.scope
import com.example.messengerservicedemo.repository.WeatherRepository
import com.example.messengerservicedemo.response.Location
import com.example.messengerservicedemo.response.Place
import com.example.messengerservicedemo.response.Weather
import com.example.messengerservicedemo.service.ForegroundNF
import com.example.messengerservicedemo.util.AmapLocationUtil
import com.example.model.UserS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.hgj.mvvmhelper.base.appContext
import me.hgj.mvvmhelper.ext.logE
import java.lang.Exception
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext

/**
 * 作者 : xys
 * 时间 : 2022-06-06 10:50
 * 描述 : 描述
 */
class MessengerService : Service() {

    private lateinit var clientMsg: Message
    private lateinit var mForegroundNF: ForegroundNF
    private val mHandler = MyHandler(WeakReference(this))
    private lateinit var mMessenger: Messenger
    private var amapLocationUtil: AmapLocationUtil? = null

    companion object {
        const val WHAT1 = 1
        const val WHAT2 = 2
        const val WHAT3 = 3
    }


    override fun onCreate() {
        super.onCreate()
        mForegroundNF = ForegroundNF(this)
        mForegroundNF.startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            //服务被系统kill掉之后重启进来的
            return START_NOT_STICKY
        }
        mForegroundNF.startForegroundNotification()

        scope.launch(Dispatchers.IO) {
            "定位启动".logE(logFlag)
            //获取定位
            initLocationOption()
        }
        scope.launch(Dispatchers.Main) {
            "服务已经启动".logE(logFlag)

        }


        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mForegroundNF.stopForegroundNotification()
        job.cancel()
        super.onDestroy()
    }

    fun initLocationOption() {
        if (null == amapLocationUtil) {
            amapLocationUtil = AmapLocationUtil(appContext)
        }
        amapLocationUtil?.let {
            it.initLocation()
            it.startLocation()
            it.setOnCallBackListener(object : AmapLocationUtil.onCallBackListener {
                override fun onCallBack(
                    longitude: Double,
                    latitude: Double,
                    location: AMapLocation?,
                    isSucdess: Boolean,
                    address: String?
                ) {
                    Log.e("--->", "longitude $longitude\nlatitude $latitude\nisSucdess $isSucdess\naddress $address")
                    Log.e("--->",location?.province + "\n" +location?.city + "\n" +location?.district)
                    val location = Location(longitude.toString(),latitude.toString())
                    val place= Place(address?:"",location,address?:"")
                    "${place.address}: ${place.location.lat}: ${place.location.lat}".logE(logFlag)

                    if (isSucdess) {
                        "定位成功".logE(logFlag)
                        refreshWeather(location.lng,location.lat,place.address)

                    } else {
                        //定位失败，重试定位
                        it.startLocation()
                    }
                }
            })
        }
    }

    fun refreshWeather(lng: String, lat: String, placeName: String) = fire(Dispatchers.IO) {
        coroutineScope {
            val deferredRealtime = async {
                SunnyWeatherNetwork.getRealtimeWeather(lng, lat)
            }
            val deferredDaily = async {
                SunnyWeatherNetwork.getDailyWeather(lng, lat)
            }
            val realtimeResponse = deferredRealtime.await()
            val dailyResponse = deferredDaily.await()
            if (realtimeResponse.status == "ok" && dailyResponse.status == "ok") {
                val weather = Weather(realtimeResponse.result.realtime, dailyResponse.result.daily)
                weather.toString().logE(logFlag)
                Result.success(weather)
            } else {
                Result.failure(
                    RuntimeException(
                        "realtime response status is ${realtimeResponse.status}" +
                                "daily response status is ${dailyResponse.status}"
                    )
                )
            }
        }
    }

    fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData<Result<T>>(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            emit(result)
        }

    override fun onBind(intent: Intent): IBinder {
        Log.e("TAG", "onBind~")
        //传入Handler实例化Messenger
        mMessenger = Messenger(mHandler)
        //将Messenger中的binder返回给客户端,让它可以远程调用
        return mMessenger.binder
    }

    private class MyHandler(val wrActivity: WeakReference<MessengerService>) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            wrActivity.get()?.run {
                clientMsg=msg
                when (msg.what) {
                    WHAT1 -> {
                        //val person = msg.data.getParcelable("person") as Person?
                        //val user = acceptBundle.get("person") as Person
                        val userS=msg.data?.getSerializable("person")
                        //val person : Person? = acceptBundle.getParcelable("person")
                        Log.e("来自client的",userS.toString())

//                        val user = msg.data.get("user")
//                        Log.e("来自客户端的",user.toString())

//                        val date = msg.data.getString("data")
//                        Log.e("来自客户端的",date.toString())

                        //客户端的Messenger就是放在Message的replyTo中的
                        replyToClient(clientMsg)
                    }
                    WHAT2 ->{

                        replyToClient(clientMsg)
                    }
                    else -> super.handleMessage(msg)
                }
            }
        }
    }

    private fun replyToClient(msg: Message) {
        val clientMessenger = msg.replyTo
        val replyMessage = Message.obtain(null, WHAT1)
        val userS = UserS("小明", 25)
        replyMessage.data = Bundle().apply {
            putSerializable("person", userS)
            //putString("reply", replyText)
        }
        try {
            clientMessenger?.send(replyMessage)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}

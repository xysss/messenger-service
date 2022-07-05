package com.example.messengerservicedemo

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import com.amap.api.location.AMapLocation
import com.example.messengerservicedemo.network.SunnyWeatherNetwork
import com.example.messengerservicedemo.ext.job
import com.example.messengerservicedemo.ext.logFlag
import com.example.messengerservicedemo.ext.scope
import com.example.messengerservicedemo.response.Location
import com.example.messengerservicedemo.response.Place
import com.example.messengerservicedemo.response.Weather
import com.example.messengerservicedemo.serialport.ProtocolAnalysis
import com.example.messengerservicedemo.serialport.SerialPortHelper
import com.example.messengerservicedemo.serialport.model.SensorData
import com.example.messengerservicedemo.service.ForegroundNF
import com.example.messengerservicedemo.util.AmapLocationUtil
import com.example.model.UserS
import com.serial.port.kit.core.common.TypeConversion
import com.serial.port.manage.data.WrapReceiverData
import com.serial.port.manage.listener.OnDataPickListener
import com.swallowsonny.convertextlibrary.writeFloatLE
import com.swallowsonny.convertextlibrary.writeInt16LE
import com.swallowsonny.convertextlibrary.writeInt32LE
import com.swallowsonny.convertextlibrary.writeInt8
import kotlinx.coroutines.*
import me.hgj.mvvmhelper.base.appContext
import me.hgj.mvvmhelper.ext.logE
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

/**
 * 作者 : xys
 * 时间 : 2022-06-06 10:50
 * 描述 : 描述
 */
class MessengerService : Service(),ProtocolAnalysis.ReceiveDataCallBack{

    private lateinit var clientMsg: Message
    private lateinit var mForegroundNF: ForegroundNF
    private val mHandler = MyHandler(WeakReference(this))
    private lateinit var mMessenger: Messenger
    private var amapLocationUtil: AmapLocationUtil? = null
    private val protocolAnalysis = ProtocolAnalysis()
    private var age=0

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

        //注册回调
        protocolAnalysis.setUiCallback(this)

        mForegroundNF.startForegroundNotification()

        scope.launch(Dispatchers.IO) {
            "定位启动".logE(logFlag)
            //获取定位
            initLocationOption()

            // 增加统一监听回调
            SerialPortHelper.portManager.addDataPickListener(onDataPickListener)
            scope.launch(Dispatchers.IO) {
                protocolAnalysis.startDealMessage()
            }

            // 打开串口
            if (!SerialPortHelper.portManager.isOpenDevice) {
                val open = SerialPortHelper.portManager.open()
                "串口打开${if (open) "成功" else "失败"}".logE(logFlag)

                //传感器信息读取请求
                SerialPortHelper.getSensorInfo()
                //传感器信息数据
                SerialPortHelper.getSensorData()

            }
        }
//        scope.launch(Dispatchers.Main) {
//
//        }
        return super.onStartCommand(intent, flags, startId)
    }

    private val onDataPickListener: OnDataPickListener = object : OnDataPickListener {
        override fun onSuccess(data: WrapReceiverData) {
            "统一响应数据：${TypeConversion.bytes2HexString(data.data)}".logE(logFlag)

            scope.launch(Dispatchers.IO) {
                for (byte in data.data)
                    protocolAnalysis.addRecLinkedDeque(byte)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mForegroundNF.stopForegroundNotification()
        //取消协程
        job.cancel()
        // 移除统一监听回调
        SerialPortHelper.portManager.removeDataPickListener(onDataPickListener)
        // 关闭串口
        val close = SerialPortHelper.portManager.close()
        "串口关闭${if (close) "成功" else "失败"}".logE(logFlag)
        // 销毁定位
        amapLocationUtil?.let { it.destroyLocation() }
    }

    private fun initLocationOption() {
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

//    fun sendRequest() {
//        scope.launch(Dispatchers.Main){
//            //当前运行在协程中，且在主线程运行
//            val asyncBanner = getBanners(this) //这里返回Deferred<List<Banner>>对象
//            val asyncPersons = getStudents(this) //这里返回Deferred<List<Student>>对象
//            val banners = asyncBanner.await()           //这里返回List<Banner>对象
//            val students = asyncPersons.await()         //这里返回List<Student>对象
//            //开始更新UI
//
//        }
//    }


//    //挂断方法，获取学生信息
//    suspend fun getBanners(scope: CoroutineScope): Deferred<List<Banner>> {
//        return RxHttp.get("/service/...")
//            .add("key", "value")
//            .addHeader("headKey", "headValue")
//            .toClass<List<Banner>>()
//            .async(scope)  //注意这里使用async异步操作符
//    }
//
//    //挂断方法，获取家庭成员信息
//    suspend fun getStudents(scope: CoroutineScope): Deferred<List<Student>> {
//        return RxHttp.get("/service/...")
//            .add("key", "value")
//            .toClass<List<Student>>()
//            .async(scope) //注意这里使用async异步操作符
//    }


    fun refreshWeather(lng: String, lat: String, placeName: String){
        scope.launch(Dispatchers.IO){
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
                showWeatherInfo(weather)
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

    private fun showWeatherInfo(weather: Weather) {
        val realtime = weather.realtime
        val daily = weather.daily
        // 填充now.xml布局中数据
        //温度
        val currentTempText = "${realtime.temperature.toInt()} ℃".logE(logFlag)
        //湿度
        val humidityText = "湿度: ${realtime.humidity} %".logE(logFlag)
        //风速
        val windSpeedText = "风速: ${realtime.wind.speed}公里/每小时".logE(logFlag)
        //
        val windDirection = "地表 10 米风向: ${realtime.wind.direction}".logE(logFlag)
        //能见度
        val visibilityText="能见度: ${realtime.visibility}公里".logE(logFlag)
        val cloudrate="总云量: ${realtime.cloudrate}".logE(logFlag)
        //天气
        val skyconText="${realtime.skycon}".logE(logFlag)
//        val sky1 = getSky(skyconText)
//        mViewBinding.image1One.setImageResource(sky1.icon)

        //mViewBinding.nowInclude.currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "空气指数 ${realtime.airQuality.aqi.chn.toInt()}".logE(logFlag)
        //mViewBinding.nowInclude.currentAQI.text = currentPM25Text
        //mViewBinding.nowInclude.nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)
        // 填充forecast.xml布局中的数据
        val days = daily.skycon.size
        for (i in 1 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]

            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            "${temperature.min.toInt()} ~ ${temperature.max.toInt()} ℃".logE(logFlag)
        }
        // 填充life_index.xml布局中的数据
        val lifeIndex = daily.lifeIndex
        //mViewBinding.lifeIndexInclude.coldRiskText.text = lifeIndex.coldRisk[0].desc
        //mViewBinding.lifeIndexInclude.dressingText.text = lifeIndex.dressing[0].desc
        //mViewBinding.tV5One.text = "紫外线指数: ${lifeIndex.ultraviolet[0].index}最大(10)"
        "紫外线指数: ${lifeIndex.ultraviolet[0].desc}".logE(logFlag)
        //mViewBinding.lifeIndexInclude.carWashingText.text = lifeIndex.carWashing[0].desc

        val weatherIdByte = ByteArray(1)
        weatherIdByte.writeInt8(10)

        val airApiByte = ByteArray(2)
        airApiByte.writeInt16LE(realtime.airQuality.aqi.chn.toInt())

        val maxTempByte = ByteArray(4)
        maxTempByte.writeFloatLE(daily.temperature[0].max)

        val minTempByte = ByteArray(4)
        minTempByte.writeFloatLE(daily.temperature[0].min)

        val nowTempByte = ByteArray(4)
        nowTempByte.writeFloatLE(realtime.temperature)

        val humidityByte= ByteArray(4)
        humidityByte.writeFloatLE(realtime.humidity)

        val windDirectionByte= ByteArray(4)
        windDirectionByte.writeFloatLE(realtime.wind.direction)

        val windSpeedByte= ByteArray(4)
        windSpeedByte.writeFloatLE(realtime.wind.speed)

        val cloudrateByte= ByteArray(4)
        cloudrateByte.writeFloatLE(realtime.cloudrate)

        val rainfallByte= ByteArray(4)
        rainfallByte.writeFloatLE(realtime.cloudrate)

        val unitByte= ByteArray(4)
        unitByte.writeFloatLE(realtime.cloudrate)

        val weatherByteArray=weatherIdByte+airApiByte+maxTempByte+minTempByte+
                nowTempByte+humidityByte+windDirectionByte+windSpeedByte+cloudrateByte+rainfallByte+unitByte
        //发送天气数据
        SerialPortHelper.sendWeatherData(weatherByteArray)
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
        val userS = UserS("小明", age++)
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

    override fun onDataReceive(sensorData: SensorData) {

    }
}

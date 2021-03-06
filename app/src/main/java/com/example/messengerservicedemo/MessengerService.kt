package com.example.messengerservicedemo

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.*
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.amap.api.location.AMapLocation
import com.example.messengerservicedemo.api.NetUrl
import com.example.messengerservicedemo.broadcast.BootCompleteMyReceiver
import com.example.messengerservicedemo.ext.*
import com.example.messengerservicedemo.network.SunnyWeatherNetwork
import com.example.messengerservicedemo.network.manager.NetState
import com.example.messengerservicedemo.network.manager.NetworkStateManager
import com.example.messengerservicedemo.network.manager.NetworkStateReceive
import com.example.messengerservicedemo.response.Location
import com.example.messengerservicedemo.response.Place
import com.example.messengerservicedemo.response.Weather
import com.example.messengerservicedemo.serialport.ProtocolAnalysis
import com.example.messengerservicedemo.serialport.SerialPortHelper
import com.example.messengerservicedemo.serialport.model.SensorData
import com.example.messengerservicedemo.service.ForegroundNF
import com.example.messengerservicedemo.util.AmapLocationUtil
import com.example.messengerservicedemo.util.Android10DownloadFactory
import com.example.messengerservicedemo.util.UriUtils
import com.example.model.ComHubData
import com.serial.port.kit.core.common.TypeConversion
import com.serial.port.manage.data.WrapReceiverData
import com.serial.port.manage.listener.OnDataPickListener
import com.swallowsonny.convertextlibrary.writeFloatLE
import com.swallowsonny.convertextlibrary.writeInt16LE
import com.swallowsonny.convertextlibrary.writeInt8
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import me.hgj.mvvmhelper.base.appContext
import me.hgj.mvvmhelper.ext.logE
import me.hgj.mvvmhelper.ext.msg
import rxhttp.toFlow
import rxhttp.wrapper.entity.Progress
import rxhttp.wrapper.param.RxHttp
import java.io.File
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


/**
 * ?????? : xys
 * ?????? : 2022-06-06 10:50
 * ?????? : ??????
 */
class MessengerService : Service(),ProtocolAnalysis.ReceiveDataCallBack, LifecycleOwner {

    private lateinit var clientMsg: Message
    private lateinit var mForegroundNF: ForegroundNF
    private val mHandler = MyHandler(WeakReference(this))
    private lateinit var mMessenger: Messenger
    private var amapLocationUtil: AmapLocationUtil? = null
    private val protocolAnalysis = ProtocolAnalysis()
    private var age=0
    private var mLifecycleRegistry =  LifecycleRegistry(this)
    private var netWorkReceiver: NetworkStateReceive? = null

    companion object {
        const val WHAT1 = 1
        const val WHAT2 = 2
        const val WHAT3 = 3
    }


    override fun onCreate() {
        super.onCreate()
        mForegroundNF = ForegroundNF(this)
        mForegroundNF.startForegroundNotification()
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            //???????????????kill????????????????????????
            return START_NOT_STICKY
        }

        //????????????
        protocolAnalysis.setUiCallback(this)

        mForegroundNF.startForegroundNotification()

        scope.launch(Dispatchers.IO) {

//            val localComponentName = ComponentName(
//                appContext,
//                BootCompleteMyReceiver::class.java
//            )
//            val i: Int = application.packageManager.getComponentEnabledSetting(localComponentName)
//            getAutostartSettingIntent()

            "????????????".logE(logFlag)
            //????????????
            initLocationOption()

            // ????????????????????????
            SerialPortHelper.portManager.addDataPickListener(onDataPickListener)
            //????????????????????????
            protocolAnalysis.startDealMessage()

            // ????????????
            if (!SerialPortHelper.portManager.isOpenDevice) {
                val open = SerialPortHelper.portManager.open()
                "????????????${if (open) "??????" else "??????"}".logE(logFlag)
                //???????????????????????????
                SerialPortHelper.getSensorInfo()
                //?????????????????????
                SerialPortHelper.getSensorData()
            }
        }

        //????????????????????????????????????
        netWorkReceiver = NetworkStateReceive()
        application.registerReceiver(
            netWorkReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        val filter = IntentFilter()
        filter.apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        }
        registerReceiver(netWorkReceiver, filter)


        downLoad({
            //?????????
            "???????????????${it.progress}%".logE(logFlag)
        }, {
            //????????????
            "???????????????????????????${it}".logE(logFlag)
        }, {
            //????????????
            it.msg.logE(logFlag)
        })

        //????????????
        NetworkStateManager.instance.mNetworkStateCallback.observe(this){
            onNetworkStateChanged(it)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * ??????????????????????????????Intent
     *
     * @param context context
     * @return ??????????????????????????????Intent
     */
    fun getAutostartSettingIntent(): Intent {
        var componentName: ComponentName? = null
        val brand = Build.MANUFACTURER
        val intent = Intent()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        when (brand.lowercase(Locale.getDefault())) {
            "samsung" -> componentName = ComponentName(
                "com.samsung.android.sm",
                "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity"
            )
            "huawei" -> {
                Log.e("??????????????? >>>>", "getAutostartSettingIntent: ??????")
                componentName = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                )
            }
            "xiaomi" -> //                componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                componentName = ComponentName(
                    "com.android.settings",
                    "com.android.settings.BackgroundApplicationsManager"
                )
            "vivo" -> //            componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.safaguard.PurviewTabActivity");
                componentName = ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            "oppo" -> //            componentName = new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity");
                componentName = ComponentName(
                    "com.coloros.oppoguardelf",
                    "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
                )
            "yulong", "360" -> componentName = ComponentName(
                "com.yulong.android.coolsafe",
                "com.yulong.android.coolsafe.ui.activity.autorun.AutoRunListActivity"
            )
            "meizu" -> componentName =
                ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity")
            "oneplus" -> componentName = ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
            "letv" -> {
                intent.action = "com.letv.android.permissionautoboot"
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                intent.data = Uri.fromParts("package", appContext.getPackageName(), null)
            }
            else -> {
                intent.action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                intent.data = Uri.fromParts("package", appContext.getPackageName(), null)
            }
        }
        intent.component = componentName
        return intent
    }


    fun downLoad(downLoadData: (Progress) -> Unit = {}, downLoadSuccess: (String) -> Unit, downLoadError: (Throwable) -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            if (checkedAndroid_Q()) {
                //android 10 ??????
                val factory = Android10DownloadFactory(appContext, "${System.currentTimeMillis()}.apk")
                RxHttp.get(NetUrl.DOWNLOAD_URL)
                    .toFlow(factory) {
                        downLoadData.invoke(it)
                    }.catch {
                        //????????????
                        downLoadError(it)
                    }.collect {
                        //????????????
                        downLoadSuccess.invoke(UriUtils.getFileAbsolutePath(appContext,it)?:"")
                    }
            } else {
                //android 10??????
                val localPath = appContext.externalCacheDir!!.absolutePath + "/${System.currentTimeMillis()}.apk"
                RxHttp.get(NetUrl.DOWNLOAD_URL)
                    .toFlow(localPath) {
                        downLoadData.invoke(it)
                    }.catch {
                        //????????????
                        downLoadError(it)
                    }.collect {
                        //????????????
                        downLoadSuccess.invoke(it)
                    }
            }
        }
    }

    fun upload(filePath: String, uploadData: (Progress) -> Unit = {}, uploadSuccess: (String) -> Unit, uploadError: (Throwable) -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            if (checkedAndroid_Q() && filePath.startsWith("content:")) {
                //android 10 ??????
                RxHttp.postForm(NetUrl.UPLOAD_URL)
                    .addPart(appContext, "apkFile", Uri.parse(filePath))
                    .toFlow<String> {
                        //??????????????????,0-100???????????????????????????????????????
                        uploadData.invoke(it)
                    }.catch {
                        //????????????
                        uploadError.invoke(it)
                    }.collect {
                        //????????????
                        uploadSuccess.invoke(it)
                    }
            } else {
                // android 10??????
                val file = File(filePath)
                if(!file.exists()){
                    uploadError.invoke(Exception("???????????????"))
                    return@launch
                }
                RxHttp.postForm(NetUrl.UPLOAD_URL)
                    .addFile("apkFile", file)
                    .toFlow<String> {
                        //??????????????????,0-100???????????????????????????????????????
                        uploadData.invoke(it)
                    }.catch {
                        //????????????
                        uploadError.invoke(it)
                    }.collect {
                        //????????????
                        uploadSuccess.invoke(it)
                    }
            }
        }
    }

    private fun checkedAndroid_Q(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * ????????????Activity/Fragment??????????????????????????????????????????onNetworkStateChanged?????????
     */
    private fun onNetworkStateChanged(netState: NetState) {
        if (netState.isSuccess) {
            //ToastUtils.showShort("???????????????!")
            "???????????????!Service".logE(logFlag)
            if (!mmkv.getBoolean(ValueKey.isFirstInitSuccess,false)){
                //????????????
                initLocationOption()
            }
        } else {
            //ToastUtils.showShort("???????????????!")
            "???????????????!Service".logE(logFlag)
        }
    }

    private val onDataPickListener: OnDataPickListener = object : OnDataPickListener {
        override fun onSuccess(data: WrapReceiverData) {
            "?????????????????????${TypeConversion.bytes2HexString(data.data)}".logE(logFlag)

            scope.launch(Dispatchers.IO) {
                for (byte in data.data)
                    protocolAnalysis.addRecLinkedDeque(byte)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        application.unregisterReceiver(netWorkReceiver)

        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        mForegroundNF.stopForegroundNotification()
        //????????????
        job.cancel()
        // ????????????????????????
        SerialPortHelper.portManager.removeDataPickListener(onDataPickListener)
        // ????????????
        val close = SerialPortHelper.portManager.close()
        "????????????${if (close) "??????" else "??????"}".logE(logFlag)
        // ????????????
        amapLocationUtil?.let { it.destroyLocation() }

        //????????????service
        val intent = Intent(this,MessengerService::class.java)
        startService(intent)
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
                        "????????????".logE(logFlag)
                        refreshWeather(location.lng,location.lat,place.address)
                        mmkv.putBoolean(ValueKey.isFirstInitSuccess,true)
                    } else {
                        //???????????????????????????
                        //it.startLocation()
                        mmkv.putBoolean(ValueKey.isFirstInitSuccess,false)
                    }
                }
            })
        }
    }

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
        // ??????now.xml???????????????
        //??????
        val currentTempText = "${realtime.temperature.toInt()} ???".logE(logFlag)
        //??????
        val humidityText = "??????: ${realtime.humidity} %".logE(logFlag)
        //??????
        val windSpeedText = "??????: ${realtime.wind.speed}??????/?????????".logE(logFlag)
        //
        val windDirection = "?????? 10 ?????????: ${realtime.wind.direction}".logE(logFlag)
        //?????????
        val visibilityText="?????????: ${realtime.visibility}??????".logE(logFlag)
        val cloudrate="?????????: ${realtime.cloudrate}".logE(logFlag)
        //??????
        val skyconText="${realtime.skycon}".logE(logFlag)
//        val sky1 = getSky(skyconText)
//        mViewBinding.image1One.setImageResource(sky1.icon)

        //mViewBinding.nowInclude.currentSky.text = getSky(realtime.skycon).info
        val currentPM25Text = "???????????? ${realtime.airQuality.aqi.chn.toInt()}".logE(logFlag)
        //mViewBinding.nowInclude.currentAQI.text = currentPM25Text
        //mViewBinding.nowInclude.nowLayout.setBackgroundResource(getSky(realtime.skycon).bg)
        // ??????forecast.xml??????????????????
        val days = daily.skycon.size
        for (i in 1 until days) {
            val skycon = daily.skycon[i]
            val temperature = daily.temperature[i]

            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            "${temperature.min.toInt()} ~ ${temperature.max.toInt()} ???".logE(logFlag)
        }
        // ??????life_index.xml??????????????????
        val lifeIndex = daily.lifeIndex
        //mViewBinding.lifeIndexInclude.coldRiskText.text = lifeIndex.coldRisk[0].desc
        //mViewBinding.lifeIndexInclude.dressingText.text = lifeIndex.dressing[0].desc
        //mViewBinding.tV5One.text = "???????????????: ${lifeIndex.ultraviolet[0].index}??????(10)"
        "???????????????: ${lifeIndex.ultraviolet[0].desc}".logE(logFlag)
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
        //??????????????????
        //SerialPortHelper.sendWeatherData(weatherByteArray)




    }
    override fun onBind(intent: Intent): IBinder {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        Log.e("TAG", "onBind~")
        //??????Handler?????????Messenger
        mMessenger = Messenger(mHandler)
        //???Messenger??????binder??????????????????,????????????????????????
        return mMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        return super.onUnbind(intent)
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
                        val comHubData=msg.data?.getSerializable("ComHubData")
                        //val person : Person? = acceptBundle.getParcelable("person")
                        Log.e("??????client???",comHubData.toString())

//                        val user = msg.data.get("user")
//                        Log.e("??????????????????",user.toString())

//                        val date = msg.data.getString("data")
//                        Log.e("??????????????????",date.toString())

                        //????????????Messenger????????????Message???replyTo??????
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
        val comHubData = ComHubData("??????", age++)
        replyMessage.data = Bundle().apply {
            putSerializable("ComHubData", comHubData)
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

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }
}

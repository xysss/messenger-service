package com.example.messengerservicedemo

import ZtlApi.ZtlManager
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
import com.swallowsonny.convertextlibrary.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import me.hgj.mvvmhelper.base.appContext
import me.hgj.mvvmhelper.ext.logE
import rxhttp.toFlow
import rxhttp.wrapper.entity.Progress
import rxhttp.wrapper.param.RxHttp
import java.io.File
import java.io.InputStream
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import kotlin.experimental.and


/**
 * 作者 : xys
 * 时间 : 2022-06-06 10:50
 * 描述 : 描述
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

        weatherHashMap.clear()

        weatherHashMap.put("CLEAR_DAY",0)
        weatherHashMap.put("CLEAR_NIGHT",1)
        weatherHashMap.put("PARTLY_CLOUDY_DAY",2)
        weatherHashMap.put("PARTLY_CLOUDY_NIGHT",3)
        weatherHashMap.put("CLOUDY",4)
        weatherHashMap.put("LIGHT_HAZE",5)
        weatherHashMap.put("MODERATE_HAZE",6)
        weatherHashMap.put("HEAVY_HAZE",7)
        weatherHashMap.put("LIGHT_RAIN",8)
        weatherHashMap.put("MODERATE_RAIN",9)
        weatherHashMap.put("HEAVY_RAIN",10)
        weatherHashMap.put("STORM_RAIN",11)
        weatherHashMap.put("FOG",12)
        weatherHashMap.put("LIGHT_SNOW",13)
        weatherHashMap.put("MODERATE_SNOW",14)
        weatherHashMap.put("HEAVY_SNOW",15)
        weatherHashMap.put("STORM_SNOW",16)
        weatherHashMap.put("DUST",17)
        weatherHashMap.put("SAND",18)
        weatherHashMap.put("WIND",19)

        ZtlManager.GetInstance().setContext(this)

        scope.launch {
            delay(10000)
            //sendUpdate()
            setUIReq()
        }
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (null == intent) {
            //服务被系统kill掉之后重启进来的
            return START_NOT_STICKY
        }

        //注册回调
        protocolAnalysis.setUiCallback(this)

        mForegroundNF.startForegroundNotification()

        //bugly进入首页检查更新
        //Beta.checkUpgrade(false, true)

        scope.launch(Dispatchers.IO) {
            //开始处理串口信息
            protocolAnalysis.startDealMessage()
        }

        scope.launch(Dispatchers.IO) {
            //开始轮训gpio值
            protocolAnalysis.reqGpio()
        }

        // 打开串口
        if (!SerialPortHelper.portManager.isOpenDevice) {
            val open = SerialPortHelper.portManager.open()
            "串口打开${if (open) "成功" else "失败"}".logE(logFlag)
            //传感器信息读取请求
            //SerialPortHelper.getSensorInfo()
            //传感器信息数据
            //SerialPortHelper.getSensorData()
        }else{
            "串口已经打开".logE(logFlag)
        }

        // 增加统一监听回调
        SerialPortHelper.portManager.addDataPickListener(onDataPickListener)

        //动态注册网络状态监听广播
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

        //网络监听
        NetworkStateManager.instance.mNetworkStateCallback.observe(this){
            onNetworkStateChanged(it)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * 获取自启动管理页面的Intent
     *
     * @param context context
     * @return 返回自启动管理页面的Intent
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
                Log.e("自启动管理 >>>>", "getAutostartSettingIntent: 华为")
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


    private fun downLoad(downLoadData: (Progress) -> Unit = {}, downLoadSuccess: (String) -> Unit, downLoadError: (Throwable) -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            if (checkedAndroid_Q()) {
                //android 10 以上
                val factory = Android10DownloadFactory(appContext, "${System.currentTimeMillis()}.apk")
                RxHttp.get(NetUrl.DOWNLOAD_URL)
                    .toFlow(factory) {
                        downLoadData.invoke(it)
                    }.catch {
                        //异常回调
                        downLoadError(it)
                    }.collect {
                        //成功回调
                        downLoadSuccess.invoke(UriUtils.getFileAbsolutePath(appContext,it)?:"")
                    }
            } else {
                //android 10以下
                val localPath = appContext.externalCacheDir!!.absolutePath + "/${System.currentTimeMillis()}.apk"
                RxHttp.get(NetUrl.DOWNLOAD_URL)
                    .toFlow(localPath) {
                        downLoadData.invoke(it)
                    }.catch {
                        //异常回调
                        downLoadError(it)
                    }.collect {
                        //成功回调
                        downLoadSuccess.invoke(it)
                    }
            }
        }
    }

    fun upload(filePath: String, uploadData: (Progress) -> Unit = {}, uploadSuccess: (String) -> Unit, uploadError: (Throwable) -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            if (checkedAndroid_Q() && filePath.startsWith("content:")) {
                //android 10 以上
                RxHttp.postForm(NetUrl.UPLOAD_URL)
                    .addPart(appContext, "apkFile", Uri.parse(filePath))
                    .toFlow<String> {
                        //上传进度回调,0-100，仅在进度有更新时才会回调
                        uploadData.invoke(it)
                    }.catch {
                        //异常回调
                        uploadError.invoke(it)
                    }.collect {
                        //成功回调
                        uploadSuccess.invoke(it)
                    }
            } else {
                // android 10以下
                val file = File(filePath)
                if(!file.exists()){
                    uploadError.invoke(Exception("文件不存在"))
                    return@launch
                }
                RxHttp.postForm(NetUrl.UPLOAD_URL)
                    .addFile("apkFile", file)
                    .toFlow<String> {
                        //上传进度回调,0-100，仅在进度有更新时才会回调
                        uploadData.invoke(it)
                    }.catch {
                        //异常回调
                        uploadError.invoke(it)
                    }.collect {
                        //成功回调
                        uploadSuccess.invoke(it)
                    }
            }
        }
    }

    private fun checkedAndroid_Q(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    /**
     * 示例，在Activity/Fragment中如果想监听网络变化，可重写onNetworkStateChanged该方法
     */
    private fun onNetworkStateChanged(netState: NetState) {
        val netStateByte = ByteArray(1)

        if (netState.isSuccess) {
            mmkv.putBoolean(ValueKey.isNetworking,true)
            "终于有网了!Service".logE(logFlag)

            //获取定位
            initLocationOption()
            netStateByte.writeInt8(1)

            val calendar = Calendar.getInstance()
            val year = calendar[1]
            val monthOfYear = calendar[2] + 1
            val dayOfMonth = calendar[5]
            val hour = calendar[11]
            val minute = calendar[12]
            val second = calendar[13]
            //传递时间
            val yearByte= ByteArray(1)
            yearByte.writeInt8(year-2000)
            val monthOfYearByte= ByteArray(1)
            monthOfYearByte.writeInt8(monthOfYear)
            val dayOfMonthByte= ByteArray(1)
            dayOfMonthByte.writeInt8(dayOfMonth)
            val hourByte= ByteArray(1)
            hourByte.writeInt8(hour)
            val minuteByte= ByteArray(1)
            minuteByte.writeInt8(minute)
            val secondByte= ByteArray(1)
            secondByte.writeInt8(second)
            val timeByteArray = yearByte+monthOfYearByte+dayOfMonthByte+hourByte+minuteByte+secondByte
            SerialPortHelper.sendTime(timeByteArray)

        } else {
            mmkv.putBoolean(ValueKey.isNetworking,false)
            "网络无连接!Service".logE(logFlag)
            netStateByte.writeInt8(0)
        }
        //发送网络状态
        SerialPortHelper.sendNetState(netStateByte)

//        val deviceSensorState = ByteArray(1)
//        deviceSensorState.writeInt8(0)  //0 关闭  1 启动
//        //停止主动上报
//        SerialPortHelper.setDeviceSensorState(deviceSensorState)
    }

    private suspend fun setUIReq(){
        val fileName = appContext.getExternalFilesDir("apk/jidinghe.tft").toString()
        val myFile = File(fileName)
        val ins: InputStream = myFile.inputStream()
        uIPackageByte = ins.readBytes()
        "localPath: $fileName".logE(logFlag)

        val softwareVersion= ByteArray(1)
        softwareVersion.writeInt8(0)
        val hardwareVersion= ByteArray(1)
        hardwareVersion.writeInt8(0)
        val fwLength= ByteArray(4)
        fwLength.writeInt32LE(uIPackageByte.size.toLong())
        val beginSize=softwareVersion + hardwareVersion + fwLength

        SerialPortHelper.sendUIReq(beginSize)
    }

    private suspend fun sendUpdate(){
//        downLoad({
//            //下载中
//            "下载进度：${it.progress}%".logE(logFlag)
//        }, {
//            //下载完成
//            "下载成功，路径为：${it}".logE(logFlag)
//        }, {
//            //下载失败
//            it.msg.logE(logFlag)
//        })

        val fileName = appContext.getExternalFilesDir("apk/stb.bin").toString()
        val myFile = File(fileName)
        val ins: InputStream = myFile.inputStream()
        val packageByte = ins.readBytes()
        "localPath: $fileName".logE(logFlag)

        val softwareVersion= ByteArray(1)
        softwareVersion.writeInt8(0)
        val hardwareVersion= ByteArray(1)
        hardwareVersion.writeInt8(0)
        val fwLength= ByteArray(4)
        fwLength.writeInt32LE(packageByte.size.toLong())
        val beginSize=softwareVersion + hardwareVersion + fwLength

        SerialPortHelper.sendBeginUpdate(beginSize)
        delay(200)

        sendUpdateFile(packageByte)
    }

    private suspend fun sendUpdateFile(byteArray: ByteArray){
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
                    SerialPortHelper.sendUpdate(mResultList,mResultList.size+9,mResultList.size)
                    //"update分包： 总长度: ${byteArray.size} 发送进度： $i  长度：: ${mResultList.toHexString()}}".logE(logFlag)
                    j=0
                    offsetIndex=i

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

                SerialPortHelper.sendUpdate(mResultList,mResultList.size+9,mResultList.size)
                "update last 总长度: ${byteArray.size} 发送长度： ${mResultList.size} : ${mResultList.toHexString()}".logE(logFlag)
            }
        }else{
            val offSetByteArray= ByteArray(4)
            offSetByteArray.writeInt32LE(0.toLong())
            val dataLength= ByteArray(2)
            dataLength.writeInt16LE(byteArray.size)
            mResultList=offSetByteArray+dataLength+byteArray

            SerialPortHelper.sendUpdate(mResultList,mResultList.size+9,mResultList.size)
            "update不足512： last 总长度: ${byteArray.size} 发送长度： ${mResultList.size} : ${mResultList.toHexString()}".logE(logFlag)
        }

        var checkSum=0L
        for (k in byteArray.indices){
            checkSum+=byteArray[k].toInt() and 0xff
        }
        "checkSum: $checkSum".logE(logFlag)
        val checkSumByte= ByteArray(4)
        checkSumByte.writeInt32LE(checkSum)
        SerialPortHelper.sendEndUpdate(checkSumByte)
    }

    private val onDataPickListener: OnDataPickListener = object : OnDataPickListener {
        override fun onSuccess(data: WrapReceiverData) {
            "统一响应数据：长度： ${data.data.size} :${TypeConversion.bytes2HexString(data.data)}".logE("串口")
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
        //取消协程
        job.cancel()
        // 移除统一监听回调
        SerialPortHelper.portManager.removeDataPickListener(onDataPickListener)
        // 关闭串口
        val close = SerialPortHelper.portManager.close()
        "串口关闭${if (close) "成功" else "失败"}".logE(logFlag)
        // 销毁定位
        amapLocationUtil?.let { it.destroyLocation() }

        //销毁重启service
//        val intent = Intent(this,MessengerService::class.java)
//        startService(intent)
    }

    private fun initLocationOption() {
        if (null == amapLocationUtil) {
            amapLocationUtil = AmapLocationUtil(appContext)
        }else{
            amapLocationUtil=null
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
                        mmkv.putBoolean(ValueKey.isFirstInitSuccess,true)
                    } else {
                        //定位失败，重试定位
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
                val weather = Weather(realtimeResponse.result.realtime, dailyResponse.result.daily,realtimeResponse.result.alert)
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

    private suspend fun showWeatherInfo(weather: Weather) {
        val realtime = weather.realtime
        val daily = weather.daily
        val alert = weather.alert
        // 填充now.xml布局中数据

        "天气：${realtime.skycon}".logE(logFlag)
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
        "天级别紫外线指数: ${lifeIndex.ultraviolet[0].index}".logE(logFlag)
        //mViewBinding.lifeIndexInclude.carWashingText.text = lifeIndex.carWashing[0].desc

        val weatherIdByte = ByteArray(1)
        weatherIdByte.writeInt8(weatherHashMap[realtime.skycon]?:0)
        "天气ID: ${weatherHashMap[realtime.skycon]}".logE(logFlag)

        val airApiByte = ByteArray(2)
        airApiByte.writeInt16LE(realtime.airQuality.aqi.chn.toInt())
        "空气指数: ${realtime.airQuality.aqi.chn.toInt()}".logE(logFlag)

        val ultravioletByte = ByteArray(1)
        ultravioletByte.writeInt8(realtime.lifeIndex.ultraviolet.index)
        "实时紫外线指数: ${realtime.lifeIndex.ultraviolet.index}".logE(logFlag)

        val maxTempByte = ByteArray(4)
        maxTempByte.writeFloatLE(daily.temperature[0].max)
        "最高温度: ${daily.temperature[0].max}".logE(logFlag)

        val minTempByte = ByteArray(4)
        minTempByte.writeFloatLE(daily.temperature[0].min)
        "最低温度: ${daily.temperature[0].min}".logE(logFlag)

        val nowTempByte = ByteArray(4)
        nowTempByte.writeFloatLE(realtime.temperature)
        "当前温度:${realtime.temperature}".logE(logFlag)

        val humidityByte= ByteArray(4)
        humidityByte.writeFloatLE(realtime.humidity)
        "湿度:${realtime.humidity}".logE(logFlag)

        val windDirectionByte= ByteArray(4)
        windDirectionByte.writeFloatLE(realtime.wind.direction)
        "风向: ${realtime.wind.direction}".logE(logFlag)

        val windSpeedByte= ByteArray(4)
        windSpeedByte.writeFloatLE(realtime.wind.speed)
        "风力:${realtime.wind.speed}".logE(logFlag)

        val cloudrateByte= ByteArray(4)
        cloudrateByte.writeFloatLE(realtime.cloudrate)
        "云量:${realtime.cloudrate} ".logE(logFlag)

        val rainfallByte= ByteArray(4)
        rainfallByte.writeFloatLE(realtime.precipitation.local.intensity)
        "降雨量:${realtime.precipitation.local.intensity}".logE(logFlag)

        val rainProbabilityByte= ByteArray(4)
        rainProbabilityByte.writeFloatLE(daily.precipitation[0].probability)
        "降雨概率:${daily.precipitation[0].probability}".logE(logFlag)

        val pm25Byte= ByteArray(4)
        pm25Byte.writeFloatLE(realtime.airQuality.pm25)
        "pm25:${realtime.airQuality.pm25}".logE(logFlag)

        val pm10Byte= ByteArray(4)
        pm10Byte.writeFloatLE(realtime.airQuality.pm10)
        "pm10:${realtime.airQuality.pm10}".logE(logFlag)

        val o3Byte= ByteArray(4)
        o3Byte.writeFloatLE(realtime.airQuality.o3)
        "o3:${realtime.airQuality.o3}".logE(logFlag)

        val so2Byte= ByteArray(4)
        so2Byte.writeFloatLE(realtime.airQuality.so2)
        "so2:${realtime.airQuality.so2}".logE(logFlag)

        val no2Byte= ByteArray(4)
        no2Byte.writeFloatLE(realtime.airQuality.no2)
        "no2:${realtime.airQuality.no2}".logE(logFlag)

        val coByte= ByteArray(4)
        coByte.writeFloatLE(realtime.airQuality.co)
        "co:${realtime.airQuality.co}".logE(logFlag)

        val coldRiskByte= ByteArray(1)
        coldRiskByte.writeInt8(daily.lifeIndex.coldRisk[0].index)
        "感冒指数:${daily.lifeIndex.coldRisk[0].index}".logE(logFlag)

        val dressingByte= ByteArray(1)
        dressingByte.writeInt8(daily.lifeIndex.dressing[0].index)
        "穿衣指数:${daily.lifeIndex.dressing[0].index}".logE(logFlag)

        val carWashingByte= ByteArray(1)
        carWashingByte.writeInt8(daily.lifeIndex.carWashing[0].index)
        "洗车指数:${daily.lifeIndex.carWashing[0].index}".logE(logFlag)

        val sunTimeNullByte= ByteArray(1)
        sunTimeNullByte.writeInt8(0)
        val alertCodeNullByte= ByteArray(1)
        alertCodeNullByte.writeInt8(255)

        val sunriseByte: ByteArray = (daily.astro[0].sunrise.time).toByteArray(charset("UTF-8"))+sunTimeNullByte

//        val sunriseByte= ByteArray(6)
//        sunriseByte.writeStringLE(daily.astro[0].sunrise.time+"0")
        "日出时间:${daily.astro[0].sunrise.time}".logE(logFlag)

        val sunsetByte: ByteArray = (daily.astro[0].sunset.time).toByteArray(charset("UTF-8"))+sunTimeNullByte

//        val sunsetByte= ByteArray(6)
//        sunsetByte.writeStringLE(daily.astro[0].sunset.time+"0")
        "日出时间:${daily.astro[0].sunset.time}".logE(logFlag)

        var alertCodeByte= ByteArray(2)
        if(alert.content.isEmpty()){
            alertCodeByte=alertCodeNullByte+alertCodeNullByte
            "预警代码:${alertCodeByte.toHexString()}".logE(logFlag)
        }else{
            alertCodeByte.writeInt16LE(alert.content[0].code.toInt())
            "预警代码:${alert.content[0].code.toInt()}".logE(logFlag)
        }

        val weatherByteArray = weatherIdByte+airApiByte+ultravioletByte+maxTempByte+minTempByte+
                nowTempByte+humidityByte+windDirectionByte+windSpeedByte+cloudrateByte+rainfallByte+
                rainProbabilityByte+pm25Byte+pm10Byte+o3Byte+so2Byte+no2Byte+coByte+coldRiskByte+dressingByte+
                carWashingByte+sunriseByte+sunsetByte+alertCodeByte
        //发送天气数据
        SerialPortHelper.sendWeatherData(weatherByteArray)

    }
    override fun onBind(intent: Intent): IBinder {
        mLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        Log.e("TAG", "onBind~")
        //传入Handler实例化Messenger
        mMessenger = Messenger(mHandler)
        //将Messenger中的binder返回给客户端,让它可以远程调用
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
                        Log.e("来自client的",comHubData.toString())

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
        val comHubData = ComHubData("小明", age++)
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

    override fun initLocation() {
        if (mmkv.getBoolean(ValueKey.isNetworking,false)){
            initLocationOption()
        }
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }
}

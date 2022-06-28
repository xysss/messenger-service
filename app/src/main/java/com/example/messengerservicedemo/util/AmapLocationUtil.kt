package com.example.messengerservicedemo.util

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.blankj.utilcode.util.NetworkUtils

/**
 * 作者 : xys
 * 时间 : 2022-06-28 10:59
 * 描述 : 封装高德地图Util
 */


class AmapLocationUtil(private val mContext: Context) {
    private var locationClient: AMapLocationClient? = null
    private var locationOption: AMapLocationClientOption? = null
    private var mOnCallBackListener: onCallBackListener? = null

    /**
     * 初始化定位
     */
    fun initLocation() {
        //初始化client
        if (null == locationClient) locationClient = AMapLocationClient(mContext)
        locationOption = defaultOption

        locationClient?.let {
            //设置定位参数
            it.setLocationOption(locationOption)
            // 设置定位监听
            it.setLocationListener(locationListener)
        }

    }
    //可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
    //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
    //可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
    //可选，设置定位间隔。默认为2秒
    //可选，设置是否返回逆地理地址信息。默认是true
    //可选，设置是否单次定位。默认是false
    //可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
    //可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
    //可选，设置是否使用传感器。默认是false
    //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
    //可选，设置是否使用缓存定位，默认为true
    //可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
    //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
    //如果网络可用就选择高精度
    private val defaultOption: AMapLocationClientOption
        get() {
            val mOption = AMapLocationClientOption()
            //如果网络可用就选择高精度
            if (NetworkUtils.isConnected()) {
                //可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
                mOption.locationMode = AMapLocationClientOption.AMapLocationMode.Battery_Saving
                mOption.isGpsFirst = true //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
            } else {
                mOption.locationMode =
                    AMapLocationClientOption.AMapLocationMode.Device_Sensors //可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
                mOption.isGpsFirst = true //可选，设置是否gps优先，只在高精度模式下有效。默认关闭
            }
            mOption.httpTimeOut = 30000 //可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
            mOption.interval = 6*1000*60*60 //可选，设置定位间隔。默认为2秒
            mOption.isNeedAddress = true //可选，设置是否返回逆地理地址信息。默认是true
            mOption.isOnceLocation = false //可选，设置是否单次定位。默认是false
            mOption.isOnceLocationLatest =
                false //可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
            AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP) //可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
            mOption.isSensorEnable = true //可选，设置是否使用传感器。默认是false
            mOption.isWifiScan =
                true //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
            mOption.isLocationCacheEnable = true //可选，设置是否使用缓存定位，默认为true
            return mOption
        }

    private var locationListener = AMapLocationListener { location ->
        val sb = StringBuilder()
        location?.let {
            //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
            if (location.errorCode == 0) {
                longitude = location.longitude
                latitude = location.latitude
                val district = location.district
                locationSuccess(longitude, latitude, true, location, district)
                //定位成功，停止定位：如果实时定位，就把stopLocation()关闭
                stopLocation()
            } else {
                //定位失败
//                    sb.append("定位失败" + "\n");
//                    sb.append("错误码:" + location.getErrorCode() + "\n");
//                    sb.append("错误信息:" + location.getErrorInfo() + "\n");
//                    sb.append("错误描述:" + location.getLocationDetail() + "\n");
//                    Log.e("---> 定位失败", sb.toString());
                LocationFarile(false, location)
            }
        }
    }

    private fun LocationFarile(isSucdess: Boolean, location: AMapLocation) {
        mOnCallBackListener?.let {
            it.onCallBack(0.0, 0.0, location, false, "")
        }

    }

    fun locationSuccess(
        longitude: Double,
        latitude: Double,
        isSucdess: Boolean,
        location: AMapLocation?,
        address: String?
    ) {
        mOnCallBackListener?.let {
            it.onCallBack(longitude, latitude, location, true, address)
        }
    }

    fun setOnCallBackListener(listener: onCallBackListener?) {
        mOnCallBackListener = listener
    }

    interface onCallBackListener {
        fun onCallBack(
            longitude: Double,
            latitude: Double,
            location: AMapLocation?,
            isSucdess: Boolean,
            address: String?
        )
    }

    /**
     * 开始定位
     */
    fun startLocation() {
        locationClient?.let { it.startLocation() }
    }

    /**
     * 停止定位
     */
    fun stopLocation() {
        locationClient?.let { it.stopLocation() }
    }

    /**
     * 销毁定位
     */
    fun destroyLocation() {
        locationClient?.let {
            /**
             * 如果AMapLocationClient是在当前Activity实例化的，
             * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
             */
            it.onDestroy()
            locationClient = null
            locationOption = null
        }

    }

    companion object {
        var longitude = 0.0
        var latitude = 0.0
    }
}

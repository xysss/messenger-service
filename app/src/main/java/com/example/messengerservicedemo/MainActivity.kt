package com.example.messengerservicedemo

import ZtlApi.ZtlManager
import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ToastUtils
import com.tbruyelle.rxpermissions2.RxPermissions
import me.hgj.mvvmhelper.base.appContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

//        AMapLocationClient.setApiKey("b96f7ca9017f60d6444abce336e66f01")
//
//        //高德定位必须来保障信息安全
//        AMapLocationClient.updatePrivacyAgree(appContext, true)
//        AMapLocationClient.updatePrivacyShow(appContext, true, true)
//
//        val intent = Intent(this,MessengerService::class.java)
//        startService(intent)

        //这个是共享ViewModel
        //请求权限

        //设置开机自启动
        ZtlManager.GetInstance().setBootPackageActivity(
            appContext.packageName, "com.example.messengerservicedemo.MainActivity"
        )

        //需要固件内置appservice
        //第一个参数填入包名，第二个参数填入需要保活的服务
        ZtlManager.GetInstance().keepService(appContext.packageName, "com.example.messengerservicedemo.MessengerService")

        requestCameraPermissions()
    }

    override fun onResume() {
        finish()
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 请求相机权限
     */
    @SuppressLint("CheckResult")
    private fun requestCameraPermissions() {
        ToastUtils.showShort("请求权限")
        //请求打开相机权限
        val rxPermissions = RxPermissions(this)
        rxPermissions.request(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.SYSTEM_ALERT_WINDOW,
//            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.READ_EXTERNAL_STORAGE).subscribe { aBoolean ->
            if (aBoolean) {
                ToastUtils.showShort("权限已经打开")
            } else {
                ToastUtils.showShort("权限被拒绝")
            }
        }
    }

}
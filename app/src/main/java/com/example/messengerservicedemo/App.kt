package com.example.messengerservicedemo

import android.app.Application
import android.content.Intent
import com.amap.api.location.AMapLocationClient
import com.effective.android.anchors.AnchorsManager
import com.effective.android.anchors.Project
import me.hgj.mvvmhelper.base.MvvmHelper
import me.hgj.mvvmhelper.base.appContext
import me.hgj.mvvmhelper.ext.currentProcessName

/**
 * 作者 : xys
 * 时间 : 2022-06-27 16:41
 * 描述 : 描述
 */

class App: Application() {

    //companion 静态  object 单例
    companion object {
        const val WeatherToken = "o2ZDuROz4Ns2ZZV5" //彩云天气xys申请令牌
    }

    override fun onCreate() {
        super.onCreate()
        MvvmHelper.init(this,BuildConfig.DEBUG)
        // 获取当前进程名
        val processName = currentProcessName
        if (currentProcessName == packageName) {
            // 主进程初始化
            onMainProcessInit()
        } else {
            // 其他进程初始化
            processName?.let { onOtherProcessInit(it) }
        }

        AMapLocationClient.setApiKey("daf2ce3d1aec0ba4cab0996985dbcc50")

        //高德定位必须来保障信息安全
        AMapLocationClient.updatePrivacyAgree(appContext, true)
        AMapLocationClient.updatePrivacyShow(appContext, true, true)

        val intent = Intent(this,MessengerService::class.java)
        startService(intent)

    }

    /**
     * @description  代码的初始化请不要放在onCreate直接操作，按照下面新建异步方法
     */
    private fun onMainProcessInit() {
        //支持同异步依赖任务初始化 Android 启动框架
        //如果一个任务要确保在 application#onCreate 前执行完毕，则该任务成为锚点任务
        AnchorsManager.getInstance()
            .debuggable(BuildConfig.DEBUG)
            //传递任务 id 设置锚点任务
            .addAnchor(InitNetWork.TASK_ID, InitUtils.TASK_ID, InitComm.TASK_ID, InitToast.TASK_ID).start(
                Project.Builder("app", AppTaskFactory())  //可选，构建依赖图可以使用工厂，
                    .add(InitNetWork.TASK_ID)
                    .add(InitComm.TASK_ID)
                    .add(InitUtils.TASK_ID)
                    .add(InitToast.TASK_ID)
                    .build()
            )
    }

    /**
     * 其他进程初始化，[processName] 进程名
     */
    private fun onOtherProcessInit(processName: String) {}

}
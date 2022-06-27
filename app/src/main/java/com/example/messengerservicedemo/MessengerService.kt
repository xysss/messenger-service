package com.example.messengerservicedemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import com.blankj.utilcode.util.ToastUtils
import com.example.messengerservicedemo.ext.job
import com.example.messengerservicedemo.ext.logFlag
import com.example.messengerservicedemo.ext.scope
import com.example.messengerservicedemo.service.ForegroundNF
import com.example.model.Person
import com.example.model.UserS
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.hgj.mvvmhelper.ext.logE
import java.lang.ref.WeakReference

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
            "服务已经启动".logE(logFlag)


        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mForegroundNF.stopForegroundNotification()
        job.cancel()
        super.onDestroy()
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

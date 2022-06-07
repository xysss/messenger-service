package com.example.messengerservicedemo

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import model.Person
import java.lang.ref.WeakReference

/**
 * 作者 : xys
 * 时间 : 2022-06-06 10:50
 * 描述 : 描述
 */
//这里服务端Service是运行在单独的进程中的 android:process=":other"
class MessengerService : Service() {

    companion object {
        const val WHAT1 = 1
        const val WHAT2 = 2
        const val WHAT3 = 3
    }

    private val mHandler = MyHandler(WeakReference(this))

    private lateinit var mMessenger: Messenger

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
                when (msg.what) {
                    WHAT1 -> {
                        val person = msg.data.getParcelable("person") as Person?
                        Log.e("来自客户端的",person.toString())

//                        val user = msg.data.get("user")
//                        Log.e("来自客户端的",user.toString())

//                        val date = msg.data.getString("data")
//                        Log.e("来自客户端的",date.toString())

                        //客户端的Messenger就是放在Message的replyTo中的
                        replyToClient(msg, "I have received your message and will reply to you later")
                    }
                    WHAT2 ->{

                    }
                    else -> super.handleMessage(msg)
                }
            }
        }
    }

    private fun replyToClient(msg: Message, replyText: String) {
        val clientMessenger = msg.replyTo
        val replyMessage = Message.obtain(null, WHAT1)
        replyMessage.data = Bundle().apply {
            putString("reply", replyText)
        }
        try {
            clientMessenger?.send(replyMessage)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }




}

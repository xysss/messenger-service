package com.example.messengerservicedemo.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract.Intents.Insert.ACTION
import com.example.messengerservicedemo.MainActivity
import com.example.messengerservicedemo.ext.logFlag
import me.hgj.mvvmhelper.base.appContext
import me.hgj.mvvmhelper.ext.logE

class BootCompleteMyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        "接收广播 onReceive: ${intent.action}".logE(logFlag)
        //开机启动
        if (ACTION == intent.action) {
            //第一种方式：根据包名
            val mainIntent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
            mainIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(mainIntent)
            //context.startService(mainIntent)
        }

    }
}
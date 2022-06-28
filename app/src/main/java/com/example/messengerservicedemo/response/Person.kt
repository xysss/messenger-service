package com.example.messengerservicedemo.response

import com.example.messengerservicedemo.ext.logFlag
import me.hgj.mvvmhelper.ext.logE

/**
 * 作者 : xys
 * 时间 : 2022-06-28 11:02
 * 描述 : 描述
 */
class Person(val age:Int,val name:String) {
    fun print(){
        "Person"+name + age + "岁了".logE(logFlag)
    }
}
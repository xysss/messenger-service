package com.example.messengerservicedemo.network.manager

import com.kunminx.architecture.ui.callback.UnPeekLiveData

/**
 * 作者 : xys
 * 时间 : 2022-07-06 14:52
 * 描述 : 网络变化管理者
 */

class NetworkStateManager private constructor(){
    val mNetworkStateCallback = UnPeekLiveData<NetState>()
    companion object{
        val instance: NetworkStateManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            NetworkStateManager()
        }
    }
}
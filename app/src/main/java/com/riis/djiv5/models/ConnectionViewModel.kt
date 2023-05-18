package com.riis.djiv5.models

import android.app.Application
import android.content.Context
import androidx.annotation.NonNull
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.dji.mobileinfra.StringUtils
import dji.sdk.keyvalue.key.RemoteControllerKey
import dji.sdk.keyvalue.value.remotecontroller.PairingState
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.et.action
import dji.v5.et.create
import dji.v5.et.get
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "myApp"

    val registerState = MutableLiveData<String>()
    val sdkNews = MutableLiveData<SDKNews>()


    fun registerApp(context: Context, @NonNull callback: SDKManagerCallback) {
        SDKManager.getInstance().init(context, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                callback.onRegisterSuccess()
                registerState.postValue("registered")
            }

            override fun onRegisterFailure(error: IDJIError?) {
                callback.onRegisterFailure(error)
                registerState.postValue("unregistered")
            }

            override fun onProductDisconnect(product: Int) {
                callback.onProductDisconnect(product)
            }

            override fun onProductConnect(product: Int) {
                callback.onProductConnect(product)
            }

            override fun onProductChanged(product: Int) {
                callback.onProductChanged(product)
            }

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                callback.onInitProcess(event, totalProcess)
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                callback.onDatabaseDownloadProgress(current, total)
            }
        })
    }

    fun releaseSDKCallback() {
        SDKManager.getInstance().destroy()
    }

    fun updateNews() {
        //whatever this iz
        sdkNews.postValue(SDKNews(1, 1, "this"))
    }

    fun doPairing(callback: ((String) -> Unit)? = null) {
        if (!SDKManager.getInstance().isRegistered) {
            return
        }
        RemoteControllerKey.KeyPairingStatus.create().get({
            if (it == PairingState.PAIRING) {
                RemoteControllerKey.KeyStopPairing.create().action()
                callback?.invoke(StringUtils().toString())
            } else {
                RemoteControllerKey.KeyRequestPairing.create().action()
                callback?.invoke(StringUtils().toString())
            }
        }) {
            callback?.invoke(it.toString())
        }
    }

    data class SDKNews(
        var title: Int,
        var description: Int,
        var date: String,
    )
}

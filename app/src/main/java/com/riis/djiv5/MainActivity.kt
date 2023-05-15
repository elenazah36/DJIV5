package com.riis.djiv5

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

class MainActivity : AppCompatActivity() {
    private val TAG = "myApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerApp()
    }

    private fun registerApp() {
        SDKManager.getInstance().init(this, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                Log.i(TAG, "myApp onRegisterSuccess")
            }

            override fun onRegisterFailure(error: IDJIError) {
                Log.i(TAG, "myApp onRegisterFailure")
            }

            override fun onProductDisconnect(productId: Int) {
                Log.i(TAG, "myApp onProductDisconnect")
            }

            override fun onProductConnect(productId: Int) {
                Log.i(TAG, "myApp onProductConnect")
            }

            override fun onProductChanged(productId: Int) {
                Log.i(TAG, "myApp onProductChanged")
            }

            override fun onInitProcess(event: DJISDKInitEvent, totalProcess: Int) {
                Log.i(TAG, "myApp onInitProcess")
                if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
                    Log.i(TAG, "myApp start registerApp")
                    SDKManager.getInstance().registerApp()
                }
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                Log.i(TAG, "myApp onDatabaseDownloadProgress")
            }
        })
    }
}
package com.riis.djiv5

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat


class ConnectionActivity : AppCompatActivity() {
    private lateinit var mTextConnectionStatus: TextView
    private lateinit var mTextProduct: TextView
    private lateinit var mTextModelAvailable: TextView
    private lateinit var mBtnOpen: Button
    private lateinit var mVersionTv: TextView

    private val model: ConnectionViewModel// this is all our data stored in the view model
        get() {
            TODO()
        //needs to be linked to the ViewModel in order to connect to the aircraft
        }

    companion object {
        const val TAG = "ConnectionActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        ActivityCompat.requestPermissions(this, // request all the permissions that we'll need
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.VIBRATE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.READ_PHONE_STATE
            ), 1)
    }
}
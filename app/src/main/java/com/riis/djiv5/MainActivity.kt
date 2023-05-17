package com.riis.djiv5

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.riis.djiv5.pages.WayPointV3
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback

class MainActivity : AppCompatActivity() {

    private val clickListener =
        View.OnClickListener { v: View ->
            // based on what the user picks, it'll use either Waypoint 1 or 2
            when (v.id) {
                R.id.btn_waypoint1 -> startActivity(
                    this@MainActivity,
                    WayPointV3::class.java
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.btn_waypoint1).setOnClickListener(clickListener)
    }

    private fun startActivity(context: Context, activity: Class<*>?) {
        // this will start the activity
        val intent = Intent(context, activity)
        context.startActivity(intent)
    }
}
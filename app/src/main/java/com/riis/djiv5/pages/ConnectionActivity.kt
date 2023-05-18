package com.riis.djiv5.pages

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.activity.viewModels
import com.riis.djiv5.MainActivity
import com.riis.djiv5.R
import com.riis.djiv5.models.ConnectionViewModel
import com.riis.djiv5.models.MSDKInfoViewModel
import com.secneo.sdk.Helper
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.interfaces.SDKManagerCallback
import dji.v5.utils.common.StringUtils
import dji.v5.utils.common.ToastUtils


class ConnectionActivity : AppCompatActivity() {
    private lateinit var mTextConnectionStatus: TextView
    private lateinit var mTextProduct: TextView
    private lateinit var mTextModelAvailable: TextView
    private lateinit var mBtnOpen: Button
    private lateinit var mBtnPair: Button
    private lateinit var mVersionTv: TextView

    private val model: ConnectionViewModel by viewModels()
    protected val msdkInfoVm: MSDKInfoViewModel by viewModels()
    private val handler: Handler = Handler(Looper.getMainLooper())
    // this is all our data stored in the view model

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

        initUI()
        initMSDKInfoView()
        registerApp()

    }


    private fun initUI() { // Initializes the UI with all the string values
        mTextConnectionStatus = findViewById(R.id.text_connection_status)
        mTextModelAvailable = findViewById(R.id.text_model_available)
        mTextProduct = findViewById(R.id.text_product_info)
        mBtnOpen = findViewById(R.id.btn_open)
        mVersionTv = findViewById(R.id.textView2)
        //mVersionTv.text = resources.getString(R.string.sdk_version, DJISDKManager.getInstance().sdkVersion)
        mBtnOpen.isEnabled = false
        mBtnOpen.setOnClickListener { // navigate to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        mBtnPair = findViewById(R.id.btn_pair)
    }


    @SuppressLint("SetTextI18n")
    private fun initMSDKInfoView() {
        ToastUtils.init(this)
        msdkInfoVm.msdkInfo.observe(this) {
            mVersionTv.text = StringUtils.getResStr(R.string.sdk_version, it.SDKVersion + " " + it.buildVer)
            mTextProduct.text = StringUtils.getResStr(R.string.product_information, it.productType.name)
            //text_view_package_product_category.text = StringUtils.getResStr(R.string.package_product_category, it.packageProductCategory)
            //text_view_is_debug.text = StringUtils.getResStr(R.string.is_sdk_debug, it.isDebug)
            //text_core_info.text = it.coreInfo.toString()
        }
        model.registerState.observe(this) {
                mTextConnectionStatus.text = model.registerState.value
        }
        mBtnPair.setOnClickListener {
            model.doPairing {
                ToastUtils.showToast(it)
            }
        }
    }


    private fun registerApp() {
        model.registerApp(this, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                ToastUtils.showToast("Register Success")
                msdkInfoVm.initListener()
                /*handler.postDelayed({
                    prepareUxActivity()
                }, 5000)*/
            }

            override fun onRegisterFailure(error: IDJIError?) {
                ToastUtils.showToast("Register Failure: (errorCode: ${error?.errorCode()}, description: ${error?.description()})")
            }

            override fun onProductDisconnect(product: Int) {
                ToastUtils.showToast("Product: $product Disconnect")
            }

            override fun onProductConnect(product: Int) {
                ToastUtils.showToast("Product: $product Connect")
            }

            override fun onProductChanged(product: Int) {
                ToastUtils.showToast("Product: $product Changed")
            }

            override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
                ToastUtils.showToast("Init Process event: ${event?.name}")
            }

            override fun onDatabaseDownloadProgress(current: Long, total: Long) {
                ToastUtils.showToast("Database Download Progress current: $current, total: $total")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        model.releaseSDKCallback()
        ToastUtils.destroy()
    }

    override fun onResume() {
        super.onResume()
        model.releaseSDKCallback()
    }


}
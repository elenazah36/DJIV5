package com.riis.djiv5

import android.app.Application
import android.content.Context

class MApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        //com.secneo.sdk.Helper.install(this)
    }
}
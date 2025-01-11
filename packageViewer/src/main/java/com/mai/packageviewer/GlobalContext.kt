package com.mai.packageviewer

import android.annotation.SuppressLint
import android.content.Context

class GlobalContext {

    companion object {
        /**
         * 全局context
         */
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

}
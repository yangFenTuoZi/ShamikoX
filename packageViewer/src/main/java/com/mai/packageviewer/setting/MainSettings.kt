package com.mai.packageviewer.setting

import android.content.Context
import android.content.SharedPreferences

class MainSettings(private val context: Context) {

    companion object {
        var INSTANCE : MainSettings? = null
        private const val FILE_NAME = "settings"
        const val ORDER_BY_NAME = "order_by_name"
        const val SHOW_SYSTEM_APP = "show_system_app"
    }

    fun getBool(key: String, defaultValue: Boolean): Boolean {
        return getSp().getBoolean(key, defaultValue)
    }

    fun setBool(key: String, value: Boolean) {
        getSp().edit().putBoolean(key, value).apply()
    }

    private fun getSp(): SharedPreferences {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

}
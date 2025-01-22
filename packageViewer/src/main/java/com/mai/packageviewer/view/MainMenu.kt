package com.mai.packageviewer.view

import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.forEach
import com.mai.packageviewer.R
import com.mai.packageviewer.setting.MainSettings

/**
 * 筛选与搜索菜单
 */
class MainMenu(val menu: Menu, val activity: Activity) {

    /**
     * 回调
     */
    var mainMenuListener: MainMenuListener? = null

    // 搜索框，用于判断处理back事件
    private lateinit var searchViewSearchButton: View
    private lateinit var searchViewCloseButton: View

    // 筛选条件
    var orderByName = false
    var showSystemApp = false

    init {
        menu.forEach {
            when (it.itemId) {
                R.id.menuTitle -> {
                    val spannableString = SpannableString(it.title)
                    spannableString.setSpan(
                        ForegroundColorSpan(
                            getColorFromAttr(
                                activity,
                                com.google.android.material.R.attr.colorPrimary
                            )
                        ), 0, spannableString.length, 0
                    )
                    it.title = spannableString
                }

                R.id.order_by_name -> {
                    it.isChecked = MainSettings.INSTANCE!!.getBool(MainSettings.ORDER_BY_NAME, false)
                    orderByName = it.isChecked
                }

                R.id.order_by_date -> {
                    if (!MainSettings.INSTANCE!!.getBool(MainSettings.ORDER_BY_NAME, false)) {
                        it.isChecked = true
                        orderByName = !it.isChecked
                    }
                }

                R.id.show_system_app -> {
                    it.isChecked =
                        MainSettings.INSTANCE!!.getBool(MainSettings.SHOW_SYSTEM_APP, false)
                    showSystemApp = it.isChecked
                }

                R.id.app_bar_search -> {
                    val searchView = it.actionView as SearchView
                    searchView.queryHint =
                        activity.getString(R.string.input_app_name_or_package_name)
                    searchViewSearchButton =
                        searchView.findViewById(androidx.appcompat.R.id.search_button)
                    searchViewCloseButton =
                        searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
                            // 实时搜索
                            return true
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            if (newText != null)
                                mainMenuListener?.onQueryTextChange(newText)
                            return true
                        }
                    })
                }
            }
        }
    }

    private fun getColorFromAttr(context: Context, attr: Int): Int {
        // 创建TypedArray来存取自定义属性
        val typedArray: TypedArray = context.obtainStyledAttributes(intArrayOf(attr))
        // 获取颜色值
        val color = typedArray.getColor(0, Color.BLACK) // 默认颜色为黑色
        // 回收TypedArray
        typedArray.recycle()
        return color
    }

    /**
     * 处理菜单的点击事件
     */
    fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.order_by_name -> {
                item.isChecked = true
                MainSettings.INSTANCE!!.setBool(MainSettings.ORDER_BY_NAME, true)

                orderByName = true
                mainMenuListener?.onOrderChanged(true)
            }

            R.id.order_by_date -> {
                item.isChecked = true
                MainSettings.INSTANCE!!.setBool(MainSettings.ORDER_BY_NAME, false)

                orderByName = false
                mainMenuListener?.onOrderChanged(false)
            }

            R.id.show_system_app -> {
                val b = !item.isChecked
                item.isChecked = b
                MainSettings.INSTANCE!!.setBool(MainSettings.SHOW_SYSTEM_APP, b)

                showSystemApp = b
                mainMenuListener?.onIncludeSystemApp(b)
            }

            else -> {
                return false
            }
        }
        return true
    }

    /**
     * 处理back事件时的搜索框相应
     */
    fun handleBackPresses(): Boolean {
        return if (searchViewSearchButton.visibility != View.VISIBLE) {
            searchViewCloseButton.performClick()
            true
        } else {
            false
        }
    }

    interface MainMenuListener {
        fun onOrderChanged(orderByName: Boolean)

        // 属性过滤
        fun onIncludeSystemApp(includeSystemApp: Boolean)

        // 文字变化
        fun onQueryTextChange(newText: String)
    }

}
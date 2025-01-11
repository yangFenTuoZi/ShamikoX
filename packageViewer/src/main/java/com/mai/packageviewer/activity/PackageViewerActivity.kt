package com.mai.packageviewer.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.snackbar.Snackbar
import com.mai.packageviewer.ApplicationBuildConfig
import com.mai.packageviewer.R
import com.mai.packageviewer.adapter.AppAdapter
import com.mai.packageviewer.data.AppInfo
import com.mai.packageviewer.databinding.ActivityPackageViewerBinding
import com.mai.packageviewer.util.AppInfoHelper
import com.mai.packageviewer.util.PackageReceiver
import com.mai.packageviewer.view.AppInfoDetailDialog
import com.mai.packageviewer.view.MainMenu
import net.sourceforge.pinyin4j.PinyinHelper
import java.util.Locale
import java.util.Vector
import kotlin.math.min


class PackageViewerActivity : AppCompatActivity() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var dialogList: AppInfoDetailDialog? = null
    }

    private lateinit var binding: ActivityPackageViewerBinding
    private lateinit var mainMenu: MainMenu

    private var appInfoList = ArrayList<AppInfo>()
    private var appInfoFilterList = ArrayList<AppInfo>()
    private val appAdapter = AppAdapter(appInfoFilterList)

    private var onBackPressedTimeStamp = System.currentTimeMillis()

    private var packageReceiver = PackageReceiver { _, intent ->
        if (intent.action != null) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED,
                    -> {
                    if (!AppInfoHelper.isRunning)
                        onDataSetChanged()
                }
            }
        }
    }

    var choose: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        binding = ActivityPackageViewerBinding.inflate(layoutInflater)
        binding.loadingView.setOnClickListener {}
        setContentView(binding.root)
        choose = intent.getBooleanExtra("choose", false)
        createOptionsMenu()
        initRecyclerView()

        registerReceiver(packageReceiver, IntentFilter().apply {
            addDataScheme("package")
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
        })

    }

    override fun onPause() {
        if (dialogList?.isShowing() == true) {
            dialogList?.dismiss()
        }
        super.onPause()
    }

    override fun onStop() {
        unregisterReceiver(packageReceiver)
        super.onStop()
    }

    private fun initRecyclerView() {
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = appAdapter
    }

    private fun getInstallPackages(callback: (MutableList<PackageInfo>) -> Unit) {
        Thread {
            val flags =
                PackageManager.GET_META_DATA or ((if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)) or PackageManager.GET_PROVIDERS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS
            val installedPackages = packageManager.getInstalledPackages(flags)
            Handler(Looper.getMainLooper()).post {
                callback(installedPackages)
            }
        }.start()
    }

    /**
     * 获取并筛选app
     */
    private fun onDataSetChanged() {
        showLoading()
        getInstallPackages { packages ->
            if (packages.size == 1 && packages[0].packageName == ApplicationBuildConfig.APPLICATION_ID) {
                Snackbar.make(
                    binding.root,
                    R.string.can_not_get_package_list,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.authorize) {
                        try {
                            val intent = Intent().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.data = Uri.fromParts("package", packageName, null)
                            } else {
                                intent.action = Intent.ACTION_VIEW
                                intent.setClassName(
                                    "com.android.settings",
                                    "com.android.setting.InstalledAppDetails"
                                )
                                intent.putExtra(
                                    "com.android.settings.ApplicationPkgName",
                                    packageName
                                )
                            }
                            startActivity(intent)
                        } catch (_: Exception) {
                            Snackbar.make(
                                binding.root,
                                getString(R.string.authorize_by_hand),
                                Snackbar.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    .show()
            }

            AppInfoHelper.handle(packages, object : AppInfoHelper.AppInfoCallback {
                override fun onResult(ret: Vector<MutableList<AppInfo>>) {
                    appInfoList.clear()
                    ret.forEach {
                        appInfoList.addAll(it)
                    }
                    onOptionsChanged()
                }
            }, packages.size / 60 + 1)
        }
    }

    /**
     * 筛选条件与排序依据变更，重新获取条件并过滤
     * @param sortOnly 只排序，不更改过滤条件
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun onOptionsChanged(sortOnly: Boolean = false) {
        runOnUiThread {
            if (!sortOnly) {
                appInfoFilterList.clear()
                // 重新过滤
                appInfoFilterList.addAll(
                    appInfoList.filterNot {
                        it.packageName == ApplicationBuildConfig.APPLICATION_ID
                                || (!mainMenu.showSystemApp && it.isSystemApp)
                    }.filter { true })
            }

            // 排序
            if (mainMenu.orderByName) {
                appInfoFilterList.sortWith { lh, rh ->
                    val charL = lh.label[0].lowercaseChar()
                    val charR = rh.label[0].lowercaseChar()
                    val strL = if (charL.isLowerCase() || charL.isDigit()) {
                        // label为字母
                        lh.label.lowercase(Locale.getDefault()).toCharArray()
                    } else {
                        // 首字拼音+读音，简单比较直接忽略错误
                        try {
                            PinyinHelper.toHanyuPinyinStringArray(charL)[0].toCharArray()
                        } catch (_: Exception) {
                            lh.label.lowercase(Locale.getDefault()).toCharArray()
                        }
                    }
                    val strR = if (charR.isLowerCase() || charR.isDigit()) {
                        rh.label.lowercase(Locale.getDefault()).toCharArray()
                    } else {
                        try {
                            PinyinHelper.toHanyuPinyinStringArray(charR)[0].toCharArray()
                        } catch (_: Exception) {
                            rh.label.lowercase(Locale.getDefault()).toCharArray()
                        }
                    }

                    var result = 1  // 简单比较，忽略首字读音相同情况
                    for (i in 0 until min(strL.size, strR.size)) {
                        if (strL[i] != strR[i]) {
                            // 继续比较下一位
                            val num = strL[i].code - strR[i].code
                            result = when {
                                num > 0 -> 1
                                num == 0 -> 0
                                else -> -1
                            }
                            break
                        } else {
                            continue
                        }
                    }
                    result
                }
            } else {
                // 更新时间降序
                appInfoFilterList.sortByDescending {
                    it.lastUpdateTime
                }
            }
            hideLoading()
            Snackbar.make(
                binding.root,
                getString(R.string.apps_found, appInfoFilterList.size),
                Snackbar.LENGTH_SHORT
            )
                .show()
            appAdapter.update(appInfoFilterList)
            appAdapter.notifyDataSetChanged()
        }
    }

    private fun createOptionsMenu() {
        binding.toolbar.inflateMenu(R.menu.menu_app_list)
        binding.toolbar.title = "Package Viewer"
        if (choose) binding.toolbar.subtitle = getString(R.string.choose_mode)
        mainMenu = MainMenu(binding.toolbar.menu!!, this)
        mainMenu.mainMenuListener = object : MainMenu.MainMenuListener {

            override fun onOrderChanged(orderByName: Boolean) {
                onOptionsChanged(true)
            }

            override fun onIncludeSystemApp(includeSystemApp: Boolean) {
                onOptionsChanged()
            }

            override fun onQueryTextChange(newText: String) {
                // 搜索
                appAdapter.filter.filter(newText)
            }

        }

        onDataSetChanged()
    }

    private fun showLoading() {
        binding.loadingView.visibility = View.VISIBLE
        mainMenu.menu.setGroupEnabled(R.id.menu_group_sort, false)
        mainMenu.menu.setGroupEnabled(R.id.menu_group_filter_pref, false)
    }

    private fun hideLoading() {
        binding.loadingView.visibility = View.GONE
        mainMenu.menu.setGroupEnabled(R.id.menu_group_sort, true)
        mainMenu.menu.setGroupEnabled(R.id.menu_group_filter_pref, true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (::mainMenu.isInitialized) {
            // 处理点击事件
            if (mainMenu.onOptionsItemSelected(item))
                return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 判断搜索框状态
            if (mainMenu.handleBackPresses()) {
                true
            } else {
                if (System.currentTimeMillis() - onBackPressedTimeStamp > 2000L && !choose) {
                    onBackPressedTimeStamp = System.currentTimeMillis()
                    Snackbar.make(binding.root, getString(R.string.press_again_to_exit), 2000)
                        .show()
                    true
                } else {
                    try {
                        if (AppInfoHelper.isRunning) {
                            AppInfoHelper.forceStop()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    super.onKeyDown(keyCode, event)
                }
            }
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}
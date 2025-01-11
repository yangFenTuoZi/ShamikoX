package com.mai.packageviewer.data

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Build
import com.mai.packageviewer.GlobalContext


class AppInfo(packageInfo: PackageInfo) {

    private val applicationInfo: ApplicationInfo = packageInfo.applicationInfo!!

    /**
     * 系统App
     */
    val isSystemApp =
        ((applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                ) or (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)

    /**
     * minSdkVersion
     * AndroidN之前不支持
     */
    val minSdkVersion =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) applicationInfo.minSdkVersion else -1

    /**
     * targetSdkVersion
     */
    val targetSdkVersion = applicationInfo.targetSdkVersion

    /**
     * ApplicationID
     */
    val packageName: String = applicationInfo.packageName ?: ""

    /**
     * versionCode
     */
    val versionCode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong()

    /**
     * versionName
     */
    val versionName: String? = packageInfo.versionName

    /**
     * 首次安装时间
     */
    val firstInstallTime = packageInfo.firstInstallTime

    /**
     * 最后更新时间
     */
    val lastUpdateTime = packageInfo.lastUpdateTime

    /**
     * AppName
     */
    var label = applicationInfo.loadLabel(GlobalContext.context.packageManager).toString()

    /**
     * uid
     */
    var uid = applicationInfo.uid

    /**
     * icon
     */
    var iconDrawable: Drawable? = applicationInfo.loadIcon(GlobalContext.context.packageManager)
}

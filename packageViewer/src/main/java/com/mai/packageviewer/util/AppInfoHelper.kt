package com.mai.packageviewer.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.mai.packageviewer.data.AppInfo
import com.mai.packageviewer.data.BaseKVObject
import java.text.SimpleDateFormat
import java.util.LinkedList
import java.util.Vector
import java.util.concurrent.ConcurrentHashMap

/**
 * 通过多线程处理App信息的初始化
 * 耗时操作主要是获取label，测试机获取单个label时间约20ms
 * 使用过多线程并不会简单提升获取速度
 */
object AppInfoHelper {
    var isRunning = false

    private var threadList = ConcurrentHashMap<Int, Thread>()

    /**
     * 处理数据的初始化
     * @param list 数据
     * @param callback 回调
     * @param threads 线程数，默认为4
     */
    fun handle(list: MutableList<PackageInfo>, callback: AppInfoCallback, threads: Int = 4, packageManager: PackageManager) {
        isRunning = true
        val ret = Vector<MutableList<AppInfo>>(threads)
        // 根据threads分割数据
        val averageAssign = averageAssign(list, threads)
        for (i in 0 until threads) {
            val thread = Thread {
                val result = ArrayList<AppInfo>(averageAssign[i].size)
                averageAssign[i].forEach {
                    val appInfo = AppInfo(it, packageManager)
                    result.add(appInfo)
                }
                ret.add(result)

                //判断任务状态
                if (ret.size == threads) {
                    isRunning = false
                    callback.onResult(ret)
                    threadList.clear()
                } else {
                    try {
                        threadList.remove(i)
                    } catch (_: Exception) {

                    }
                }
            }
            threadList[i] = thread
            thread.start()
        }
    }

    /**
     * 均分List
     * @param source 源数据
     * @param n 份数
     * @return 分割后的List
     */
    private fun <T> averageAssign(source: List<T>, n: Int): List<List<T>> {
        val result: MutableList<List<T>> = ArrayList()
        var remainder = source.size % n
        val number = source.size / n
        var offset = 0
        for (i in 0 until n) {
            val value: List<T> =
                if (remainder > 0) {
                    val ret = source.subList(i * number + offset, (i + 1) * number + offset + 1)
                    remainder--
                    offset++
                    ret
                } else {
                    source.subList(i * number + offset, (i + 1) * number + offset)
                }
            result.add(value)
        }
        return result
    }

    /**
     * 强制结束线程
     */
    fun forceStop() {
        threadList.forEach { (_, v) ->
            try {
                if (v.isAlive) {
                    v.interrupt()
                }
            } catch (_: Exception) {

            }
        }
    }

    interface AppInfoCallback {
        fun onResult(ret: Vector<MutableList<AppInfo>>)
    }

    /**
     * 将AppInfo转换为详情页的键值对
     * 处理签名
     */
    fun AppInfo.toDetailList(): MutableList<BaseKVObject<String>> {
        val ret = LinkedList<BaseKVObject<String>>()
        ret.add(BaseKVObject("label", this.label))
        ret.add(BaseKVObject("packageName", this.packageName))
        ret.add(BaseKVObject("versionName", "${this.versionName}"))
        ret.add(BaseKVObject("versionCode", "${this.versionCode}"))
        if (this.minSdkVersion != -1)
            ret.add(BaseKVObject("minSdkVersion", "${this.minSdkVersion}"))
        ret.add(BaseKVObject("targetSdkVersion", "${this.targetSdkVersion}"))
        ret.add(
            BaseKVObject(
                "firstInstallTime",
                SimpleDateFormat.getDateTimeInstance().format(this.firstInstallTime)
            )
        )
        ret.add(
            BaseKVObject(
                "lastUpdateTime",
                SimpleDateFormat.getDateTimeInstance().format(this.lastUpdateTime)
            )
        )

        ret.add(BaseKVObject("systemApp", "${this.isSystemApp}"))

        ret.add(BaseKVObject("uid", "${this.uid}"))

        return ret
    }


}
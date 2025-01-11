package com.mai.packageviewer.view

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mai.packageviewer.R
import com.mai.packageviewer.activity.PackageViewerActivity
import com.mai.packageviewer.adapter.AppInfoDetailAdapter
import com.mai.packageviewer.data.AppInfo
import com.mai.packageviewer.databinding.DialogAppinfoDetailBinding
import com.mai.packageviewer.util.AppInfoHelper.toDetailList

/**
 * 详情页Dialog
 */
class AppInfoDetailDialog(val context: PackageViewerActivity, data: AppInfo) {

    val alertDialog: AlertDialog

    init {
        val binder = DialogAppinfoDetailBinding.inflate(LayoutInflater.from(context))
        binder.appInfoDetailRecyclerView.layoutManager = LinearLayoutManager(context)
        binder.appInfoDetailRecyclerView.adapter = AppInfoDetailAdapter(data.toDetailList())
        // 通过AlertDialog实现

        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.detail)
            .setCancelable(true)
            .setView(binder.root)
            .setOnCancelListener { PackageViewerActivity.dialogList = null }
        if (context.choose) {
            alertDialogBuilder
                .setTitle(R.string.choose)
                .setPositiveButton(R.string.choose) { _, _ ->
                    val intent = Intent()
                    intent.putExtra("label", data.label)
                    intent.putExtra("package_name", data.packageName)
                    intent.putExtra("version_name", data.versionName)
                    intent.putExtra("version_code", data.versionCode)
                    intent.putExtra("uid", data.uid)
                    context.setResult(RESULT_OK, intent)
                    context.finish()
                }
        }

        alertDialog = alertDialogBuilder.create()
        alertDialog.show()

        PackageViewerActivity.dialogList = this
    }

    fun isShowing(): Boolean {
        return alertDialog.isShowing
    }

    fun dismiss() {
        try {
            PackageViewerActivity.dialogList = null
            alertDialog.dismiss()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

}
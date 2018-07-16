package com.twiceyuan.editorvalidator.sample

import android.app.ProgressDialog
import android.content.Context
import android.os.SystemClock
import org.jetbrains.anko.runOnUiThread
import java.util.concurrent.Executors

/**
 * 异步验证手机号合法性
 */
@Suppress("DEPRECATION")
fun Context.checkPhoneNum(phone: String, resultCallback: (result: Boolean, message: String) -> Unit) {

    val progressBarDialog = ProgressDialog(this)
    progressBarDialog.setMessage("正在校验手机号合法性")
    progressBarDialog.show()

    Executors.newSingleThreadExecutor().execute {
        // mock network activity
        SystemClock.sleep(1000)
        progressBarDialog.dismiss()

        runOnUiThread {
            if (phone.startsWith("1")) {
                resultCallback(true, "valid")
            } else {
                resultCallback(false, "手机号第一位必须为 1")
            }
        }
    }
}

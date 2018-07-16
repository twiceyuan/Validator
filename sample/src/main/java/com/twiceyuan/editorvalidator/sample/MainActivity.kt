package com.twiceyuan.editorvalidator.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.twiceyuan.validator.beginValidate
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val validatorChain = beginValidate()
                .checkEmpty(editor = et_username, label = "用户名")
                .checkEmpty(editor = et_password, label = "密码")
                .checkRegex(editor = et_password, pattern = Regex("^\\w{8,16}"), message = "密码为8-16位的字符")
                .checkEmpty(editor = et_password_confirm, label = "确认密码")
                .confirmInput(editor = et_password_confirm, target = et_password, message = "两次密码输入不同")
                .checkEmpty(editor = et_phone, label = "手机号")
                .checkLength(editor = et_phone, minimumLen = 11, maximumLen = 12)
                .checkPhoneValidByApi(et_phone)

        btn_submit.setOnClickListener {
            validatorChain.checkAll {
                toast("串行验证通过")
            }
        }

        btn_submit_parallels.setOnClickListener {
            validatorChain.checkAllParallels {
                toast("并行验证通过")
            }
        }
    }
}

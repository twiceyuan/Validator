package com.twiceyuan.editorvalidator.sample

import android.widget.EditText
import com.twiceyuan.validator.ValidatorChain

/////////////////////////////////// EditText 相关基础封装 ///////////////////////////////////
/**
 * EditText 同步验证器
 */
fun ValidatorChain.checkWithEditor(editor: EditText, expression: () -> Boolean, message: String) =
        checkItem(expression) {
            editor.requestFocus()
            editor.error = message
        }

/**
 * EditText 异步验证
 */
fun ValidatorChain.checkAsyncWithEditor(
        editor: EditText,
        asyncCheckExpression: (resultDispatcher: (result: Boolean, message: String) -> Unit) -> Unit) =
        checkItemAsync { resultHandler ->
            asyncCheckExpression { result, message ->
                resultHandler(result)
                if (!result) {
                    editor.requestFocus()
                    editor.error = message
                }
            }
        }


/////////////////////////////////// EditText 相关实现封装 ///////////////////////////////////

/**
 * 验证确认输入
 */
fun ValidatorChain.confirmInput(editor: EditText, target: EditText, message: String) =
        checkWithEditor(editor, { editor.text.toString() == target.text.toString() }, message)

/**
 * 验证非空
 */
fun ValidatorChain.checkEmpty(
        editor: EditText,
        label: String = "") =
        checkWithEditor(editor, { editor.length() != 0 }, "${label}不能为空")

/**
 * 正则验证输入
 */
fun ValidatorChain.checkRegex(
        editor: EditText,
        pattern: Regex,
        message: String) =
        checkWithEditor(editor, { pattern.toPattern().matcher(editor.text.toString()).matches() }, message)

/**
 * 验证长度
 */
fun ValidatorChain.checkLength(
        editor: EditText,
        minimumLen: Int = 0,
        maximumLen: Int = Int.MAX_VALUE,
        message: (min: Int, max: Int) -> String = { min, max -> "长度必须在$min-$max" }) =
        checkWithEditor(
                editor = editor,
                expression = { editor.length() in minimumLen..maximumLen },
                message = message(minimumLen, maximumLen))

/**
 * 业务封装：通过网络请求验证手机号是否合法
 */
fun ValidatorChain.checkPhoneValidByApi(editor: EditText) =
        checkAsyncWithEditor(editor) { resultHandler ->
            editor.context.checkPhoneNum(editor.text.toString()) { result, message ->
                resultHandler(result, message)
            }
        }
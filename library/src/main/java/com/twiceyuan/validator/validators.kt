package com.twiceyuan.validator

import java.util.concurrent.atomic.AtomicInteger

fun beginValidate() = RootChain

/**
 * 异步校验结果接收器
 */
typealias CheckResultHandler = (result: Boolean) -> Unit

/**
 * 异步验证结果
 */
typealias CheckAsyncExpression = (resultHandler: CheckResultHandler) -> Unit

/**
 * 同步验证表达式，存储一个返回值为 Boolean 的表达式
 */
typealias CheckSyncExpression = () -> Boolean

/**
 * 验证器的根节点
 */
object RootChain : ValidatorChain(null)

/**
 * 验证器 同步类型验证节点
 *
 * [_failure] 存储失败回调
 * [_checkExpression] 存储同步验证表达式
 */
class SyncValidatorChain(
        previous: ValidatorChain?,
        val _failure: () -> Unit,
        val _checkExpression: CheckSyncExpression
) : ValidatorChain(previous)

/**
 * 验证器 异步类型验证节点
 *
 * [_checkExpression] 存储异步验证表达式
 */
class AsyncValidatorChain(
        previous: ValidatorChain?,
        val _checkExpression: CheckAsyncExpression
) : ValidatorChain(previous)

/**
 * 验证器节点
 *
 * 所有验证操作必须基于 [checkItem] 或者 [checkItemAsync] 来构造新的验证节点。[previous] 代表上一个节点
 */
open class ValidatorChain(private val previous: ValidatorChain?) {

    /**
     * 同步验证
     */
    fun checkItem(resultExpression: CheckSyncExpression, failure: () -> Unit): ValidatorChain =
            SyncValidatorChain(this, failure, resultExpression)

    /**
     * 异步验证
     */
    fun checkItemAsync(checkExpression: CheckAsyncExpression): ValidatorChain =
            AsyncValidatorChain(this, checkExpression)

    /**
     * 结束验证链，并验证之前所有节点，位置靠前的节点不通过时，不会验证后面的节点
     */
    fun checkAll(passedCallback: () -> Unit) {
        val itemIterator = reduceCheckItems().iterator()
        executeCheckItem(itemIterator) { passedCallback() }
    }

    /**
     * 执行一个验证节点，并由递归决定应该同步还是异步验证下一节点。
     */
    private fun executeCheckItem(itemIterator: Iterator<ValidatorChain>, allChecked: () -> Unit) {
        // 没有下一个节点时，代表所有节点验证通过
        if (!itemIterator.hasNext()) {
            allChecked()
            return
        }

        val checkItem = itemIterator.next()

        when (checkItem) {
            is SyncValidatorChain -> {
                val result = checkItem._checkExpression()
                if (result) {
                    // 验证通过时，验证下一个节点
                    executeCheckItem(itemIterator, allChecked)
                } else {
                    checkItem._failure()
                }
            }
            is AsyncValidatorChain -> {
                // 执行异步验证表达式
                checkItem._checkExpression { result ->
                    // 异步结果中获取是否验证成功，成功则验证下一个节点
                    if (result) {
                        executeCheckItem(itemIterator, allChecked)
                    }
                }
            }
            is RootChain -> {
                // 根节点无需操作，直接传递到下一节点
                executeCheckItem(itemIterator, allChecked)
            }
        }
    }

    /**
     * 获取所有节点。实际为遍历有 [ValidatorChain] 的 [previous] 构成的链表，并 reverse 来获取正序的 list。
     */
    private fun reduceCheckItems(): List<ValidatorChain> {
        val checkItems = mutableListOf(this) // add self first.
        var previousChain = previous
        while (previousChain != null) {
            checkItems.add(previousChain)
            previousChain = previousChain.previous
        }
        return checkItems.reversed()
    }

    /**
     * 并行验证所有节点
     */
    fun checkAllParallels(checkedCallback: () -> Unit) {
        val passedCount = AtomicInteger(0)
        val items = reduceCheckItems()
        val itemCount = items.size

        val countPassedHook = {
            if (passedCount.incrementAndGet() == itemCount) {
                checkedCallback()
            }
        }

        items.forEach {
            when (it) {
                is SyncValidatorChain -> {
                    val result = it._checkExpression()
                    if (result) {
                        countPassedHook()
                    } else {
                        it._failure()
                    }
                }
                is AsyncValidatorChain -> {
                    it._checkExpression { result ->
                        if (result) {
                            countPassedHook()
                        }
                    }
                }
                is RootChain -> Unit
            }
        }
    }
}

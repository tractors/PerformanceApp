package com.will.performanceapp.launchstarter.task

/**
 * 主线程任务
 */
abstract class  MainTask : Task() {

    override fun runOnMainThread(): Boolean {
        return true
    }
}
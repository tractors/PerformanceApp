package com.will.performanceapp.launchstarter.stat

import com.will.performanceapp.launchstarter.util.DispatcherLog
import java.util.concurrent.atomic.AtomicInteger

/**
 * 任务状态类
 */
class TaskStat {
    companion object{
        @Volatile private var sCurrentSituation :String = ""
        private val sBeans : MutableList<TaskStatBean> = ArrayList()
        private var sTaskDoneCount : AtomicInteger = AtomicInteger()
        // 是否开启统计
        private val sOpenLaunchStat:Boolean = false

        fun getCurrentSituation():String{
            return sCurrentSituation
        }

        fun setCurrentSituation(currentSituation:String){
            if (!sOpenLaunchStat){
                return
            }

            DispatcherLog.i("currentSituation   $currentSituation")
            sCurrentSituation = currentSituation
            setLaunchStat()
        }

        fun markTaskDone(){
            sTaskDoneCount.getAndIncrement()
        }

        private fun setLaunchStat() {
            val  bean:TaskStatBean = TaskStatBean()
            bean.situation = sCurrentSituation
            bean.count = sTaskDoneCount.get()
            sBeans.add(bean)
            sTaskDoneCount = AtomicInteger(0)

        }
    }
}
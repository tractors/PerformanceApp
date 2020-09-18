package com.will.performanceapp.launchstarter.task

import android.os.Looper
import android.os.Process
import androidx.core.os.TraceCompat
import com.will.performanceapp.launchstarter.TaskDispatcher
import com.will.performanceapp.launchstarter.stat.TaskStat
import com.will.performanceapp.launchstarter.util.DispatcherLog

/**
 * 任务真正执行的地方
 */
class DispatchRunnable : Runnable {
    private var mTask:Task
    private var mTaskDispatcher:TaskDispatcher?=null

    constructor(mTask: Task){
        this.mTask = mTask
    }

    constructor(task: Task, dispatcher: TaskDispatcher) {
        this.mTask = task
        this.mTaskDispatcher = dispatcher
    }


    override fun run() {
        TraceCompat.beginSection(mTask.javaClass.simpleName)
        DispatcherLog.i(mTask.javaClass.simpleName
                + " begin run" + "  Situation  " + TaskStat.getCurrentSituation())

        Process.setThreadPriority(mTask.priority())
        var startTime : Long = System.currentTimeMillis()

        mTask.setWaiting(true)
        mTask.waitToSatisfy()

        var waitTime:Long = System.currentTimeMillis()
        startTime = System.currentTimeMillis()

        //执行task
        mTask.setRunning(true)
        mTask.run()

        //执行task的尾部任务
        var tailRunnable : Runnable? = mTask.getTailRunnable()

        tailRunnable?.run()

        if (!mTask.needCall() || !mTask.runOnMainThread()){
            printTaskLog(startTime,waitTime)

            TaskStat.markTaskDone()
            mTask.setFinished(true)

            if (mTaskDispatcher != null){
                mTaskDispatcher!!.satisfyChildren(mTask)
                mTaskDispatcher!!.markTaskDone(mTask)
            }

            DispatcherLog.i(mTask.javaClass.simpleName + " finish");
        }

        TraceCompat.endSection()
    }

    /**
     * 打印出来Task执行的日志
     */
    private fun printTaskLog(startTime : Long,waitTime : Long){
        var runTime : Long = System.currentTimeMillis() - startTime
        if (DispatcherLog.isDebug()) {
            DispatcherLog.i(mTask.javaClass.simpleName + "  wait " + waitTime + "    run "
                    + runTime + "   isMain " + (Looper.getMainLooper() == Looper.myLooper())
                    + "  needWait " + (mTask.needWait() || (Looper.getMainLooper() == Looper.myLooper()))
                    + "  ThreadId " + Thread.currentThread().id
                    + "  ThreadName " + Thread.currentThread().name
                    + "  Situation  " + TaskStat.getCurrentSituation()
            );
        }
    }
}
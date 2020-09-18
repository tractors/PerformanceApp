package com.will.performanceapp.launchstarter

import android.content.Context
import android.os.Looper
import android.util.Log
import androidx.annotation.UiThread
import com.will.performanceapp.launchstarter.sort.TaskSortUtil
import com.will.performanceapp.launchstarter.stat.TaskStat
import com.will.performanceapp.launchstarter.task.DispatchRunnable
import com.will.performanceapp.launchstarter.task.Task
import com.will.performanceapp.launchstarter.task.TaskCallBack
import com.will.performanceapp.launchstarter.util.DispatcherLog
import com.will.performanceapp.util.Utils
import java.lang.RuntimeException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 启动器调用类
 */
const val WAITTIME :Int = 10000
class TaskDispatcher {
    private var mStartTime : Long = 0
    private var mFutures : MutableList<Future<*>> = ArrayList()
    private var mAllTasks : MutableList<Task> = ArrayList()
    private var mClsAllTasks : MutableList<Class<out Task>> = ArrayList()
    @Volatile private var mMainThreadTasks : MutableList<Task> = ArrayList()
    private var mCountDownLatch : CountDownLatch? = null

    //保存需要Wait的Task的数量
    private val mNeedWaitCount : AtomicInteger = AtomicInteger()

    //调用了await的时候还没结束的且需要等待的Task
    private var mNeedWaitTasks : MutableList<Task> = ArrayList()

    //已经结束了的Task
    private var mFinishedTasks : MutableList<Class<out Task>> = ArrayList(100)

    private var mDependedHashMap : HashMap<Class<out Task>,MutableList<Task>> = HashMap()

    //启动器分析的次数，统计下分析的耗时；
    private var mAnalyseCount : AtomicInteger = AtomicInteger()

    private constructor()

    companion object{
        private lateinit var sContext : Context
        private var sIsMainProcess : Boolean = false
        @Volatile private var sHasInit : Boolean = false

        /**
         * 初始化
         */
        fun init(context: Context){
            if (context != null){
                sContext = context
                sHasInit = true
                sIsMainProcess = Utils.isMainProcess(sContext)
            }
        }

        /**
         * 注意：每次获取的都是新对象
         */
        fun createInstance():TaskDispatcher{
            if (!sHasInit){
                throw RuntimeException("must call TaskDispatcher.init first")
            }
            return TaskDispatcher()
        }

        fun getContext():Context{
            return sContext
        }

        fun isMainProcess():Boolean{
            return sIsMainProcess
        }
    }


    fun addTask(task: Task):TaskDispatcher{
        if (task != null){
            collectDepends(task)
            mAllTasks.add(task)
            mClsAllTasks.add(task.javaClass)
            // 非主线程且需要wait的，主线程不需要CountDownLatch也是同步的
            if (ifNeedWait(task)){
                mNeedWaitTasks.add(task)
                mNeedWaitCount.getAndIncrement()
            }
        }
        return this
    }

    private fun collectDepends(task: Task) {
        if (task.dependsOn() != null && task.dependsOn()!!.isNotEmpty()){
            task.dependsOn()!!.forEach {
                if (mDependedHashMap[it] == null){
                    mDependedHashMap.put(it,ArrayList<Task>())
                }

                mDependedHashMap[it]?.add(task)
                if (mFinishedTasks.contains(it)){
                    task.satisfy()
                }
            }
        }
    }

    private fun ifNeedWait(task: Task): Boolean {
        return !task.runOnMainThread() && task.needWait()
    }

    @UiThread
    public fun start(){
        mStartTime = System.currentTimeMillis()
        if (Looper.getMainLooper() != Looper.myLooper()){
            throw RuntimeException("must be called from UiThread")
        }

        if (mAllTasks.size > 0){
            mAnalyseCount.getAndIncrement()
            printDependedMsg()
            mAllTasks = TaskSortUtil.getSortResult(mAllTasks, mClsAllTasks).toMutableList()
            mCountDownLatch = CountDownLatch(mNeedWaitCount.get())

            sendAndExecuteAsyncTasks()

            DispatcherLog.i("task analyse cost" + (System.currentTimeMillis() - mStartTime) + "  begin main ")
            executeTaskMain()
        }

        DispatcherLog.i("task analyse cost startTime cost " + (System.currentTimeMillis() - mStartTime))
    }

    public fun cancel(){
        for (future in mFutures){
            future.cancel(true)
        }
    }

    private fun executeTaskMain(){
        mStartTime = System.currentTimeMillis()
        mMainThreadTasks.forEach {
            var time : Long = System.currentTimeMillis()
            DispatchRunnable(it,this).run()

            DispatcherLog.i("real main " + it.javaClass.simpleName + " cost   " +
                    (System.currentTimeMillis() - time));
        }

        DispatcherLog.i("maintask cost " + (System.currentTimeMillis() - mStartTime));
    }

    private fun sendAndExecuteAsyncTasks(){
        mAllTasks.forEach {
            if (it.onlyInMainProcess() && !sIsMainProcess){
                markTaskDone(it)
            } else {
                sendTaskReal(it)
            }

            it.setSend(true)
        }
    }

    /**
     * 查看被依赖的信息
     */
    private fun printDependedMsg(){
        DispatcherLog.i("needWait size : " + (mNeedWaitCount.get()))
        if (false){
            mDependedHashMap.keys.forEach {
                DispatcherLog.i("cls " + it.javaClass.simpleName+ "   " + mDependedHashMap.get(it)?.size)
                val task : MutableList<Task>? = mDependedHashMap.get(it)
                task?.forEach { t ->
                    DispatcherLog.i("t       " + t.javaClass.simpleName)
                }
            }
        }
    }

    /**
     * 通知Children一个前置任务已完成
     */
    public fun satisfyChildren(launchTask: Task){
       val arrayList : MutableList<Task>? = mDependedHashMap[launchTask.javaClass]
        if (arrayList != null && arrayList.size > 0){
            arrayList.forEach {
                it.satisfy()
            }
        }

    }

    public fun markTaskDone(task: Task){
        if (ifNeedWait(task)){
            mFinishedTasks.add(task.javaClass)
            mNeedWaitTasks.remove(task)
            mCountDownLatch?.countDown()
            mNeedWaitCount.getAndDecrement()
        }
    }

    private fun sendTaskReal(task: Task){
        if (task.runOnMainThread()){
            mMainThreadTasks.add(task)

            if (task.needCall()){
                task.setTaskCallBack(object : TaskCallBack{
                    override fun call() {
                        TaskStat.markTaskDone()
                        task.setFinished(true)
                        satisfyChildren(task)
                        markTaskDone(task)

                        DispatcherLog.i(task.javaClass.simpleName + " finish")

                        Log.i("testLog", "call")
                    }

                })
            }
        } else {

            // 直接发，是否执行取决于具体线程池
            var future : Future<*> = task.runOn().submit(DispatchRunnable(task,this))

            mFutures.add(future)
        }
    }

    public fun executeTask(task: Task){
        if (ifNeedWait(task)){
            mNeedWaitCount.getAndIncrement()
        }

        task.runOn().execute(DispatchRunnable(task,this))
    }

    @UiThread
    public fun await(){
        try {
            if (DispatcherLog.isDebug()) {
                DispatcherLog.i("still has " + mNeedWaitCount.get())

                mNeedWaitTasks.forEach {
                    DispatcherLog.i("needWait: " + it.javaClass.simpleName)
                }
            }

            if (mNeedWaitCount.get() > 0) {
                mCountDownLatch?.await(WAITTIME.toLong(), TimeUnit.MILLISECONDS);
            }
        } catch (e:InterruptedException ) {
            e.printStackTrace()
        }
    }
}
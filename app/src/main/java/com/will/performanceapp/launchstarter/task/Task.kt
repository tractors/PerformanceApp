package com.will.performanceapp.launchstarter.task

import android.content.Context
import android.os.Process
import com.will.performanceapp.launchstarter.TaskDispatcher
import com.will.performanceapp.launchstarter.util.DispatcherExecutor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

/**
 * 任务实现类
 */
abstract class Task : ITask{
    protected val mTag : String = javaClass.simpleName
    protected val mContext:Context = TaskDispatcher.getContext()

    // 当前进程是否是主进程
    protected val mIsMainProcess : Boolean = TaskDispatcher.isMainProcess()

    // 是否正在等待
    @Volatile private var mIsWaiting : Boolean = false

    // 是否正在执行
    @Volatile private var mIsRunning:Boolean = false
    // Task是否执行完成
    @Volatile private var mIsFinished : Boolean = false
    // Task是否已经被分发
    @Volatile private var mIsSend : Boolean = false

    // 当前Task依赖的Task数量（需要等待被依赖的Task执行完毕才能执行自己），默认没有依赖
    private val mDepends : CountDownLatch = CountDownLatch(if(dependsOn() == null) 0 else dependsOn()!!.size)

    /**
     * 当前Task等待，让依赖的Task先执行
     */
    public fun waitToSatisfy(){
        try {
            mDepends.await();
        } catch (e:InterruptedException) {
            e.printStackTrace();
        }
    }

    /**
     * 依赖的Task执行完一个
     */
    public fun satisfy(){
        mDepends.countDown()
    }

    /**
     * 是否需要尽快执行，解决特殊场景的问题：一个Task耗时非常多但是优先级却一般，很有可能开始的时间较晚，
     * 导致最后只是在等它，这种可以早开始。
     */
    public fun needRunAsSoon():Boolean{
        return false
    }

    /**
     * Task的优先级，运行在主线程则不要去改优先级
     */
    override fun priority(): Int {
        return Process.THREAD_PRIORITY_BACKGROUND
    }

    /**
     * Task执行在哪个线程池，默认在IO的线程池；
     * CPU 密集型的一定要切换到DispatcherExecutor.getCPUExecutor();
     */
    override fun runOn(): ExecutorService {
        return DispatcherExecutor.getIOExecutor()
    }

    /**
     * 异步线程执行的Task是否需要在被调用await的时候等待，默认不需要
     */
    override fun needWait(): Boolean {
        return false
    }

    /**
     * 当前Task依赖的Task集合（需要等待被依赖的Task执行完毕才能执行自己），默认没有依赖
     */
    final override fun dependsOn(): List<Class<out Task?>>? {
        return null
    }

    override fun runOnMainThread(): Boolean {
        return false
    }

    override fun getTailRunnable(): Runnable? {
        return null
    }

    override fun setTaskCallBack(callback: TaskCallBack) {

    }

    override fun needCall(): Boolean {
        return false
    }

    /**
     * 是否只在主进程，默认是
     */
    override fun onlyInMainProcess(): Boolean {
        return true
    }

    public fun isRunning():Boolean {
        return mIsRunning
    }

    public fun setRunning(isRunning : Boolean) {
        this.mIsRunning = isRunning
    }

    public fun isFinished() :Boolean {
        return mIsFinished
    }

    public fun setFinished(finished : Boolean) {
        mIsFinished = finished
    }

    public fun isSend():Boolean {
        return mIsSend
    }

    public fun setSend(send :Boolean) {
        mIsSend = send
    }

    public fun isWaiting() : Boolean {
        return mIsWaiting
    }

    public fun setWaiting(isWaiting : Boolean) {
        this.mIsWaiting = isWaiting
    }
}
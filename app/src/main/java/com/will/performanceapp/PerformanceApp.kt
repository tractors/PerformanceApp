package com.will.performanceapp

import android.app.Application
import com.will.performanceapp.util.LaunchTimer
import com.will.performanceapp.util.LogUtils
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class PerformanceApp : Application() {



    companion object{
        private lateinit var mApplication : Application
        public fun getApplication():Application{
            return mApplication
        }
    }

    private val mCountDownLatch : CountDownLatch = CountDownLatch(1)
    final val CPU_COUNT : Int = Runtime.getRuntime().availableProcessors()
    final val CORE_POOL_SIZE : Int = max(2, min(CPU_COUNT -1,4))
    final val MAXIMUM_POOL_SIZE : Int = CPU_COUNT *2 +1
    final val KEEP_ALIVE_SECONDS: Int = 30

//    private mLocationListener : AMapLocationListener = AMapLocationListener
    override fun onCreate() {
        super.onCreate()

        mApplication = this
    LaunchTimer.startRecord()
        val service : ExecutorService = Executors.newFixedThreadPool(CORE_POOL_SIZE)
    service.submit { Runnable {
        initUmeng()
        mCountDownLatch.countDown()
    } }

    try {
        mCountDownLatch.await()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
    LaunchTimer.endRecord()

        initAMap()

        initFresco()

        getDeviceId()

        initJpush()

        initWeex()

        initStetho()

    }

    private fun initUmeng() {
        TODO("Not yet implemented")
    }

    private fun initAMap() {
        LogUtils.i("initAMap")
    }

    private fun initFresco() {
        LogUtils.i("initFresco")
    }

    private fun getDeviceId() {
        LogUtils.i("getDeviceId")
    }

    private fun initJpush() {
        LogUtils.i("initJpush")
    }

    private fun initWeex() {
        LogUtils.i("initWeex")
    }

    private fun initStetho() {
        LogUtils.i("initStetho")
    }


}
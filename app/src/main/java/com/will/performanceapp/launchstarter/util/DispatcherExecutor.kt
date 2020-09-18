package com.will.performanceapp.launchstarter.util

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min

/**
 * 线程池调度控制器类
 */
class DispatcherExecutor {
    companion object{

        private lateinit var sCPUThreadPoolExecutor : ThreadPoolExecutor
        private lateinit var sIOThreadPoolExecutor : ExecutorService
        private val CPU_COUNT : Int = Runtime.getRuntime().availableProcessors()
        private val CORE_POOL_SIZE = max(2, min(CPU_COUNT-1,5))
        private val MAXIMUM_POOL_SIZE = CORE_POOL_SIZE
        private const val KEEP_ALIVE_SECONDS : Long= 5

        private val sPoolWorkQueue:BlockingQueue<Runnable>  = LinkedBlockingDeque()
        private val sThreadFactory : DefaultThreadFactory = DefaultThreadFactory()
        private val sHandler : RejectedExecutionHandler = RejectedExecutionHandler { r, _ ->
            Executors.newCachedThreadPool().execute(r)
        }

        init {
            sCPUThreadPoolExecutor = ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
            sPoolWorkQueue, sThreadFactory, sHandler)
            sCPUThreadPoolExecutor.allowCoreThreadTimeOut(true)
            sIOThreadPoolExecutor = Executors.newCachedThreadPool(sThreadFactory)
        }

        /**
         * 获取CPU线程池
         */
        fun getCPUExecutor():ThreadPoolExecutor{
            return sCPUThreadPoolExecutor
        }

        /**
         * 获取IO线程池
         */
        fun getIOExecutor():ExecutorService{
            return sIOThreadPoolExecutor
        }

        /**
         * 默认线程工厂类
         */
        private class DefaultThreadFactory : ThreadFactory{
            companion object{
                private val poolNumber:AtomicInteger = AtomicInteger(1)
            }

            private var group: ThreadGroup
            private val threadNumber : AtomicInteger = AtomicInteger(1)
            private var namePrefix : String?

            constructor() {
                val s : SecurityManager = System.getSecurityManager()
                this.group = if (s != null) s.threadGroup else Thread.currentThread().threadGroup
                this.namePrefix = "TaskDispatcherPool-" +
                        poolNumber.getAndIncrement() +
                        "-Thread-"
            }

            override fun newThread(r: Runnable?): Thread {
                val  t :Thread = Thread(group,r,namePrefix + threadNumber.getAndIncrement(),0)
                if (t.isDaemon)
                    t.isDaemon = false

                if (t.priority != Thread.NORM_PRIORITY)
                    t.priority = Thread.NORM_PRIORITY
                return t
            }

        }
    }


}
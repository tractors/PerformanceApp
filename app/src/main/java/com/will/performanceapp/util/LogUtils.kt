package com.will.performanceapp.util

import android.util.Log
import com.will.performanceapp.PerformanceApp
import java.util.concurrent.ExecutorService

const val TAG : String = "performance"
class LogUtils {

    companion object{
        private  var sExecutorService : ExecutorService?=null

        public fun setExecutor(executorService: ExecutorService){
            this.sExecutorService = executorService
        }

        public fun i(msg : String){
            if (Utils.isMainProcess(PerformanceApp.getApplication())){
                Log.i(TAG,msg);
            }

            // 异步
            if(sExecutorService != null){
//            sExecutorService.execute();
            }
        }
    }

}
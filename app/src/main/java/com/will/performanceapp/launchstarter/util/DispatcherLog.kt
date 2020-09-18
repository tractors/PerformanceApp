package com.will.performanceapp.launchstarter.util

import android.util.Log

/**
 * 控制器日志输出
 */
class DispatcherLog {
    companion object{
        private var sDebug : Boolean = true

        fun i(msg:String){
            if (!sDebug){
                return
            }
            Log.i("task",msg)
        }

        fun isDebug():Boolean{
            return sDebug
        }

        fun setDebug(debug:Boolean){
            sDebug = debug
        }
    }
}
package com.will.performanceapp.util

import android.util.Log


class LaunchTimer {

    companion object{
        private var sTime : Long = 0
        public fun startRecord(){
            sTime = System.currentTimeMillis()
        }

        public fun endRecord(){
            endRecord("")
        }

        public fun endRecord(msg : String){
            var cost : Long = System.currentTimeMillis() - sTime
            Log.i(TAG,msg + "cost: " + cost)
        }
    }


}
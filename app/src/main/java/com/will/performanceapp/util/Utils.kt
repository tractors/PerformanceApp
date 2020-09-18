package com.will.performanceapp.util

import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

class Utils {


    companion object{

        private  var sCurProcessName: String = ""
        public fun isMainProcess(context: Context): Boolean {
            var processName = getCurProcessName(context)
            if (processName != null && processName.contains(":")) {
                return false
            }

            return (processName != null && processName == context.packageName)
        }


        private fun getCurProcessName(context: Context): String {
            val procName: String = sCurProcessName
            if (!TextUtils.isEmpty(procName)) {
                return procName
            }

            try {
                val pid: Int = android.os.Process.myPid()

                val mActivityManager: ActivityManager =
                    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

                for ((index, appProcess) in mActivityManager.runningAppProcesses.withIndex()) {
                    if (appProcess.pid == pid) {
                        sCurProcessName = appProcess.processName
                        return sCurProcessName
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            sCurProcessName = getCurProcessNameFromProc(context)

            return sCurProcessName
        }

        private fun getCurProcessNameFromProc(context: Context): String {
            var cmdlineReader: BufferedReader? = null
            try {
                cmdlineReader = BufferedReader(
                    InputStreamReader(
                        FileInputStream(
                            "/proc/" + android.os.Process.myPid() + "/cmdline"
                        ), "iso-8859-1"
                    )
                )

                var processName: StringBuffer = StringBuffer()

                var c: Int = cmdlineReader.read()
                while (c > 0) {
                    processName.append(c as Char)
                }

                return processName.toString()
            } catch (e: Throwable) {
            } finally {
                try {
                    cmdlineReader?.close()
                } catch (e: Exception) {
                }
            }

            return null.toString()
        }
    }

}
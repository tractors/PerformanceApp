package com.will.performanceapp.launchstarter.sort

import androidx.annotation.NonNull
import com.will.performanceapp.launchstarter.task.Task
import com.will.performanceapp.launchstarter.util.DispatcherLog
import java.lang.IllegalStateException

/**
 * 任务优先级工具类
 */
class TaskSortUtil {
    companion object{
        //高优先级的Task
        private var sNewTasksHigh : MutableList<Task> = ArrayList()

        /**
         * 任务的有向无环图的拓扑排序
         */
        @Synchronized public fun getSortResult(originTasks : List<Task>, clsLaunchTasks : MutableList<Class<out Task>>):List<Task>{
            var makeTime : Long = System.currentTimeMillis()

            var dependSet : MutableSet<Int> = androidx.collection.ArraySet()

            var graph:Graph  = Graph(originTasks.size)

            for (i in originTasks.indices){
                var task:Task = originTasks[i]

                if (task.isSend() || task.dependsOn() == null || task.dependsOn()!!.isEmpty()){
                    continue
                }

                task.dependsOn()!!.forEach {
                    var indexOfDepend = getIndexOfTask(originTasks,clsLaunchTasks,it)

                    if (indexOfDepend < 0){
                        throw IllegalStateException(task.javaClass.simpleName +
                                " depends on " + it.simpleName + " can not be found in task list ")
                    }

                    dependSet.add(indexOfDepend)
                    graph.addEdge(indexOfDepend,i)
                }
            }

            var indexList : List<Int> = graph.topologicalSort()

            var newTaskAll:List<Task> = getResultTasks(originTasks,dependSet,indexList)

            DispatcherLog.i("task analyse cost makeTime " + (System.currentTimeMillis() - makeTime))

            printAllTaskName(newTaskAll)

            return newTaskAll
        }

        @NonNull
        private fun getResultTasks(originTasks:List<Task>, dependSet:Set<Int>, indexList : List<Int>):List<Task>{
            var newTasksAll : MutableList<Task> = ArrayList(originTasks.size)
            // 被别人依赖的
            var newTaskDepended : MutableList<Task> = ArrayList()

            // 没有依赖的
            var newTasksWithOutDepend : MutableList<Task> = ArrayList()

            // 需要提升自己优先级的，先执行（这个先是相对于没有依赖的先）
            var newTasksRunAsSoon:MutableList<Task> = ArrayList()

            indexList.forEach {
                if (dependSet.contains(it)){
                    newTaskDepended.add(originTasks[it])
                } else {
                    var task:Task = originTasks.get(it)
                    if (task.needRunAsSoon()){
                        newTasksRunAsSoon.add(task)
                    } else {
                        newTasksWithOutDepend.add(task)
                    }
                }
            }

            // 顺序：被别人依赖的————>需要提升自己优先级的————>需要被等待的————>没有依赖的

            sNewTasksHigh.addAll(newTaskDepended)
            sNewTasksHigh.addAll(newTasksRunAsSoon)
            newTasksAll.addAll(sNewTasksHigh)
            newTasksAll.addAll(newTasksWithOutDepend)
            return newTasksAll
        }

        private fun printAllTaskName(newTasksAll:List<Task>){
            if (true){
                return
            }
            newTasksAll.forEach {
                DispatcherLog.i(it.javaClass.simpleName)
            }
        }

        fun getTasksHigh():List<Task>{
            return sNewTasksHigh
        }

        /**
         * 获取任务在任务列表中的index
         */
        private fun getIndexOfTask(originTasks : List<Task>,clsLaunchTasks:List<Class<out Task>>,cls : Class<*>):Int{
            val index:Int = clsLaunchTasks.indexOf(cls)
            if (index >= 0){
                return index
            }

            // 仅仅是保护性代码
            val size:Int = originTasks.size

            for (i in 0 until size){
                if (cls.simpleName == originTasks[i].javaClass.simpleName){
                    return i
                }
            }

            return index
        }
    }
}
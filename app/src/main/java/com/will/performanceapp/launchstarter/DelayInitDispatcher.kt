package com.will.performanceapp.launchstarter

import android.os.Looper
import android.os.MessageQueue
import com.will.performanceapp.launchstarter.task.DispatchRunnable
import com.will.performanceapp.launchstarter.task.Task
import java.util.*

class DelayInitDispatcher {
    private val mDelayTasks:Queue<Task> = LinkedList()

    private val mIdleHandler : MessageQueue.IdleHandler = MessageQueue.IdleHandler {
        if (mDelayTasks.size > 0){
            val task:Task = mDelayTasks.poll()
            DispatchRunnable(task).run()
        }

        !mDelayTasks.isEmpty()
    }

    public fun addTask(task: Task):DelayInitDispatcher{
        mDelayTasks.add(task)
        return this
    }

    public fun start(){
        Looper.myQueue().addIdleHandler(mIdleHandler)
    }
}
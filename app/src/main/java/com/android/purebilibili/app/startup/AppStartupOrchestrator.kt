package com.android.purebilibili.app

import android.os.Build
import com.android.purebilibili.core.ui.performance.PerformanceDebugCounter
import com.android.purebilibili.core.ui.performance.PerformanceObservability
import com.android.purebilibili.core.ui.performance.PerformanceTraceSection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class AppStartupOrchestrator(
    sdkInt: Int = Build.VERSION.SDK_INT,
    deferredDelayMs: Long = PureApplicationRuntimeConfig.deferredNonCriticalStartupDelayMs(),
    dex2OatDelayMs: Long = PureApplicationRuntimeConfig.dex2OatProfileInstallDelayMs(),
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val onTaskFailure: (AppStartupTask, Throwable) -> Unit = { _, _ -> },
) {
    private val tasks = defaultAppStartupTasks(
        sdkInt = sdkInt,
        deferredDelayMs = deferredDelayMs,
        dex2OatDelayMs = dex2OatDelayMs,
    )
    private val completionById = tasks.associate { it.id to CompletableDeferred<Unit>() }
    private val startedTriggers = ConcurrentHashMap.newKeySet<StartupTrigger>()

    init {
        check(validateStartupTaskGraph(tasks)) { "Invalid startup task dependency graph" }
    }

    fun startupTasks(): List<AppStartupTask> = tasks

    fun start(
        trigger: StartupTrigger,
        scope: CoroutineScope,
        taskRunner: suspend (AppStartupTask) -> Unit,
    ): Job {
        if (!startedTriggers.add(trigger)) {
            return scope.launch { }
        }

        return scope.launch {
            supervisorScope {
                tasks.asSequence()
                    .filter { it.trigger == trigger }
                    .map { task ->
                        launch {
                            task.dependencies.forEach { dependencyId ->
                                completionById.getValue(dependencyId).await()
                            }
                            try {
                                if (task.delayMs > 0L) delay(task.delayMs)
                                withContext(dispatcherFor(task.dispatcher)) {
                                    PerformanceObservability.increment(
                                        PerformanceDebugCounter.STARTUP_TASK_TRIGGERED
                                    )
                                    PerformanceObservability.trace(
                                        PerformanceTraceSection.STARTUP_TASK
                                    ) {
                                        try {
                                            taskRunner(task)
                                        } finally {
                                            PerformanceObservability.increment(
                                                PerformanceDebugCounter.STARTUP_TASK_COMPLETED
                                            )
                                        }
                                    }
                                }
                            } catch (throwable: Throwable) {
                                if (throwable is kotlinx.coroutines.CancellationException) throw throwable
                                onTaskFailure(task, throwable)
                            } finally {
                                completionById.getValue(task.id).complete(Unit)
                            }
                        }
                    }
                    .toList()
                    .joinAll()
            }
        }
    }

    private fun dispatcherFor(dispatcher: StartupDispatcher): CoroutineDispatcher = when (dispatcher) {
        StartupDispatcher.MAIN -> mainDispatcher
        StartupDispatcher.IO -> ioDispatcher
        StartupDispatcher.DEFAULT -> defaultDispatcher
    }
}

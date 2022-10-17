/*
 * Copyright (C) 2021 Kira Weinlein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kiwi.quickseq

import com.kiwi.quickseq.QuickSeqLogLine.Companion.diagramEndMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.diagramStartMarker
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/* ktlint-disable no-consecutive-blank-lines */

/**
 * The Logger...
 * - saves the execution logs to a designated file ([saveToLogFile])
 * - keeps track of the call chain during test runtime (see [callerClass], [QuickSeqLogContext], [saveDeferredLog], [printDeferredLogIfNeeded])
 */
object QuickSeqLogger {

    val generatedFilesDirName = "sequence-diagrams"
    val logDirName = "$generatedFilesDirName/logs"
    val logFilePath = "$logDirName/Log.txt"
    val logFile = File(logFilePath)

    var insideDiagram = false

    fun saveToLogFile(line: Any?) {
        line?.toString()?.let {
            if (it.contains(diagramStartMarker)) insideDiagram = true
            if (insideDiagram) {
                File(logDirName).let { dir -> if (!dir.exists()) dir.mkdirs() }
                it.split("\n").forEach {
                    logFile.appendText(if (!it.isBlank()) "${getFormattedLocalTime()}$it\n" else "\n")
                }
            }
            if (it.contains(diagramEndMarker)) insideDiagram = false
        }
    }

    fun getFormattedLocalTime() =
        ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd/HH-mm-ss-z"))!!



    // GET THE NAMES OF THE CURRENT FUNCTION AND ITS CALLER FROM STACK TRACE ===========================================

    fun stackTraceFunction(index: Int) = Thread.currentThread().stackTrace[index].methodName.toString()
    fun stackTraceClass(index: Int): String = Thread.currentThread().stackTrace[index].className.toString().run {
        val className = this.split(".").last()
        if (className.endsWith("Kt")) className.removeSuffix("Kt") + ".kt" else className
    }

    val ownIndex = 3 // 0: Thread.getStackTrace(), 1: stackTraceClass/stackTraceFunction, 2: ownClass/ownLogContext, 3: the function who called this
    fun ownClass() = stackTraceClass(ownIndex)
    fun ownLogContext() = "${stackTraceClass(ownIndex)}@${stackTraceFunction(ownIndex)}"

    val callerIndex = 4
    val testRunnerClass = "Test_Runner"
    fun callerClass(isEntryPoint: Boolean) = if (isEntryPoint) testRunnerClass else stackTraceClass(callerIndex)
    fun callerLogContext(isEntryPoint: Boolean) =
        if (isEntryPoint) testRunnerClass else "${stackTraceClass(callerIndex)}@${stackTraceFunction(callerIndex)}"



    // KEEP TRACK OF LOG LINES THAT MUST BE LOGGED LATER ===============================================================

    /**
     * The LogContext is an implementation of the Composite pattern. It represents the current call stack,
     * ignoring all methods which were not transformed by QuickSeq.
     *
     * This is needed when something must be logged right after a function returns:
     * In this case, the function who called must do the job ([printDeferredLogIfNeeded]).
     *
     * You can find an illustration in the README of the quickseq repo.
     */
    private class QuickSeqLogContext(
        val id: String,
        val nestedContexts: MutableList<QuickSeqLogContext> = mutableListOf(),
        val lines: MutableList<String> = mutableListOf(),
        val parent: QuickSeqLogContext? = null
    )

    private var logContextHolder = mutableMapOf<String, QuickSeqLogContext>()

    fun clearLogContext() {
        logContextHolder = mutableMapOf()
    }

    /**
     * Imagine this as "I am ClassB@functionB and I have been called by ClassA@functionA.
     * Please remember that, in case I need to delegate something to the function who called me."
     */
    fun registerLogContext(newContextId: String, parentContextId: String) {
        if (insideDiagram && newContextId != parentContextId) {
            val parentContext: QuickSeqLogContext =
                logContextHolder[parentContextId] ?: run {
                    logContextHolder[parentContextId] = QuickSeqLogContext(parentContextId)
                    logContextHolder[parentContextId]!!
                }
            val newContext = QuickSeqLogContext(newContextId, parent = parentContext)
            logContextHolder[newContextId] = newContext
            parentContext.nestedContexts.add(newContext)
        }
    }

    /**
     * "Buffers" the current log line to be logged later (see [printDeferredLogIfNeeded]).
     * This is necessary if it cannot be logged immediately.
     */
    fun saveDeferredLog(contextId: String, line: String) {
        if (insideDiagram) logContextHolder[contextId]?.lines?.add(line)
    }

    /**
     * Checks if any "buffered" log lines (see [saveDeferredLog]) should be printed, and if yes, prints them recursively.
     */
    fun printDeferredLogIfNeeded(
        thisContextId: String,
        shouldCheckParent: Boolean = true,
        ignoreContextId: String? = null
    ) {
        if (thisContextId == ignoreContextId) return
        if (!insideDiagram) return

        logContextHolder[thisContextId]?.apply {
            if (shouldCheckParent) parent?.let {
                printDeferredLogIfNeeded(parent.id, false, thisContextId)
            }

            nestedContexts.apply {
                while (isNotEmpty()) {
                    printDeferredLogIfNeeded(last().id, false)
                    removeAt(lastIndex)
                }
            }

            lines.apply {
                while (isNotEmpty()) {
                    saveToLogFile(last())
                    removeAt(lastIndex)
                }
            }
        }
    }
}

/* ktlint-enable no-consecutive-blank-lines */

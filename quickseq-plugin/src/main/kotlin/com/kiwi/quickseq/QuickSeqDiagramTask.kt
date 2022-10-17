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

import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileNotFoundException

/**
 * This class offers two "actions" which can be called from a Gradle script:
 * - [generate] translates the log file into PlantUML sequence diagram syntax
 * - [clearLogsAndDiagrams] moves the log and PlantUML files to a backup folder (or deletes them, if explicitly stated)
 */

open class QuickSeqDiagramTask(
    var projectRootDir: String,
    val generateUnfilteredDiagrams: Boolean = true,
    val maxRowsPerDiagram: Int = 50
) {

    init {
        if (!projectRootDir.isBlank()) projectRootDir = projectRootDir + "/"
    }

    /**
     * Translates the log file into PlantUML sequence diagram syntax.
     */
    @TaskAction
    fun generate() {
        println("\nQuickSeq: Generating sequence diagrams...")
        try {
            val lines = File("$projectRootDir${QuickSeqLogger.logFilePath}").readLines().map { QuickSeqLogLine(it) }
            val diagrams = lines.splitPerDiagram()
            for (diagram in diagrams) {
                diagram.convertToPumlFiles()
            }
            println("\nQuickSeq: Finished diagram creation.")
        } catch (ex: Exception) {
            when (ex) {
                is FileNotFoundException -> {
                    println(
                        "WARNING: There was no log file (${QuickSeqLogger.logFilePath}) to extract " +
                            "sequence diagrams from. Aborting Gradle task QuickSeqDiagramTask.generate()\n"
                    )
                }
                else -> throw ex
            }
        }
    }

    /**
     * Moves the log and PlantUML files to a backup folder (or deletes them, if explicitly stated).
     */
    @TaskAction
    fun clearLogsAndDiagrams(saveBackup: Boolean = true) {
        if (saveBackup) println("\nQuickSeq: Creating backup of old diagrams...")
        val time = File("$projectRootDir${QuickSeqLogger.logFilePath}").let {
            (
                if (it.exists())
                    it.readLines().find { line -> line.isNotBlank() }?.split(QuickSeqLogLine.markerSeparator)?.first()?.trim()
                else null
                ) ?: QuickSeqLogger.getFormattedLocalTime()
        }

        for (folderName in listOf("logs", "diagrams")) {
            File("$quickSeqDirName/backup/$time/$folderName").let { targetDir ->
                File("$quickSeqDirName/$folderName").let { originalDir ->
                    if (originalDir.exists()) {
                        if (saveBackup) {
                            if (!targetDir.exists()) targetDir.mkdirs()
                            originalDir.copyRecursively(targetDir)
                        }
                        originalDir.deleteRecursively()
                    }
                }
            }
        }
    }

    // FILE UTILS =======================================================================

    private val quickSeqDirName = "$projectRootDir${QuickSeqLogger.generatedFilesDirName}"
    private val diagramDirName = "$quickSeqDirName/diagrams"
    val filteredDiagramDirName = "$diagramDirName/only-annotated-methods"
    val fullDiagramDirName = "$diagramDirName/all-methods"

    private fun List<QuickSeqLogLine>.splitPerDiagram(): MutableList<List<QuickSeqLogLine>> {
        val diagrams = mutableListOf<List<QuickSeqLogLine>>()
        var lastDiagramStartIndex = 0

        this.forEachIndexed { currentIndex, line ->
            if (line.isDiagramStart) lastDiagramStartIndex = currentIndex
            if (line.isDiagramEnd) diagrams.add(slice(lastDiagramStartIndex..currentIndex))
        }
        return diagrams
    }

    private fun List<QuickSeqLogLine>.convertToPumlFiles() {
        val title = this[0].diagramTitle!!
        val fileName = title.split(" ").joinToString("_") + ".puml"

        listOf(filteredDiagramDirName, fullDiagramDirName).forEach {
            if (!File(it).exists()) File(it).mkdirs()
        }

        File("$filteredDiagramDirName/$fileName").createDiagram(title, this.drop(1))
        if (generateUnfilteredDiagrams)
            File("$fullDiagramDirName/ALL_$fileName").createFullDiagram(title, this.drop(1))
    }

    private fun File.createDiagram(title: String, lines: List<QuickSeqLogLine>) {
        writeText("@startuml\n")
        appendText("title $title${lines.rowLimitWarning(true)}\\n\n")

        var previousLineWasAnnotated = true
        var lastAnnotatedCalledClass = ""
        var lastAnnotatedCalledMethod = ""

        for (line in lines.truncated(true)) {
            if (line.isCallOrReturn && line.isAnnotated) {
                if (!line.isSilentReturn) {
                    if (!previousLineWasAnnotated && !(line.isReturn && lastAnnotatedCalledMethod == line.functionId)) {
                        val indirectMarker = "<b><font color=2060DD>[indirect]</b>"
                        if (line.isCall) appendText(
                            "$lastAnnotatedCalledClass -> ${line.leftClass}: " +
                                "$indirectMarker\nactivate ${line.leftClass}\n"
                        )
                        else appendText(
                            "${line.rightClass} <-- $lastAnnotatedCalledClass: $indirectMarker\n" +
                                "deactivate $lastAnnotatedCalledClass\n"
                        )
                    }
                    appendText(line.toPlantUmlLine()!!)
                }
                lastAnnotatedCalledClass = line.rightClass!!
                if (line.isCall) lastAnnotatedCalledMethod = line.functionId

                appendText(line.activationLine!!)
            }
            previousLineWasAnnotated = line.isAnnotated
        }
        appendText("@enduml\n")
    }

    private fun File.createFullDiagram(title: String, lines: List<QuickSeqLogLine>) {
        writeText("@startuml\n")
        appendText("title $title${lines.rowLimitWarning(false)}\\n\n")
        for (line in lines.truncated(false)) {
            if (line.isCallOrReturn) {
                appendText(line.toPlantUmlLine()!!)
                appendText(line.activationLine!!)
            }
        }
        appendText("@enduml\n")
    }

    private fun List<QuickSeqLogLine>.truncated(onlyAnnotated: Boolean): List<QuickSeqLogLine> {
        var lastRelevantIndex = lastIndex
        if (onlyAnnotated) {
            var annotatedLinesCount = 0
            for (i in 0..lastIndex) {
                if (this[i].isAnnotated) {
                    annotatedLinesCount++
                    if (annotatedLinesCount >= maxRowsPerDiagram) {
                        lastRelevantIndex = i
                        break
                    }
                }
            }
        }
        return this.slice(0..Integer.min(lastRelevantIndex, maxRowsPerDiagram))
    }

    private fun List<QuickSeqLogLine>.rowLimitWarning(onlyAnnotated: Boolean): String {
        var relevantRowCount = if (onlyAnnotated) this.filter { it.isAnnotated }.size else this.size
        if (relevantRowCount <= maxRowsPerDiagram)
            return ""
        else
            return "\\n<font color=red size=13><b>WARNING: This diagram was truncated because it has too many rows: " +
                "$maxRowsPerDiagram are allowed, but it has $relevantRowCount. You can change this setting via the " +
                "\"maxRowsPerDiagram\" parameter for the QuickSeqDiagramTask (in your build.gradle.kts)."
    }
}

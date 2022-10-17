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

/**
 * The constructor takes a line ([String]) from the QuickSeq log and converts it into a Kotlin object
 * with attributes such as [isAnnotated], [isCall] or [functionName].
 *
 * It can also convert the log line into PlantUML syntax ([toPlantUmlLine]).
 */
class QuickSeqLogLine(line: String) {

    // The variables will be properly initialized in the init block
    var isAnnotated = true
    var isDiagramStart = false
    var isDiagramEnd = false
    var isCall = false
    var isReturn = false
    var isSilentReturn = false

    /* ktlint-disable no-multi-spaces */
    var functionName: String? = null            // only if isCall or isReturn, with braces if isReturn ("()" or "(...)")
    var parameterNames: List<String>? = null    // only if isCall
    var returnType: String? = null              // only if isReturn
    var leftClass: String? = null               // only if isCall or isReturn
    var rightClass: String? = null              // only if isCall or isReturn
    var diagramTitle: String? = null            // only if isDiagramStart
    /* ktlint-enable no-multi-spaces */

    val isCallOrReturn
        get() = isCall || isReturn
    val activationLine
        get() =
            if (isCall) "activate $rightClass\n"
            else if (isReturn) "deactivate $rightClass\n"
            else null
    /** The concept is the same as in [QuickSeqTransformer.getLogContextId] */
    val functionId
        get() = "$rightClass@${functionName?.split("(")?.first()}"

    init {
        val splittedLine = line.split(markerSeparator)
        for (token in splittedLine.map { markerSeparator + it }) {
            if (token.contains(notAnnotatedMarker)) isAnnotated = false
            else if (token.contains(leftClassMarker)) leftClass = token.withoutMarkers()
            else if (token.contains(rightClassMarker)) rightClass = token.withoutMarkers()
            else if (token.contains(silentReturnMarker)) isSilentReturn = true
            else if (token.contains(functionNameMarker)) functionName = token.withoutMarkers()
            else if (token.contains(diagramEndMarker)) isDiagramEnd = true
            else if (token.contains(diagramStartMarker)) {
                isDiagramStart = true
                diagramTitle = token.withoutMarkers()
            } else if (token.contains(callMarker)) {
                isCall = true
                parameterNames = token.withoutMarkers().split(",").map { it.trim() }
            } else if (token.contains(returnMarker)) {
                isReturn = true
                returnType = token.withoutMarkers().returnTypeWithoutPackage()
            }
        }
    }

    fun toPlantUmlLine(): String? {
        if (isCall)
            return "${leftClass!!} -> ${rightClass!!}: ${functionName!!}(" +
                "<font size=10> ${parameterNames!!.joinToString(", ")} <font size=13>)\n"
        if (isReturn && !isSilentReturn)
            return "${leftClass!!} <-- ${rightClass!!}: <font color=AAAAAA><b>${returnType!!}</b> " +
                "<font size=10>by ${functionName!!}\n"
        return null
    }

    fun String.withoutMarkers() = substringAfterLast("]").trim()

    fun String.returnTypeWithoutPackage(): String {
        var returnTypeWithoutPackage = ""
        if (!this.contains("<")) returnTypeWithoutPackage = this.split(".").last()
        else {
            var lastDotIndex = 0
            for (i in 0 until this.length) {
                if (this[i] == '.') lastDotIndex = i
                else if (this[i] == '<') {
                    returnTypeWithoutPackage = this.toCharArray().slice(lastDotIndex + 1 until length).joinToString("")
                    break
                }
            }
        }
        return returnTypeWithoutPackage
    }

    companion object {
        val markerSeparator = " $ "
        val callMarker = markerSeparator + "[CALL] "
        val returnMarker = markerSeparator + "[RETURN] "
        val leftClassMarker = markerSeparator + "[LEFT_CLASS] "
        val rightClassMarker = markerSeparator + "[RIGHT_CLASS] "
        val functionNameMarker = markerSeparator + "[FUNCTION] "
        val notAnnotatedMarker = markerSeparator + "[NOT_ANNOTATED] "
        val diagramStartMarker = markerSeparator + "[DIAGRAM_START] "
        val diagramEndMarker = markerSeparator + "[DIAGRAM_END] "
        val silentReturnMarker = markerSeparator + "[SILENT] "
    }
}

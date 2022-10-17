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

import com.kiwi.quickseq.QuickSeqLogLine.Companion.callMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.diagramStartMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.functionNameMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.leftClassMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.notAnnotatedMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.returnMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.rightClassMarker
import org.gradle.internal.impldep.org.testng.AssertJUnit.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LogLineConversionTest {

    @BeforeEach
    fun setup() = clearGeneratedFiles()

    @Test
    fun `should convert diagram start to LogLine correctly`() {
        val diagramStartLine = QuickSeqLogLine("$diagramStartMarker My Title")
        assert(diagramStartLine.isAnnotated == true)
        assert(diagramStartLine.isCall == false)
        assert(diagramStartLine.isReturn == false)
        assert(diagramStartLine.isSilentReturn == false)
        assert(diagramStartLine.isDiagramStart == true)
        assert(diagramStartLine.isDiagramEnd == false)
        assert(diagramStartLine.parameterNames == null)
        assert(diagramStartLine.returnType == null)
        assert(diagramStartLine.diagramTitle == "My Title")
        assert(diagramStartLine.functionName == null)
        assert(diagramStartLine.leftClass == null)
        assert(diagramStartLine.rightClass == null)
        assert(diagramStartLine.activationLine == null)
        assert(diagramStartLine.toPlantUmlLine() == null)
    }

    @Test
    fun `should convert call to LogLine correctly`() {
        val callLine = QuickSeqLogLine("$notAnnotatedMarker $callMarker param1, param2 $leftClassMarker LeftClass $rightClassMarker RightClass $functionNameMarker foo")
        assert(callLine.isAnnotated == false)
        assert(callLine.isCall == true)
        assert(callLine.isReturn == false)
        assert(callLine.isSilentReturn == false)
        assert(callLine.isDiagramStart == false)
        assert(callLine.isDiagramEnd == false)
        assert(callLine.parameterNames == listOf("param1", "param2"))
        assert(callLine.returnType == null)
        assert(callLine.diagramTitle == null)
        assert(callLine.functionName == "foo")
        assert(callLine.leftClass == "LeftClass")
        assert(callLine.rightClass == "RightClass")
        assert(callLine.activationLine == "activate RightClass\n")
        assertEquals(
            "LeftClass -> RightClass: foo(<font size=10> param1, param2 <font size=13>)\n",
            callLine.toPlantUmlLine()
        )
    }

    @Test
    fun `should convert return to LogLine correctly`() {
        val returnLine = QuickSeqLogLine("$returnMarker kotlin.Int $leftClassMarker LeftClass $rightClassMarker RightClass $functionNameMarker foo()")
        assert(returnLine.isAnnotated == true)
        assert(returnLine.isCall == false)
        assert(returnLine.isReturn == true)
        assert(returnLine.isSilentReturn == false)
        assert(returnLine.isDiagramStart == false)
        assert(returnLine.isDiagramEnd == false)
        assert(returnLine.parameterNames == null)
        assert(returnLine.returnType == "Int")
        assert(returnLine.diagramTitle == null)
        assert(returnLine.functionName == "foo()")
        assert(returnLine.leftClass == "LeftClass")
        assert(returnLine.rightClass == "RightClass")
        assert(returnLine.activationLine == "deactivate RightClass\n")
        assertEquals(
            "LeftClass <-- RightClass: <font color=AAAAAA><b>Int</b> <font size=10>by foo()\n",
            returnLine.toPlantUmlLine()
        )
    }

    @Test
    fun `should trim return type correctly`() {
        var returnLine = QuickSeqLogLine("$returnMarker kotlin.Int")
        assertEquals("Int", returnLine.returnType)

        returnLine = QuickSeqLogLine("$returnMarker kotlin.collections.Map<kotlin.String, kotlin.Int>")
        assertEquals("Map<kotlin.String, kotlin.Int>", returnLine.returnType)

        returnLine = QuickSeqLogLine("$returnMarker kotlin.collections.Map<kotlin.String, kotlin.collections.Map<kotlin.String, kotlin.Int>>")
        assertEquals("Map<kotlin.String, kotlin.collections.Map<kotlin.String, kotlin.Int>>", returnLine.returnType)
    }
}

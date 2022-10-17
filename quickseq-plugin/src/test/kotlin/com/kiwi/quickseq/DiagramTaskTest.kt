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
import com.kiwi.quickseq.QuickSeqLogLine.Companion.diagramEndMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.diagramStartMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.functionNameMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.leftClassMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.notAnnotatedMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.returnMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.rightClassMarker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiagramTaskTest {

    @BeforeEach
    fun setup() = clearGeneratedFiles()

    fun writeLog(lines: String) = QuickSeqLogger.saveToLogFile(lines)

    @Test
    fun `should convert log to PlantUML syntax correctly`() {
        println("in should convert log to PlantUML syntax correctly")

        writeLog(
            """
            $diagramStartMarker Bar
            $callMarker $leftClassMarker Test_Runner $rightClassMarker DefaultFile.kt $functionNameMarker bar
            $callMarker  $leftClassMarker DefaultFile.kt $rightClassMarker DefaultFile.kt $functionNameMarker foo
            $returnMarker kotlin.Unit $leftClassMarker DefaultFile.kt $rightClassMarker DefaultFile.kt $functionNameMarker foo()
            $diagramEndMarker $returnMarker kotlin.Unit $leftClassMarker Test_Runner $rightClassMarker DefaultFile.kt $functionNameMarker bar()
            """
        )
        assertDiagramEquals(
            "${QuickSeqDiagramTask("").fullDiagramDirName}/ALL_Bar.puml",
            """
            @startuml
            title Bar\n
            Test_Runner -> DefaultFile.kt: bar(<font size=10>  <font size=13>)
            activate DefaultFile.kt
            DefaultFile.kt -> DefaultFile.kt: foo(<font size=10>  <font size=13>)
            activate DefaultFile.kt
            DefaultFile.kt <-- DefaultFile.kt: <font color=AAAAAA><b>Unit</b> <font size=10>by foo()
            deactivate DefaultFile.kt
            Test_Runner <-- DefaultFile.kt: <font color=AAAAAA><b>Unit</b> <font size=10>by bar()
            deactivate DefaultFile.kt
            @enduml
            """
        )
    }

    @Test
    fun `should convert log to PlantUML syntax correctly if exception occurs`() {
        writeLog(
            """
            $diagramStartMarker Throwing
            $callMarker $leftClassMarker Test_Runner $rightClassMarker DefaultFile.kt $functionNameMarker throwing
            $diagramEndMarker
            """
        )
        assertDiagramEquals(
            "${QuickSeqDiagramTask("").fullDiagramDirName}/ALL_Throwing.puml",
            """
            @startuml
            title Throwing\n
            Test_Runner -> DefaultFile.kt: throwing(<font size=10>  <font size=13>)
            activate DefaultFile.kt
            @enduml
            """
        )
    }

    @Test
    fun `should add indirect marker`() {
        writeLog(
            """
            $diagramStartMarker Bar
            $callMarker $leftClassMarker Test_Runner $rightClassMarker File1.kt $functionNameMarker bar
            $notAnnotatedMarker $callMarker  $leftClassMarker File1.kt $rightClassMarker File2.kt $functionNameMarker foo
            $callMarker  $leftClassMarker File2.kt $rightClassMarker File3.kt $functionNameMarker foobar
            $returnMarker kotlin.Unit $leftClassMarker File2.kt $rightClassMarker File3.kt $functionNameMarker foobar()
            $notAnnotatedMarker $returnMarker kotlin.Unit $leftClassMarker File1.kt $rightClassMarker File2.kt $functionNameMarker foo()
            $diagramEndMarker $returnMarker kotlin.Unit $leftClassMarker Test_Runner $rightClassMarker File1.kt $functionNameMarker bar()
            """
        )
        assertDiagramEquals(
            "${QuickSeqDiagramTask("").filteredDiagramDirName}/Bar.puml",
            """
            @startuml
            title Bar\n
            Test_Runner -> File1.kt: bar(<font size=10>  <font size=13>)
            activate File1.kt
            File1.kt -> File2.kt: <b><font color=2060DD>[indirect]</b>
            activate File2.kt
            File2.kt -> File3.kt: foobar(<font size=10>  <font size=13>)
            activate File3.kt
            File2.kt <-- File3.kt: <font color=AAAAAA><b>Unit</b> <font size=10>by foobar()
            deactivate File3.kt
            File1.kt <-- File3.kt: <b><font color=2060DD>[indirect]</b>
            deactivate File3.kt
            Test_Runner <-- File1.kt: <font color=AAAAAA><b>Unit</b> <font size=10>by bar()
            deactivate File1.kt
            @enduml
            """
        )
    }
}

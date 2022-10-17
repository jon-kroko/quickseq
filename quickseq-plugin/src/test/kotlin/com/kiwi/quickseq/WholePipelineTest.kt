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

import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WholePipelineTest {

    @BeforeEach
    fun setup() = clearGeneratedFiles()

    @Test
    // This test can be useful for debugging because it (pretty surely) uses every method of the project
    fun `demo`() {
        val compilationResult = getCompiledFile(demoFile)
        invokeFunction(compilationResult, "DemoTest", "someTest")
        assertDiagramEquals(
            "${QuickSeqDiagramTask("").fullDiagramDirName}/ALL_Gazelles_are_cool.puml",
            """
            @startuml
            title Gazelles are cool\n
            Test_Runner -> DemoTest: someTest(<font size=10>  <font size=13>)
            activate DemoTest
            DemoTest -> Demo: someMethod(<font size=10>  <font size=13>)
            activate Demo
            DemoTest <-- Demo: <font color=AAAAAA><b>Int</b> <font size=10>by someMethod()
            deactivate Demo
            Test_Runner <-- DemoTest: <font color=AAAAAA><b>Unit</b> <font size=10>by someTest()
            deactivate DemoTest
            @enduml
            """
        )
    }
}

val demoFile = SourceFile.kotlin(
    "Demo.kt",
    """
    import com.kiwi.quickseq.QuickSeqLogger
    import com.kiwi.quickseq.SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    class DemoTest {
        @SequenceDiagramEntryPoint("Gazelles are cool")
        @Test
        fun someTest() {
            assert(Demo().someMethod() == 42)
        }
    }

    class Demo {
        @SequenceDiagram
        fun someMethod(): Int {
            return 42
        }
    }"""
)

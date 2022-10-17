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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InlineCallChainTest {

    @BeforeAll
    fun setup() = clearGeneratedFiles()

    @Test
    fun `should process inline call chains which call another class correctly`() {
        val compilationResult = getCompiledFile(inlineCallChain)
        invokeFunction(compilationResult, "InlineCallChain", "entryPoint")
        assertFunctionGotTransformed(
            compilationResult.javaCode("InlineCallChain"),
            "public final void entryPoint"
        )
        assertDiagramEquals(
            "${QuickSeqDiagramTask("").fullDiagramDirName}/ALL_Inline_Call_Chain.puml",
            """
            @startuml
            title Inline Call Chain\n
            Test_Runner -> InlineCallChain: entryPoint(<font size=10>  <font size=13>)
            activate InlineCallChain
            InlineCallChain -> InlineCallChain: getNumber(<font size=10>  <font size=13>)
            activate InlineCallChain
            InlineCallChain <-- InlineCallChain: <font color=AAAAAA><b>Number</b> <font size=10>by getNumber()
            deactivate InlineCallChain
            InlineCallChain -> Number: add1(<font size=10>  <font size=13>)
            activate Number
            InlineCallChain <-- Number: <font color=AAAAAA><b>Number</b> <font size=10>by add1()
            deactivate Number
            InlineCallChain -> Number: minus2(<font size=10>  <font size=13>)
            activate Number
            InlineCallChain <-- Number: <font color=AAAAAA><b>Number</b> <font size=10>by minus2()
            deactivate Number
            Test_Runner <-- InlineCallChain: <font color=AAAAAA><b>Unit</b> <font size=10>by entryPoint()
            deactivate InlineCallChain
            @enduml
            """
        )
    }

    @Test
    fun `should process inline call chains inside own class correctly`() {
        val compilationResult = getCompiledFile(inlineCallChainInOwnClass)
        invokeFunction(compilationResult, "InlineCallChainInOwnClass", "entryPoint")
        assertFunctionGotTransformed(
            compilationResult.javaCode("InlineCallChainInOwnClass"),
            "public final void entryPoint"
        )
        assertDiagramEquals(
            "${QuickSeqDiagramTask("").fullDiagramDirName}/ALL_Inline_Call_Chain_inside_own_class.puml",
            """
            @startuml
            title Inline Call Chain inside own class\n
            Test_Runner -> InlineCallChainInOwnClass: entryPoint(<font size=10>  <font size=13>)
            activate InlineCallChainInOwnClass
            InlineCallChainInOwnClass -> InlineCallChainInOwnClass: getNumber(<font size=10>  <font size=13>)
            activate InlineCallChainInOwnClass
            InlineCallChainInOwnClass <-- InlineCallChainInOwnClass: <font color=AAAAAA><b>Int</b> <font size=10>by getNumber()
            deactivate InlineCallChainInOwnClass
            InlineCallChainInOwnClass -> InlineCallChainInOwnClass: add1(<font size=10>  <font size=13>)
            activate InlineCallChainInOwnClass
            InlineCallChainInOwnClass <-- InlineCallChainInOwnClass: <font color=AAAAAA><b>Int</b> <font size=10>by add1()
            deactivate InlineCallChainInOwnClass
            InlineCallChainInOwnClass -> InlineCallChainInOwnClass: minus2(<font size=10>  <font size=13>)
            activate InlineCallChainInOwnClass
            InlineCallChainInOwnClass <-- InlineCallChainInOwnClass: <font color=AAAAAA><b>Int</b> <font size=10>by minus2()
            deactivate InlineCallChainInOwnClass
            Test_Runner <-- InlineCallChainInOwnClass: <font color=AAAAAA><b>Unit</b> <font size=10>by entryPoint()
            deactivate InlineCallChainInOwnClass
            @enduml
            """
        )
    }
}

val inlineCallChain = SourceFile.kotlin(
    "InlineCallChain.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    class InlineCallChain {
        @SequenceDiagramEntryPoint("Inline Call Chain")
        @Test
        fun entryPoint() {
            getNumber().add1().minus2()
        }

        @SequenceDiagram
        fun getNumber(): Number {
            return Number(42)
        }
    }

    class Number(var number: Int) {

        @SequenceDiagram
        fun add1(): Number {
            number += 1
            return this
        }

        @SequenceDiagram
        fun minus2(): Number {
            number -= 2
            return this
        }
    }"""
)

val inlineCallChainInOwnClass = SourceFile.kotlin(
    "InlineCallChainInOwnClass.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    class InlineCallChainInOwnClass {
        @SequenceDiagramEntryPoint("Inline Call Chain inside own class")
        @Test
        fun entryPoint() {
            getNumber().add1().minus2()
        }

        @SequenceDiagram
        fun getNumber(): Int {
            return 42
        }

        @SequenceDiagram
        fun Int.add1(): Int {
            return this + 1
        }

        @SequenceDiagram
        fun Int.minus2(): Int {
            return this - 2
        }
    }"""
)

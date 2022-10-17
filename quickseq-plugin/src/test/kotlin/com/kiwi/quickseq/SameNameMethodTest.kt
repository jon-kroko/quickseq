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
class SameNameMethodTest {

    @BeforeAll
    fun setup() = clearGeneratedFiles()

    @Test
    fun `should differentiate between methods with same name`() {
        val compilationResult = getCompiledFile(sameName)
        invokeFunction(compilationResult, "SameNameKt", "entryPoint")
        assertDiagramEquals(
            "${QuickSeqDiagramTask("").fullDiagramDirName}/ALL_Same_Name.puml",
            """
                @startuml
                title Same Name\n
                Test_Runner -> SameName.kt: entryPoint(<font size=10>  <font size=13>)
                activate SameName.kt
                SameName.kt -> SameName.kt: foo(<font size=10> aString <font size=13>)
                activate SameName.kt
                SameName.kt -> SameName.kt: foo(<font size=10> anInt <font size=13>)
                activate SameName.kt
                SameName.kt -> SameName.kt: foo(<font size=10> aList <font size=13>)
                activate SameName.kt
                SameName.kt <-- SameName.kt: <font color=AAAAAA><b>List<kotlin.Int></b> <font size=10>by foo(...)
                deactivate SameName.kt
                SameName.kt <-- SameName.kt: <font color=AAAAAA><b>Int</b> <font size=10>by foo(...)
                deactivate SameName.kt
                SameName.kt <-- SameName.kt: <font color=AAAAAA><b>String</b> <font size=10>by foo(...)
                deactivate SameName.kt
                Test_Runner <-- SameName.kt: <font color=AAAAAA><b>Unit</b> <font size=10>by entryPoint()
                deactivate SameName.kt
                @enduml
            """
        )
    }
}

val sameName = SourceFile.kotlin(
    "SameName.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    @SequenceDiagramEntryPoint("Same Name")
    @Test
    fun entryPoint() {
        foo("bar")
    }

    @SequenceDiagram
    fun foo(aString: String): String {
        foo(42)
        return aString
    }

    @SequenceDiagram
    fun foo(anInt: Int): Int {
        foo(listOf(anInt))
        return anInt
    }

    @SequenceDiagram
    fun foo(aList: List<Int>): List<Int> {
        return aList
    }
"""
)

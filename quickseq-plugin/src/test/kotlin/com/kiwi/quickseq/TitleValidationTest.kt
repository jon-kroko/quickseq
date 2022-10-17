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

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class TitleValidationTest {
    @Test
    fun `should recognize invalid characters`() {
        val result = compileWithQuickSeq(sourceFile = invalidTitleCharacters)
        Assertions.assertEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, result.exitCode)
        assert(result.messages.contains("contains illegal characters") == true)
    }

    @Test
    fun `should recognize duplicate titles`() {
        val result = compileWithQuickSeq(sourceFile = duplicateTitle)
        Assertions.assertEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, result.exitCode)
        assert(result.messages.contains("the diagrams would override each other") == true)
    }
}

// FILES TO COMPILE ====================================================================================================

val entryPointNotTest = SourceFile.kotlin(
    "EntryPointNotTest.kt",
    """
    import com.kiwi.quickseq.SequenceDiagramEntryPoint

    @SequenceDiagramEntryPoint
    fun notATest() {
        println("This annotation is not allowed here")
    }"""
)

val invalidTitleCharacters = SourceFile.kotlin(
    "EntryPointNotTest.kt",
    """
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    @Test
    @SequenceDiagramEntryPoint("Title*%")
    fun someTest() { }"""
)

val duplicateTitle = SourceFile.kotlin(
    "EntryPointNotTest.kt",
    """
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    @Test
    @SequenceDiagramEntryPoint("My Title")
    fun someTest() { }

    @Test
    @SequenceDiagramEntryPoint("My Title")
    fun otherTest() { }
"""
)

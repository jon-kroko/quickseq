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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AnnotationTargetTest {

    @Nested
    inner class EntryPointAnnotation {
        @Test
        fun `should throw exception when annotated function is not a test`() {
            val result = compileWithQuickSeq(wronglyAnnotatedNonTest)
            Assertions.assertEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, result.exitCode)
            assert(result.messages.contains("only tests can be annotated like this") == true)
        }

        @Test
        fun `should transform annotated function`() {
            val javaCode = getCompiledFile(annotatedFunctions).javaCode("ClassWithAnnotatedFunctions")
            assertFunctionGotTransformed(javaCode, "public final void someFunction")
        }

        @Test
        fun `should transform annotated class`() {
            val javaCode = getCompiledFile(annotatedClasses).javaCode("AnnotatedEntryPointClass")
            assertFunctionGotTransformed(javaCode, "public final void someFunction")
        }

        @Test
        fun `should transform annotated file`() {
            val javaCode = getCompiledFile(annotatedEntryPointFile).javaCode("ClassInAnnotatedFile")
            assertFunctionGotTransformed(javaCode, "public final int someFunction")
        }

        @Test
        fun `should not transform annotated field`() {
            val result = compileWithQuickSeq(fieldWithEntryPointAnnotation)
            Assertions.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assert(result.messages.contains("This annotation is not applicable to target") == true)
        }
    }

    @Nested
    inner class NormalAnnotation {
        @Test
        fun `should throw exception when annotated function is a test`() {
            val result = compileWithQuickSeq(wronglyAnnotatedTest)
            Assertions.assertEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, result.exitCode)
            assert(result.messages.contains("tests should not be annotated like this") == true)
        }

        @Test
        fun `should transform annotated function`() {
            val javaCode = getCompiledFile(annotatedFunctions).javaCode("ClassWithAnnotatedFunctions")
            assertFunctionGotTransformed(javaCode, "public final void anotherFunction")
        }

        @Test
        fun `should transform annotated class`() {
            val javaCode = getCompiledFile(annotatedClasses).javaCode("AnnotatedNormalClass")
            assertFunctionGotTransformed(javaCode, "public final void someFunction")
        }

        @Test
        fun `should transform annotated file`() {
            val javaCode = getCompiledFile(annotatedNormalFile).javaCode("ClassInAnnotatedFile")
            assertFunctionGotTransformed(javaCode, "public final int someFunction")
        }

        @Test
        fun `should not transform annotated field`() {
            val result = compileWithQuickSeq(fieldWithNormalAnnotation)
            Assertions.assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
            assert(result.messages.contains("This annotation is not applicable to target") == true)
        }
    }

    @Nested
    inner class InheritedAnnotations {
        @Test
        fun `should compile when inherited entry point annotation is forbidden`() {
            val result = compileWithQuickSeq(nonTestWithInheritedEntryPointAnnotation)
            Assertions.assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
            assertFunctionGotTransformed(result.javaCode("NormalClass"), "public final void nonTest")
        }

        @Test
        fun `should compile when inherited normal annotation is forbidden`() {
            val javaCode =
                getCompiledFile(testWithInheritedNormalAnnotation)
                    .javaCode("TestClass")
            assertDidNotGetTransformed(javaCode, "public final void someTest")
        }
    }
}

// FILES TO COMPILE ====================================================================================================

val annotatedEntryPointFile = SourceFile.kotlin(
    "AnnotatedFile.kt",
    """
    @file:SequenceDiagramEntryPoint
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    class ClassInAnnotatedFile {
        @Test
        fun someFunction(): Int {
            return 42
        }
    }"""
)

val annotatedNormalFile = SourceFile.kotlin(
    "AnnotatedFile.kt",
    """
    @file:SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagram

    class ClassInAnnotatedFile {
        fun someFunction(): Int {
            return 42
        }
    }"""
)

val annotatedClasses = SourceFile.kotlin(
    "AnnotatedClasses.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    @SequenceDiagramEntryPoint
    class AnnotatedEntryPointClass {
        @Test
        fun someFunction() {
            println("Raaaawr")
        }
    }

    @SequenceDiagram
    class AnnotatedNormalClass {
        fun someFunction() {
            println("Raaaawr")
        }
    }
"""
)

val annotatedFunctions = SourceFile.kotlin(
    "AnnotatedFunctions.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    class ClassWithAnnotatedFunctions {
        @SequenceDiagramEntryPoint
        @Test
        fun someFunction() {
            println("Quack")
        }

        @SequenceDiagram
        fun anotherFunction() {
            println("Woof")
        }
    }"""
)

val fieldWithEntryPointAnnotation = SourceFile.kotlin(
    "AnnotatedField.kt",
    """
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    class ClassWithEntryPointAnnotatedField {
        @SequenceDiagramEntryPoint
        val someField = 42
    }"""
)

val fieldWithNormalAnnotation = SourceFile.kotlin(
    "AnnotatedField.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram

    class ClassWithNormalAnnotatedField {
        @SequenceDiagram
        val otherField = 1337
    }"""
)

val wronglyAnnotatedNonTest = SourceFile.kotlin(
    "WronglyAnnotatedNonTest.kt",
    """
    import com.kiwi.quickseq.SequenceDiagramEntryPoint

    @SequenceDiagramEntryPoint
    fun nonTest() {}
    """
)

val wronglyAnnotatedTest = SourceFile.kotlin(
    "WronglyAnnotatedTest.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram
    import org.junit.jupiter.api.Test

    @Test
    @SequenceDiagram
    fun nonTest() {}
    """
)

val nonTestWithInheritedEntryPointAnnotation = SourceFile.kotlin(
    "NonTestWithInheritedEntryPointAnnotation.kt",
    """
    @file:SequenceDiagramEntryPoint
    import com.kiwi.quickseq.SequenceDiagramEntryPoint

    @SequenceDiagramEntryPoint
    class NormalClass {
        fun nonTest() {}
    }
    """
)

val testWithInheritedNormalAnnotation = SourceFile.kotlin(
    "TestWithInheritedNormalAnnotation.kt",
    """
    @file:SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagram
    import org.junit.jupiter.api.Test

    @SequenceDiagram
    class TestClass {
        @Test
        fun someTest() {}
    }
    """
)

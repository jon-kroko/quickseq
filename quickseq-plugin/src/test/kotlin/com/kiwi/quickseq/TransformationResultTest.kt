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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TransformationResultTest {

    @Test
    fun `should transform usual method correctly`() {
        val compilationResult = getCompiledFile(defaultFile)
        assertFunction(
            compilationResult.javaCode("DefaultFileKt"), "public static final void bar",
            """
            public static final void bar() {
                QuickSeqLogger.INSTANCE.clearLogContext();
                QuickSeqLogger.INSTANCE.registerLogContext(QuickSeqLogger.INSTANCE.ownLogContext(), QuickSeqLogger.INSTANCE.callerLogContext(true));
                QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                QuickSeqLogger.INSTANCE.saveToLogFile("\n ${'$'} [DIAGRAM_START] Bar\n ${'$'} [CALL]  ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(true) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] bar");
                try {
                    System.out.println((Object)"In bar");
                    QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                    foo();
                    QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                    QuickSeqLogger.INSTANCE.saveToLogFile(" ${'$'} [DIAGRAM_END]  ${'$'} [RETURN] kotlin.Unit ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(true) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] bar()");
                }
                catch (Throwable t) {
                    QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                    QuickSeqLogger.INSTANCE.saveToLogFile(" ${'$'} [DIAGRAM_END]  ${'$'} [RETURN] " + t + " ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(true) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] bar()");
                    throw t;
                }
            }
            """.trimIndent()
        )
    }

    @Test
    fun `should transform throwing method correctly`() {
        val javaCode = getCompiledFile(defaultFile).javaCode("DefaultFileKt")
        assertFunction(
            javaCode, "public static final int throwing",
            """
                public static final int throwing() {
                    QuickSeqLogger.INSTANCE.clearLogContext();
                    QuickSeqLogger.INSTANCE.registerLogContext(QuickSeqLogger.INSTANCE.ownLogContext(), QuickSeqLogger.INSTANCE.callerLogContext(true));
                    QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                    QuickSeqLogger.INSTANCE.saveToLogFile("\n ${'$'} [DIAGRAM_START] Throwing\n ${'$'} [CALL]  ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(true) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] throwing");
                    try {
                        throw new Exception("This always gets thrown");
                    }
                    catch (Throwable t) {
                        QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                        QuickSeqLogger.INSTANCE.saveToLogFile(" ${'$'} [DIAGRAM_END]  ${'$'} [RETURN] " + t + " ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(true) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] throwing()");
                        throw t;
                    }
                }
            """.trimIndent()
        )
    }

    @Nested
    inner class InheritanceTest {
        // TODO
    }

    @Nested
    inner class EarlyReturnTest {

        @Test
        fun `should recognize early return`() {
            assertFunction(
                getCompiledFile(earlyReturnFile).javaCode("EarlyReturnKt"),
                "public static final String earlyReturn",
                """
                    public static final String earlyReturn(@NotNull final String input) {
                        Intrinsics.checkNotNullParameter(input, "input");
                        QuickSeqLogger.INSTANCE.registerLogContext(QuickSeqLogger.INSTANCE.ownLogContext(), QuickSeqLogger.INSTANCE.callerLogContext(false));
                        QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                        QuickSeqLogger.INSTANCE.saveToLogFile(" ${'$'} [CALL] input ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyReturn");
                        try {
                            if (Intrinsics.areEqual(input, "EARLY_RETURN_1")) {
                                QuickSeqLogger.INSTANCE.saveDeferredLog(QuickSeqLogger.INSTANCE.callerLogContext(false), " ${'$'} [RETURN] kotlin.String ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyReturn(...)");
                                return "Early return 1";
                            }
                            QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                            if (Intrinsics.areEqual(input, "EARLY_RETURN_2")) {
                                QuickSeqLogger.INSTANCE.saveDeferredLog(QuickSeqLogger.INSTANCE.callerLogContext(false), " ${'$'} [RETURN] kotlin.String ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyReturn(...)");
                                return "Early return 2";
                            }
                            QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                            QuickSeqLogger.INSTANCE.saveDeferredLog(QuickSeqLogger.INSTANCE.callerLogContext(false), " ${'$'} [RETURN] kotlin.String ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyReturn(...)");
                            return "Normal return";
                        }
                        catch (Throwable t) {
                            QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                            QuickSeqLogger.INSTANCE.saveToLogFile(" ${'$'} [RETURN] " + t + " ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyReturn(...)");
                            throw t;
                        }
                    }
                """.trimIndent()
            )
        }

        @Test
        fun `should recognize early exit`() {
            assertFunction(
                getCompiledFile(earlyExitFile).javaCode("EarlyExitKt"),
                "public static final void earlyExit",
                """
                    public static final void earlyExit(@NotNull final String input) {
                        Intrinsics.checkNotNullParameter(input, "input");
                        QuickSeqLogger.INSTANCE.registerLogContext(QuickSeqLogger.INSTANCE.ownLogContext(), QuickSeqLogger.INSTANCE.callerLogContext(false));
                        QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                        QuickSeqLogger.INSTANCE.saveToLogFile(" ${'$'} [CALL] input ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyExit");
                        try {
                            if (Intrinsics.areEqual(input, "EARLY_RETURN_1")) {
                                QuickSeqLogger.INSTANCE.saveDeferredLog(QuickSeqLogger.INSTANCE.callerLogContext(false), " ${'$'} [RETURN] kotlin.Unit ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyExit(...)");
                                return;
                            }
                            QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                            if (Intrinsics.areEqual(input, "EARLY_RETURN_2")) {
                                QuickSeqLogger.INSTANCE.saveDeferredLog(QuickSeqLogger.INSTANCE.callerLogContext(false), " ${'$'} [RETURN] kotlin.Unit ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyExit(...)");
                                return;
                            }
                            QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                            QuickSeqLogger.INSTANCE.saveToLogFile(" ${'$'} [RETURN] kotlin.Unit ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyExit(...)");
                        }
                        catch (Throwable t) {
                            QuickSeqLogger.printDeferredLogIfNeeded${'$'}default(QuickSeqLogger.INSTANCE, QuickSeqLogger.INSTANCE.ownLogContext(), false, null, 6, null);
                            QuickSeqLogger.INSTANCE.saveToLogFile(" ${'$'} [RETURN] " + t + " ${'$'} [LEFT_CLASS] " + QuickSeqLogger.INSTANCE.callerClass(false) + " ${'$'} [RIGHT_CLASS] " + QuickSeqLogger.INSTANCE.ownClass() + " ${'$'} [FUNCTION] earlyExit(...)");
                            throw t;
                        }
                    }
                """.trimIndent()
            )
        }
    }
}

// FILES TO COMPILE ====================================================================================================

val defaultFile = SourceFile.kotlin(
    "DefaultFile.kt",
    """
    import com.kiwi.quickseq.QuickSeqLogger
    import com.kiwi.quickseq.SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagramEntryPoint
    import org.junit.jupiter.api.Test

    @SequenceDiagramEntryPoint
    @Test
    fun bar() {
        println("In bar")
        foo()
    }

    @SequenceDiagram
    fun foo() {
        println("In foo")
    }

    @SequenceDiagramEntryPoint
    @Test
    fun throwing(): Int {
        throw Exception("This always gets thrown")
    }"""
)

val earlyReturnFile = SourceFile.kotlin(
    "EarlyReturn.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram
    
    @SequenceDiagram
    fun earlyReturn(input: String): String {
        if (input == "EARLY_RETURN_1") return "Early return 1"
        if (input == "EARLY_RETURN_2") return "Early return 2"
        return "Normal return"
    }"""
)

val earlyExitFile = SourceFile.kotlin(
    "EarlyExit.kt",
    """
    import com.kiwi.quickseq.SequenceDiagram
    
    @SequenceDiagram
    fun earlyExit(input: String) {
        if (input == "EARLY_RETURN_1") return
        if (input == "EARLY_RETURN_2") return
    }"""
)

val inheritanceFile = SourceFile.kotlin(
    "InheritanceFile.kt",
    """
    @file:SequenceDiagram
    import com.kiwi.quickseq.SequenceDiagram
    
    fun functionOutsideClass() {}    
    
    interface MyInterface
    
    open class MySuperclass {
        fun mySuperclassFunction() {
            println("Inside mySuperclassFunction at class: " + this::class.java.simpleName)
        }
    }
    
    class MySubclass: MySuperclass()
    
    class MyInterfaceSubclass: MyInterface"""
)

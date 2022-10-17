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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class LogTest {

    @BeforeEach
    fun setup() = clearGeneratedFiles()

    @Test
    fun `should write correct log for normal call chain`() {
        invokeFunction(getCompiledFile(defaultFile), "DefaultFileKt", "bar")
        assertLogEquals(
            """
            [DIAGRAM_START] Bar
            [CALL]  ${'$'} [LEFT_CLASS] Test_Runner ${'$'} [RIGHT_CLASS] DefaultFile.kt ${'$'} [FUNCTION] bar
            [CALL]  ${'$'} [LEFT_CLASS] DefaultFile.kt ${'$'} [RIGHT_CLASS] DefaultFile.kt ${'$'} [FUNCTION] foo
            [RETURN] kotlin.Unit ${'$'} [LEFT_CLASS] DefaultFile.kt ${'$'} [RIGHT_CLASS] DefaultFile.kt ${'$'} [FUNCTION] foo()
            [DIAGRAM_END]  ${'$'} [RETURN] kotlin.Unit ${'$'} [LEFT_CLASS] Test_Runner ${'$'} [RIGHT_CLASS] DefaultFile.kt ${'$'} [FUNCTION] bar()
            """.trimIndent()
        )
    }

    @Test
    fun `should write correct log if exception occurs`() {
        try {
            invokeFunction(getCompiledFile(defaultFile), "DefaultFileKt", "throwing")
        } catch (e: Exception) {
            if (e.message?.contains("This always gets thrown") != true) throw e
        }
        assertLogEquals(
            """
            [DIAGRAM_START] Throwing
            [CALL]  ${'$'} [LEFT_CLASS] Test_Runner ${'$'} [RIGHT_CLASS] DefaultFile.kt ${'$'} [FUNCTION] throwing
            [DIAGRAM_END]  ${'$'} [RETURN] java.lang.Exception: This always gets thrown ${'$'} [LEFT_CLASS] Test_Runner ${'$'} [RIGHT_CLASS] DefaultFile.kt ${'$'} [FUNCTION] throwing()
            """.trimIndent()
        )
        QuickSeqDiagramTask(File("").absolutePath).generate()
    }
}

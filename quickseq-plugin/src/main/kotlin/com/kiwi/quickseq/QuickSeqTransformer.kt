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

import com.kiwi.quickseq.HasAnnotation.EXPLICIT
import com.kiwi.quickseq.HasAnnotation.INHERITED
import com.kiwi.quickseq.HasAnnotation.NONE
import com.kiwi.quickseq.QuickSeqLogLine.Companion.callMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.diagramEndMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.diagramStartMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.functionNameMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.leftClassMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.notAnnotatedMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.returnMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.rightClassMarker
import com.kiwi.quickseq.QuickSeqLogLine.Companion.silentReturnMarker
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irCatch
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.codegen.fileParent
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.backend.js.utils.getSingleConstStringArgument
import org.jetbrains.kotlin.ir.builders.* // ktlint-disable no-wildcard-imports
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.declarations.* // ktlint-disable no-wildcard-imports
import org.jetbrains.kotlin.ir.expressions.* // ktlint-disable no-wildcard-imports
import org.jetbrains.kotlin.ir.expressions.impl.IrStringConcatenationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTryImpl
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.util.* // ktlint-disable no-wildcard-imports
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/* ktlint-disable no-consecutive-blank-lines */

/**
 * Defines how a function must be transformed in order to appear in the QuickSeq sequence diagrams.
 * See e.g. [transformBody] or [printEntryLog] to understand it better.
 */
class QuickSeqTransformer(
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoidWithContext() {

    // Annotations
    private val diagramAnnoName = FqName("com.kiwi.quickseq.SequenceDiagram")
    private val entryPointAnnoName = FqName("com.kiwi.quickseq.SequenceDiagramEntryPoint")
    private val testAnnoName = FqName("org.junit.jupiter.api.Test") // TODO check if we need to support other test annotations

    // Logging
    private val logFunction = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.saveToLogFile")).first()
    private val registerLogContextFunction = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.registerLogContext")).first()
    private val saveDeferredLogFunction = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.saveDeferredLog")).first()
    private val printDeferredLogIfNeededFunction = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.printDeferredLogIfNeeded")).first()
    private val clearLogContextFunction = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.clearLogContext")).first()
    private val loggerObject = pluginContext.referenceClass(FqName("com.kiwi.quickseq.QuickSeqLogger"))!!

    // Meta-programming
    private val callerClassGetName = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.callerClass")).single()
    // private val callerFunctionGetName = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.callerFunction")).single()
    private val callerLogContext = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.callerLogContext")).single()
    private val ownClassGetName = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.ownClass")).single()
    // private val ownFunctionGetName = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.ownFunction")).single()
    private val ownLogContext = pluginContext.referenceFunctions(FqName("com.kiwi.quickseq.QuickSeqLogger.ownLogContext")).single()

    // Further utils
    private val typeUnit = pluginContext.irBuiltIns.unitType
    private val typeThrowable = pluginContext.irBuiltIns.throwableType

    // ================================================================================================

    /**
     * We hook into the IR compiler's routine to check if a function is relevant for the sequence diagrams.
     * If yes, its body will be replaced with [transformBody].
     *
     * @throws QuickSeqException if the user has used the annotations wrongly, or if anything went wrong while the body
     * got transformed
     */
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        try {
            if (declaration.shouldBeTransformed()) {
                if (declaration.hasEntryPointAnno && !declaration.hasTestAnno) throw forbiddenEntryPointException(declaration)
                if (declaration.hasDiagramAnno && declaration.hasTestAnno) throw forbiddenDiagramException(declaration)
                declaration.body?.let { declaration.body = transformBody(declaration, it) }
            } else if (declaration.hasTestAnno) {
                declaration.body?.let { // Remove all statements from unannotated tests in order to run the tests faster
                    declaration.body = DeclarationIrBuilder(pluginContext, declaration.symbol).irBlockBody { }
                }
            }
        } catch (e: Exception) {
            when (e) {
                is QuickSeqException -> throw e
                else -> throw generalException(declaration, e)
            }
        }
        return super.visitFunctionNew(declaration)
    }

    /**
     * Returns a function body, enriched with logging functionality that is needed to create the sequence diagrams.
     */
    private fun transformBody(
        function: IrFunction,
        body: IrBody
    ): IrBlockBody {
        return DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody {
            if (function.hasEntryPointAnno) {
                +irCall(clearLogContextFunction)
                    .apply { dispatchReceiver = irGetObject(loggerObject) }
            }

            +registerLogContext(function)
            +printDeferredLogIfNeeded(ownLogContext()) // Logs only lines with urgency TODO find better wording
            +printEntryLog(function)

            val tryBlock = irBlock(resultType = function.returnType) {
                for (statement in body.statements) {
                    +statement
                    +printDeferredLogIfNeeded(ownLogContext())
                }
                if (function.returnType == typeUnit) +printExitLog(function, typeAsIrString(function))
            }.transform(QuickSeqReturnTransformer(function), null)

            val throwable = buildVariable(
                scope.getLocalDeclarationParent(), startOffset, endOffset, IrDeclarationOrigin.CATCH_PARAMETER,
                Name.identifier("t"), typeThrowable
            )

            +IrTryImpl(startOffset, endOffset, tryBlock.type).also { irTry ->
                irTry.tryResult = tryBlock
                irTry.catches += irCatch(
                    throwable,
                    irBlock {
                        +printDeferredLogIfNeeded(ownLogContext())
                        +printExitLog(function, irGet(throwable))
                        +irThrow(irGet(throwable))
                    }
                )
            }
        }
    }

    /**
     * Returns a call to a logging function (see [printLog]).
     * Content (incomplete):
     * - "A function has just been entered"
     * - Which class has triggered the call?
     * - Which function has been triggered?
     * - Which parameters does the function have?
     * - Which class does the function belong to?
     */
    private fun IrBuilderWithScope.printEntryLog(
        function: IrFunction
    ): IrCall {
        var concat = irConcat()
        val title = getDiagramTitleIfValid(function)

        concat.apply {
            if (function.hasEntryPointAnno) {
                addArgument(irString("\n"))
                addArgument(irString("$diagramStartMarker$title\n"))
            } else if (!function.hasAnyQuickSeqAnno) addArgument(irString(notAnnotatedMarker))

            addArgument(irString(callMarker))
            addArgument(irString(function.valueParameters.map { it.name.toString() }.joinToString(",")))

            addArgument(irString(leftClassMarker))
            addArgument(callerClass(function))

            addArgument(irString(rightClassMarker))
            addArgument(ownClass())

            addArgument(irString(functionNameMarker))
            addArgument(irString(function.quotedName()))
        }

        return printLog(concat)
    }

    /**
     * Counterpart to [printEntryLog].
     * Returns a call to a logging function (see [printLog]).
     * Content (incomplete):
     * - "A function is about to return"
     * - What is the function's name and class?
     * - What is the function's return type?
     */
    private fun IrBuilderWithScope.printExitLog(
        function: IrFunction,
        returnType: IrExpression? = null,
        deferLogging: Boolean = false
    ): IrContainerExpression {
        val concat = irConcat()

        concat.apply {
            if (function.hasEntryPointAnno) addArgument(irString(diagramEndMarker))
            else if (!function.hasAnyQuickSeqAnno) addArgument(irString(notAnnotatedMarker))

            addArgument(irString(returnMarker))
            returnType?.let { addArgument(it) }

            addArgument(irString(leftClassMarker))
            addArgument(callerClass(function))

            addArgument(irString(rightClassMarker))
            addArgument(ownClass())

            addArgument(irString(functionNameMarker))
            addArgument(irString(function.quotedName() + if (function.valueParameters.isNotEmpty()) "(...)" else "()"))

            if (function.shouldReturnSilently) addArgument(irString(silentReturnMarker))
        }

        return irBlock {
            if (deferLogging) +saveDeferredLog(function, concat)
            else +printLog(concat)
        }
    }

    /**
     * Makes sure that [printExitLog] gets called, even if the function returns early or the log must be printed
     * after the return expression got evaluated.
     */
    inner class QuickSeqReturnTransformer(
        private val function: IrFunction
    ) : IrElementTransformerVoidWithContext() {
        override fun visitReturn(expression: IrReturn): IrExpression {
            if (expression.returnTargetSymbol != function.symbol) return super.visitReturn(expression)

            return DeclarationIrBuilder(pluginContext, function.symbol).irBlock {
                +printExitLog(function, typeAsIrString(expression), true)
                +expression
            }
        }
    }



    // GETTERS ===========================================================================================

    private fun IrBlockBodyBuilder.typeAsIrString(function: IrFunction) = irString(function.returnType.asString())
    private fun IrBlockBuilder.typeAsIrString(expression: IrReturn) = irString(expression.value.type.asString())

    private fun IrFunction.quotedName(): String {
        val name = this.name.toString()
        return if (name.contains(" ")) "`$name`" else name
    }

    private fun IrFunction.classOrFileName() =
        (parentClassOrNull?.name ?: fileParent.name).toString()

    private val diagramTitles = mutableMapOf<String, String>()
    private fun getDiagramTitleIfValid(function: IrFunction): String {
        var diagramTitle: String? = null
        try {
            diagramTitle = function.getAnnotation(entryPointAnnoName)?.getSingleConstStringArgument()
        } catch (e: NullPointerException) { }

        diagramTitle?.let {
            if (!it.matches(Regex("[A-Za-z0-9- ]*"))) throw invalidCharactersInTitleException(it, function)
            if (diagramTitles.containsKey(it)) throw duplicateTitleException(it, function)
            diagramTitles.put(it, function.name.toString())
        }
        return if (!diagramTitle.isNullOrBlank()) diagramTitle.capitalize() else function.name.toString().capitalize()
    }



    // EXCEPTIONS =======================================================================================

    private fun forbiddenEntryPointException(f: IrFunction) = QuickSeqException(
        "Function ${f.name} " +
            "(${f.classOrFileName()}) has been annotated with @SequenceDiagramEntryPoint, but only tests can be " +
            "annotated like this. You might want to change the annotation to @SequenceDiagram."
    )

    private fun forbiddenDiagramException(f: IrFunction) = QuickSeqException(
        "Function ${f.name} " +
            "(${f.classOrFileName()}) has been annotated with @SequenceDiagram, but tests should not be annotated like " +
            "this. You might want to change the annotation to @SequenceDiagramEntryPoint."
    )

    private fun generalException(f: IrFunction, e: Exception) =
        QuickSeqException("Failed to transform method \"${f.name}\" (from ${f.classOrFileName()}) with exception: $e")

    private fun invalidCharactersInTitleException(title: String, f: IrFunction) = QuickSeqException(
        "The diagram title \"$title\" (found in ${f.classOrFileName()}, ${f.quotedName()}) contains " +
            "illegal characters. Allowed characters: A-Z, a-z, 0-9, space, dash (-)\n"
    )

    private fun duplicateTitleException(title: String, f: IrFunction) =
        QuickSeqException(
            "You have used the diagram title \"$title\" at least twice (at ${f.quotedName()}) and " +
                "${diagramTitles.get(title)}. This should be avoided, because the diagrams would override each other."
        )



    // IR OBJECTS =======================================================================================

    /**
     * Returns a call to the logging function [QuickSeqLogger.saveToLogFile]
     */
    private fun IrBuilderWithScope.printLog(
        concat: IrStringConcatenationImpl
    ): IrCall {
        return irCall(logFunction).apply {
            putValueArgument(0, concat)
            dispatchReceiver = irGetObject(loggerObject)
        }
    }

    /**
     * Returns a call to [QuickSeqLogger.saveDeferredLog]
     */
    private fun IrBuilderWithScope.saveDeferredLog(
        function: IrFunction,
        line: IrStringConcatenationImpl
    ): IrCall {
        return irCall(saveDeferredLogFunction).apply {
            putValueArgument(0, callerLogContext(function))
            putValueArgument(1, line)
            dispatchReceiver = irGetObject(loggerObject)
        }
    }

    /**
     * Returns a call to [QuickSeqLogger.printDeferredLogIfNeeded]
     */
    private fun IrBuilderWithScope.printDeferredLogIfNeeded(thisContext: IrCall): IrCall {
        return irCall(printDeferredLogIfNeededFunction).apply {
            putValueArgument(0, thisContext)
            dispatchReceiver = irGetObject(loggerObject)
        }
    }

    /**
     * Returns a call to [QuickSeqLogger.registerLogContext]
     */
    private fun IrBuilderWithScope.registerLogContext(function: IrFunction) =
        irCall(registerLogContextFunction).apply {
            putValueArgument(0, ownLogContext())
            putValueArgument(1, callerLogContext(function))
            dispatchReceiver = irGetObject(loggerObject)
        }

    private fun IrBuilderWithScope.ownClass() =
        irCall(ownClassGetName).apply { dispatchReceiver = irGetObject(loggerObject) }

    private fun IrBuilderWithScope.ownLogContext() =
        irCall(ownLogContext).apply { dispatchReceiver = irGetObject(loggerObject) }

    private fun IrBuilderWithScope.callerClass(function: IrFunction) =
        irCall(callerClassGetName).apply {
            putValueArgument(0, function.hasEntryPointAnno.toIrConst(pluginContext.irBuiltIns.booleanType))
            dispatchReceiver = irGetObject(loggerObject)
        }

    private fun IrBuilderWithScope.callerLogContext(function: IrFunction) =
        irCall(callerLogContext).apply {
            putValueArgument(0, function.hasEntryPointAnno.toIrConst(pluginContext.irBuiltIns.booleanType))
            dispatchReceiver = irGetObject(loggerObject)
        }


    // FUNCTION CHARACTERISTICS =====================================================================

    private fun IrFunction.shouldBeTransformed(): Boolean {
        if (this.name.toString().contains("<")) return false
        listOf("toString", "equals", "hashCode", "valueOf", "values", "init").forEach {
            if (this.name.toString() == it) return false
        }
        if (this.hasAnyQuickSeqAnno) return true
        else if (this.hasTestAnno || this.isSuspend) return false
        return true
    }

    private fun IrFunction.hasExplicitOrInheritedAnnotation(fqName: FqName): HasAnnotation {
        if (this.hasAnnotation(fqName)) return EXPLICIT
        if (this.parentClassOrNull?.hasAnnotation(fqName) == true) return INHERITED
        if (this.fileParent.hasAnnotation(fqName)) return INHERITED
        return NONE
    }

    private fun IrFunction.getExplicitOrInheritedAnnotation(fqName: FqName): IrConstructorCall? {
        listOf(this, this.parentClassOrNull, this.fileParent).forEach {
            it?.getAnnotation(fqName)?.let { anno -> return anno }
        }
        return null
    }

    private val IrFunction.shouldReturnSilently
        get() = getExplicitOrInheritedAnnotation(diagramAnnoName)?.getValueArgument(0)?.isTrueConst() == true

    private val IrFunction.hasDiagramAnno: Boolean
        get() {
            val has = this.hasExplicitOrInheritedAnnotation(diagramAnnoName)
            if (has == NONE) return false
            if (has == EXPLICIT) return true
            return !hasTestAnno // has == INHERITED
        }

    private val IrFunction.hasEntryPointAnno: Boolean
        get() {
            val has = this.hasExplicitOrInheritedAnnotation(entryPointAnnoName)
            if (has == NONE) return false
            if (has == EXPLICIT) return true
            return hasTestAnno // has == INHERITED
        }

    private val IrFunction.hasAnyQuickSeqAnno
        get() = this.hasDiagramAnno || this.hasEntryPointAnno

    private val IrFunction.hasTestAnno
        get() = this.hasAnnotation(testAnnoName)
}

private enum class HasAnnotation {
    NONE,
    EXPLICIT,
    INHERITED
}

/* ktlint-enable no-consecutive-blank-lines */

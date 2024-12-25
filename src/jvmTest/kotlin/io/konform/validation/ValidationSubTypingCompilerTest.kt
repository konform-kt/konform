package io.konform.validation

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.Test

@OptIn(ExperimentalCompilerApi::class)
class ValidationSubTypingCompilerTest {
    @Test
    fun compilesValidSubtyping() {
        val kotlinSource =
            SourceFile.kotlin(
                "ValidSubtyping.kt",
                """
            import io.konform.validation.Validation
            class ValidSubtyping {
                val numberValidation = Validation<Number> {}
                val intValidation = Validation<Int> { run(numberValidation) }
                val numberAsIntValidation: Validation<Int> = numberValidation
            }
        """,
            )

        val result =
            KotlinCompilation()
                .apply {
                    sources = listOf(kotlinSource)
                    inheritClassPath = true
                    // messageOutputStream = System.out // see diagnostics in real time
                }.compile()

        result.exitCode shouldBe KotlinCompilation.ExitCode.OK
    }

    @Test
    fun compileErrorOnInvalidSubtyping() {
        val kotlinSource =
            SourceFile.kotlin(
                "InvalidAssign.kt",
                """
            import io.konform.validation.Validation
            class InvalidAssign {
                val intValidation = Validation<Int> {}
                val intAsNumberValidation: Validation<Number> = intValidation
            }
        """,
            )

        val result =
            KotlinCompilation()
                .apply {
                    sources = listOf(kotlinSource)
                    inheritClassPath = true
                    // messageOutputStream = System.out // see diagnostics in real time
                }.compile()

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Initializer type mismatch: expected 'io.konform.validation.Validation<kotlin.Number>', " +
            "actual 'io.konform.validation.Validation<kotlin.Int>'"
    }

    @Test
    fun compileErrorOnInvalidRun() {
        val kotlinSource =
            SourceFile.kotlin(
                "InvalidRun.kt",
                """
            import io.konform.validation.Validation
            class InvalidRun {
                val intValidation = Validation<Int> {}
                val intAsNumberValidation = Validation<Number> {
                    run(intValidation)
                }
            }
        """,
            )

        val result =
            KotlinCompilation()
                .apply {
                    sources = listOf(kotlinSource)
                    inheritClassPath = true
                    // messageOutputStream = System.out // see diagnostics in real time
                }.compile()

        result.exitCode shouldBe KotlinCompilation.ExitCode.COMPILATION_ERROR
        result.messages shouldContain "Argument type mismatch: actual type is 'io.konform.validation.Validation<kotlin.Int>', " +
            "but 'io.konform.validation.Validation<kotlin.Number>' was expected"
    }
}

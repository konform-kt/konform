package io.konform.validation.constraints

import io.konform.validation.Invalid
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.path.ValidationPath
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainOnlyError
import kotlin.test.Test

class ValidationBuilderConstraintTest {
    private val validation =
        Validation<String> {
            constrain("must contain a tab '{value}'") {
                it.contains("\t")
            }
        }

    private val validValue = "abc\t"
    private val invalidValue = "abc"
    private val invalid = validation.validate(invalidValue) as Invalid

    @Test
    fun shouldValidateConstraint() {
        validation shouldBeValid validValue
        validation shouldBeInvalid invalidValue
    }

    @Test
    fun shouldShowHint() {
        invalid shouldContainOnlyError ValidationError.ofEmptyPath("must contain a tab 'abc'")
    }

    @Test
    fun shouldSetHint() {
        val api1 =
            Validation<String> {
                constrain("foo") {
                    false
                }
            }

        val api2 =
            Validation<String> {
                constrain("") {
                    false
                } hint "foo"
            }

        val api3 =
            Validation<String> {
                constrain("") {
                    false
                }.replace(hint = "foo")
            }

        val expected = ValidationError.ofEmptyPath("foo")

        (api1 shouldBeInvalid "").shouldContainOnlyError(expected)
        (api2 shouldBeInvalid "").shouldContainOnlyError(expected)
        (api3 shouldBeInvalid "").shouldContainOnlyError(expected)
    }

    @Test
    fun shouldSetPath() {
        val api1 =
            Validation<String> {
                constrain("foo", path = ValidationPath.of("def")) {
                    false
                }
            }

        val api2 =
            Validation<String> {
                constrain("foo") {
                    false
                } path ValidationPath.of("def")
            }

        val api3 =
            Validation<String> {
                constrain("foo") {
                    false
                }.replace(path = ValidationPath.of("def"))
            }

        val expected = ValidationError.of("def", "foo")

        (api1 shouldBeInvalid "").shouldContainOnlyError(expected)
        (api2 shouldBeInvalid "").shouldContainOnlyError(expected)
        (api3 shouldBeInvalid "").shouldContainOnlyError(expected)
    }

    enum class Severity { ERROR, WARNING }

    @Test
    fun shouldSetContext() {
        val api1 =
            Validation<String> {
                constrain("foo", userContext = Severity.ERROR) {
                    false
                }
            }

        val api2 =
            Validation<String> {
                constrain("foo") {
                    false
                } userContext Severity.ERROR
            }

        val api3 =
            Validation<String> {
                constrain("foo") {
                    false
                }.replace(userContext = Severity.ERROR)
            }

        val expected = ValidationError.ofEmptyPath("foo").copy(userContext = Severity.ERROR)

        (api1 shouldBeInvalid "").shouldContainOnlyError(expected)
        (api2 shouldBeInvalid "").shouldContainOnlyError(expected)
        (api3 shouldBeInvalid "").shouldContainOnlyError(expected)
    }
}

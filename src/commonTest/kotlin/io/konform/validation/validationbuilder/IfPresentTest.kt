package io.konform.validation.validationbuilder

import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.constraints.pattern
import io.konform.validation.countErrors
import io.konform.validation.path.ValidationPath
import io.kotest.assertions.konform.shouldBeValid
import kotlin.test.Test
import kotlin.test.assertEquals

class IfPresentTest {
    @Test
    fun validatingNullableFields() {
        val nullableFieldValidation =
            Validation<Register> {
                Register::referredBy ifPresent {
                    pattern(".+@.+".toRegex()).hint("must have correct format")
                }
            }

        Register(referredBy = null).let { assertEquals(Valid(it), nullableFieldValidation(it)) }
        Register(referredBy = "poweruser@test.com").let { assertEquals(Valid(it), nullableFieldValidation(it)) }
        Register(referredBy = "poweruser@").let { assertEquals(1, countErrors(nullableFieldValidation(it), Register::referredBy)) }
    }

    // See https://github.com/konform-kt/konform/issues/166
    @Test
    fun allowedOnNonNullableFields() {
        val validation1 =
            Validation<String> {
                ifPresent(ValidationPath.EMPTY, { it }) {}
            }
        val validation2 =
            Validation<Register> {
                Register::name ifPresent {}
            }

        validation1 shouldBeValid ""
        validation2 shouldBeValid Register()
    }

    private data class Register(
        val name: String = "",
        val referredBy: String? = null,
    )
}

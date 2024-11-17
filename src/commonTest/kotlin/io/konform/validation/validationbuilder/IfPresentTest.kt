package io.konform.validation.validationbuilder

import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.constraints.pattern
import io.konform.validation.countErrors
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

    @Test
    fun deprecationWhenPropertyIsNotNull() {
        val validation =
            Validation<Foo> {
                // This itself will give a warning if no deprecation is suppressed
                @Suppress("DEPRECATION")
                Foo::bar ifPresent {}
            }

        validation shouldBeValid Foo("")
    }

    private data class Register(
        val referredBy: String? = null,
    )

    private data class Foo(
        val bar: String,
    )
}

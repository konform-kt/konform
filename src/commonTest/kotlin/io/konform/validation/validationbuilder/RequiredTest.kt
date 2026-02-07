package io.konform.validation.validationbuilder

import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.constraints.minLength
import io.konform.validation.constraints.pattern
import io.konform.validation.countErrors
import io.konform.validation.required
import io.konform.validation.types.RequireNotNullValidation.Companion.DEFAULT_REQUIRED_HINT
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainOnlyError
import kotlin.test.Test
import kotlin.test.assertEquals

class RequiredTest {
    @Test
    fun validatingRequiredFields() {
        val nullableFieldValidation =
            Validation<Register> {
                Register::referredBy required {
                    pattern(".+@.+".toRegex()).hint("must have correct format")
                }
            }

        Register("poweruser@test.com").let { assertEquals(Valid(it), nullableFieldValidation(it)) }

        Register(null).let { assertEquals(1, countErrors(nullableFieldValidation(it), Register::referredBy)) }
        Register("poweruser@").let { assertEquals(1, countErrors(nullableFieldValidation(it), Register::referredBy)) }
    }

    @Test
    fun setRequiredHint() {
        val validation =
            Validation<Register> {
                Register::referredBy required {
                    hint = "a referral is required"
                }
            }

        (validation shouldBeInvalid Register(null)) shouldContainOnlyError
            ValidationError.of(Register::referredBy, "a referral is required")
    }

    @Test
    fun setRequiredUserContext() {
        val validation =
            Validation<Register> {
                Register::referredBy required {
                    userContext = CustomUserContext("referrer_required")
                }
            }

        (validation shouldBeInvalid Register(null)) shouldContainOnlyError
            ValidationError.of(Register::referredBy, DEFAULT_REQUIRED_HINT, CustomUserContext("referrer_required"))
    }

    @Test
    fun validatingRequiredNullableValues() {
        val nullableRequiredValidation =
            Validation<String?> {
                required {
                    pattern(".+@.+".toRegex()).hint("must have correct format")
                }
            }

        "poweruser@test.com".let { assertEquals(Valid(it), nullableRequiredValidation(it)) }

        null.let { assertEquals(1, countErrors(nullableRequiredValidation(it))) }
        "poweruser@".let { assertEquals(1, countErrors(nullableRequiredValidation(it))) }
    }

    @Test
    fun requiredFunction() {
        val validation =
            Validation<String?> {
                required("trimmed", { it?.trim() }) {
                    hint = "string must be present"
                    minLength(2)
                }
            }

        validation shouldBeValid "abc"
        (validation shouldBeInvalid null) shouldContainOnlyError
            ValidationError.of("trimmed", "string must be present")
        (validation shouldBeInvalid "  a") shouldContainOnlyError
            ValidationError.of("trimmed", "must have at least 2 characters")
    }

    @Test
    fun deprecationWhenPropertyIsNotNull() {
        val validation =
            Validation<Foo> {
                // This itself will give a warning if no deprecation is suppressed
                @Suppress("DEPRECATION")
                Foo::bar required {}
            }

        validation shouldBeValid Foo("")
    }

    @Test
    fun setRequiredHintOnNullableExtension() {
        val validation =
            Validation<String?> {
                required {
                    hint = "value must be present"
                }
            }

        validation shouldBeValid "test"
        (validation shouldBeInvalid null) shouldContainOnlyError
            ValidationError.of("value must be present")
    }

    @Test
    fun setRequiredUserContextOnNullableExtension() {
        val validation =
            Validation<String?> {
                required {
                    userContext = CustomUserContext("value_required")
                }
            }

        validation shouldBeValid "test"
        (validation shouldBeInvalid null) shouldContainOnlyError
            ValidationError.of(DEFAULT_REQUIRED_HINT, CustomUserContext("value_required"))
    }

    @Test
    fun setRequiredHintOnFunctionReference() {
        val validation =
            Validation<Register> {
                Register::getReferral required {
                    hint = "referral method must return a value"
                }
            }

        validation shouldBeValid Register(referredBy = "test")
        (validation shouldBeInvalid Register(referredBy = null)) shouldContainOnlyError
            ValidationError.of(Register::getReferral, "referral method must return a value")
    }

    private data class Register(
        val referredBy: String? = null,
    ) {
        fun getReferral(): String? = referredBy
    }

    private data class Foo(
        val bar: String,
    )

    private data class CustomUserContext(
        val errorKey: String,
    )
}

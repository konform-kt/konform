package io.konform.validation.validationbuilder

import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.ValidationError
import io.konform.validation.constraints.const
import io.konform.validation.constraints.enum
import io.konform.validation.constraints.maxLength
import io.konform.validation.constraints.minLength
import io.konform.validation.constraints.pattern
import io.konform.validation.countFieldsWithErrors
import io.konform.validation.path.PathIndex
import io.konform.validation.path.PathSegment
import io.konform.validation.path.PathValue
import io.konform.validation.path.PropRef
import io.konform.validation.path.ValidationPath
import io.konform.validation.types.AlwaysInvalidValidation
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainExactlyErrors
import io.kotest.assertions.konform.shouldContainOnlyError
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateTest {
    @Test
    fun validateLambda() {
        val splitDoubleValidation =
            Validation<Register> {
                validate("getPasswordLambda", { r: Register -> r.password }) {
                    minLength(1)
                    maxLength(10)
                }
                validate("getEmailLambda", { r: Register -> r.email }) {
                    pattern(".+@.+".toRegex()).hint("must have correct format")
                }
            }

        splitDoubleValidation shouldBeValid Register(email = "tester@test.com", password = "a")
        splitDoubleValidation.shouldBeInvalid(Register(email = "tester@test.com", password = "")) {
            it.shouldContainOnlyError(ValidationError.of("getPasswordLambda", "must have at least 1 characters"))
        }
        splitDoubleValidation.shouldBeInvalid(Register(email = "tester@test.com", password = "aaaaaaaaaaa")) {
            it.shouldContainOnlyError(ValidationError.of("getPasswordLambda", "must have at most 10 characters"))
        }
        splitDoubleValidation.shouldBeInvalid(Register(email = "tester@", password = "")) {
            it.shouldContainExactlyErrors(
                ValidationError.of("getPasswordLambda", "must have at least 1 characters"),
                ValidationError.of("getEmailLambda", "must have correct format"),
            )
        }
    }

    @Test
    fun complexLambdaAccessors() {
        data class Token(
            val claims: Map<String, String>,
        )

        fun ValidationBuilder<Token>.validateClaim(
            key: String,
            validations: ValidationBuilder<String>.() -> Unit,
        ) {
            required("claim_$key", { data: Token -> data.claims[key] }) {
                validations()
            }
        }

        val accessTokenValidation =
            Validation<Token> {
                validateClaim("scope") {
                    const("access")
                }
                validateClaim("issuer") {
                    enum("bob", "eve")
                }
            }
        val refreshTokenVerification =
            Validation<Token> {
                validateClaim("scope") {
                    const("refresh")
                }
                validateClaim("issuer") {
                    enum("bob", "eve")
                }
            }

        Token(mapOf("scope" to "access", "issuer" to "bob")).let {
            assertEquals(Valid(it), accessTokenValidation(it))
            assertEquals(1, countFieldsWithErrors(refreshTokenVerification(it)))
        }
        Token(mapOf("scope" to "refresh", "issuer" to "eve")).let {
            assertEquals(Valid(it), refreshTokenVerification(it))
            assertEquals(1, countFieldsWithErrors(accessTokenValidation(it)))
        }
        Token(mapOf("issuer" to "alice")).let {
            assertEquals(2, countFieldsWithErrors(accessTokenValidation(it)))
            assertEquals(2, countFieldsWithErrors(refreshTokenVerification(it)))
        }
    }

    @Test
    fun ifPresent() {
        val validation =
            Validation<String?> {
                ifPresent("notBlank", { it?.ifBlank { null } }) {
                    run(AlwaysInvalidValidation)
                }
            }

        validation shouldBeValid null
        validation shouldBeValid ""
        validation shouldBeValid "\t"
        validation shouldBeInvalid "abc"
    }

    @Test
    fun validatePath() {
        val validation =
            Validation<String> {
                validate("sub", { it }) {
                    run(AlwaysInvalidValidation)
                }
                validate(PathSegment.toPathSegment(Register::password), { it }) {
                    run(AlwaysInvalidValidation)
                }
                validate(ValidationPath.of("sub", 1), { it }) {
                    run(AlwaysInvalidValidation)
                }
            }

        (validation shouldBeInvalid "").shouldContainExactlyErrors(
            ValidationError(ValidationPath(listOf(PathValue("sub"))), "always invalid"),
            ValidationError(ValidationPath(listOf(PropRef(Register::password))), "always invalid"),
            ValidationError(ValidationPath(listOf(PathValue("sub"), PathIndex(1))), "always invalid"),
        )
    }

    private data class Register(
        val password: String = "",
        val email: String = "",
        val referredBy: String? = null,
        val home: Address? = null,
    )

    private data class Address(
        val address: String = "",
        val country: String = "DE",
    )
}

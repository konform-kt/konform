package io.konform.validation

import io.konform.validation.constraints.const
import io.konform.validation.constraints.containsPattern
import io.konform.validation.constraints.enum
import io.konform.validation.constraints.maxLength
import io.konform.validation.constraints.minItems
import io.konform.validation.constraints.minLength
import io.konform.validation.constraints.pattern
import io.konform.validation.path.FuncRef
import io.konform.validation.path.PathKey
import io.konform.validation.path.ValidationPath
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainError
import io.kotest.assertions.konform.shouldContainExactlyErrors
import io.kotest.assertions.konform.shouldContainOnlyError
import io.kotest.assertions.konform.shouldHaveErrorCount
import io.kotest.assertions.konform.shouldNotContainErrorAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationBuilderTest {
    private fun ValidationBuilder<String>.containsANumber() = containsPattern("[0-9]".toRegex()) hint "must have at least one number"

    @Test
    fun singleValidation() {
        val oneValidation =
            Validation<Register> {
                Register::password {
                    minLength(1)
                }
            }

        Register(password = "a").let { assertEquals(Valid(it), oneValidation(it)) }
        Register(password = "").let { assertEquals(1, countErrors(oneValidation(it), Register::password)) }
    }

    @Test
    fun disjunctValidations() {
        val twoDisjunctValidations =
            Validation<Register> {
                Register::password {
                    minLength(1)
                }
                Register::password {
                    maxLength(10)
                }
            }

        Register(password = "a").let { assertEquals(Valid(it), twoDisjunctValidations(it)) }
        Register(password = "").let { assertEquals(1, countErrors(twoDisjunctValidations(it), Register::password)) }
        Register(password = "aaaaaaaaaaa").let { assertEquals(1, countErrors(twoDisjunctValidations(it), Register::password)) }
    }

    @Test
    fun overlappingValidations() {
        val overlappingValidations =
            Validation<Register> {
                Register::password {
                    minLength(8)
                    containsANumber()
                }
            }

        Register(password = "verysecure1").let { assertEquals(Valid(it), overlappingValidations(it)) }
        Register(password = "9").let { assertEquals(1, countErrors(overlappingValidations(it), Register::password)) }
        Register(password = "insecure").let { assertEquals(1, countErrors(overlappingValidations(it), Register::password)) }
        Register(password = "pass").let { assertEquals(2, countErrors(overlappingValidations(it), Register::password)) }
    }

    @Test
    fun validatingMultipleFields() {
        val overlappingValidations =
            Validation<Register> {
                Register::password {
                    minLength(8)
                    containsANumber()
                }

                Register::email {
                    pattern(".+@.+".toRegex()).hint("must have correct format")
                }
            }

        overlappingValidations shouldBeValid Register(email = "tester@test.com", password = "verysecure1")
        (overlappingValidations shouldBeInvalid Register(email = "tester@test.com")).shouldContainExactlyErrors(
            ValidationError.of(Register::password, "must have at least 8 characters"),
            ValidationError.of(Register::password, "must have at least one number"),
        )
        (overlappingValidations shouldBeInvalid Register(password = "verysecure1")) shouldContainOnlyError
            ValidationError.of(Register::email, "must have correct format")

        (overlappingValidations shouldBeInvalid Register()).shouldContainExactlyErrors(
            ValidationError.of(Register::password, "must have at least 8 characters"),
            ValidationError.of(Register::password, "must have at least one number"),
            ValidationError.of(Register::email, "must have correct format"),
        )
    }

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
    fun validatingRequiredFields() {
        val nullableFieldValidation =
            Validation<Register> {
                Register::referredBy required {
                    pattern(".+@.+".toRegex()).hint("must have correct format")
                }
            }

        Register(referredBy = "poweruser@test.com").let { assertEquals(Valid(it), nullableFieldValidation(it)) }

        Register(referredBy = null).let { assertEquals(1, countErrors(nullableFieldValidation(it), Register::referredBy)) }
        Register(referredBy = "poweruser@").let { assertEquals(1, countErrors(nullableFieldValidation(it), Register::referredBy)) }
    }

    @Test
    fun validatingNestedTypesDirectly() {
        val nestedTypeValidation =
            Validation<Register> {
                Register::home ifPresent {
                    Address::address {
                        minLength(1)
                    }
                }
            }

        Register(home = Address("Home")).let { assertEquals(Valid(it), nestedTypeValidation(it)) }
        Register(home = Address("")).let { assertEquals(1, countErrors(nestedTypeValidation(it), Register::home, Address::address)) }
    }

    @Test
    fun validatingOptionalNullableValues() {
        val nullableTypeValidation =
            Validation<String?> {
                ifPresent {
                    pattern(".+@.+".toRegex()).hint("must have correct format")
                }
            }

        null.let { assertEquals(Valid(it), nullableTypeValidation(it)) }
        "poweruser@test.com".let { assertEquals(Valid(it), nullableTypeValidation(it)) }
        "poweruser@".let { assertEquals(1, countErrors(nullableTypeValidation(it))) }
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
    fun functionAccessorSyntax() {
        val splitDoubleValidation =
            Validation<Register> {
                Register::getPasswordFun {
                    minLength(1)
                }
                Register::getPasswordFun {
                    maxLength(10)
                }
                Register::getEmailFun {
                    pattern(".+@.+".toRegex()).hint("must have correct format")
                }
            }

        splitDoubleValidation shouldBeValid Register(email = "tester@test.com", password = "a")

        (
            splitDoubleValidation shouldBeInvalid
                Register(
                    email = "tester@test.com",
                    password = "",
                )
        ) shouldContainOnlyError ValidationError.of(FuncRef(Register::getPasswordFun), "must have at least 1 characters")

        (splitDoubleValidation shouldBeInvalid Register(email = "tester@test.com", password = "aaaaaaaaaaa")) shouldContainOnlyError
            ValidationError.of(FuncRef(Register::getPasswordFun), "must have at most 10 characters")

        (splitDoubleValidation shouldBeInvalid Register(email = "tester@")).shouldContainExactlyErrors(
            ValidationError.of(FuncRef(Register::getPasswordFun), "must have at least 1 characters"),
            ValidationError.of(FuncRef(Register::getEmailFun), "must have correct format"),
        )
    }

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
    fun validateLists() {
        data class Data(
            val registrations: List<Register> = emptyList(),
        )

        val listValidation =
            Validation<Data> {
                Data::registrations onEach {
                    Register::email {
                        minLength(3)
                    }
                }
            }

        Data().let { assertEquals(Valid(it), listValidation(it)) }
        Data(registrations = listOf(Register(email = "valid"), Register(email = "a")))
            .let {
                assertEquals(1, countErrors(listValidation(it), Data::registrations, 1, Register::email))
            }
        Data(registrations = listOf(Register(email = "a"), Register(email = "ab")))
            .let {
                assertEquals(2, countFieldsWithErrors(listValidation(it)))
                assertEquals(1, countErrors(listValidation(it), Data::registrations, 1, Register::email))
            }
    }

    @Test
    fun validateNullableLists() {
        data class Data(
            val registrations: List<Register>?,
        )

        val listValidation =
            Validation<Data> {
                Data::registrations ifPresent {
                    minItems(1)
                    onEach {
                        Register::email {
                            minLength(3)
                        }
                    }
                }
            }

        Data(null).let { assertEquals(Valid(it), listValidation(it)) }
        Data(emptyList()).let { assertEquals(1, countErrors(listValidation(it), Data::registrations)) }
        Data(registrations = listOf(Register(email = "valid"), Register(email = "a")))
            .let {
                assertEquals(1, countErrors(listValidation(it), Data::registrations, 1, Register::email))
            }
        Data(registrations = listOf(Register(email = "a"), Register(email = "ab")))
            .let {
                assertEquals(2, countFieldsWithErrors(listValidation(it)))
                assertEquals(1, countErrors(listValidation(it), Data::registrations, 1, Register::email))
            }
    }

    @Test
    fun validateArrays() {
        data class Data(
            val registrations: Array<Register> = emptyArray(),
        )

        val arrayValidation =
            Validation<Data> {
                Data::registrations onEach {
                    Register::email {
                        minLength(3)
                    }
                }
            }

        Data().let { assertEquals(Valid(it), arrayValidation(it)) }
        Data(registrations = arrayOf(Register(email = "valid"), Register(email = "a")))
            .let {
                assertEquals(1, countErrors(arrayValidation(it), Data::registrations, 1, Register::email))
            }
        Data(registrations = arrayOf(Register(email = "a"), Register(email = "ab")))
            .let {
                assertEquals(2, countFieldsWithErrors(arrayValidation(it)))
                assertEquals(1, countErrors(arrayValidation(it), Data::registrations, 1, Register::email))
            }
    }

    @Test
    fun validateNullableArrays() {
        data class Data(
            val registrations: Array<Register>?,
        )

        val arrayValidation =
            Validation<Data> {
                Data::registrations ifPresent {
                    minItems(1)
                    onEach {
                        Register::email {
                            minLength(3)
                        }
                    }
                }
            }

        Data(null).let { assertEquals(Valid(it), arrayValidation(it)) }
        Data(emptyArray()).let { assertEquals(1, countErrors(arrayValidation(it), Data::registrations)) }
        Data(registrations = arrayOf(Register(email = "valid"), Register(email = "a")))
            .let {
                assertEquals(1, countErrors(arrayValidation(it), Data::registrations, 1, Register::email))
            }
        Data(registrations = arrayOf(Register(email = "a"), Register(email = "ab")))
            .let {
                assertEquals(2, countFieldsWithErrors(arrayValidation(it)))
                assertEquals(1, countErrors(arrayValidation(it), Data::registrations, 1, Register::email))
            }
    }

    @Test
    fun validateX() {
    }

    @Test
    fun validateHashMaps() {
        data class Data(
            val registrations: Map<String, Register> = emptyMap(),
        )

        val mapValidation =
            Validation<Data> {
                Data::registrations onEach {
                    Map.Entry<String, Register>::value {
                        Register::email {
                            minLength(2)
                        }
                    }
                }
            }

        mapValidation shouldBeValid Data()

        mapValidation.shouldBeInvalid(
            Data(
                registrations =
                    mapOf(
                        "user1" to Register(email = "valid"),
                        "user2" to Register(email = "a"),
                    ),
            ),
        ) {
            it shouldContainOnlyError
                ValidationError(
                    ValidationPath.of(Data::registrations, PathKey("user2"), Register::email),
                    "must have at least 2 characters",
                )

            it.shouldContainError(
                listOf(Data::registrations, PathKey("user2"), Register::email),
                "must have at least 2 characters",
            )
            it.shouldNotContainErrorAt(Data::registrations, PathKey("user1"), Register::email)
            it.shouldHaveErrorCount(1)
        }
    }

    @Test
    fun validateNullableHashMaps() {
        data class Data(
            val registrations: Map<String, Register>? = null,
        )

        val validation =
            Validation<Data> {
                Data::registrations ifPresent {
                    onEach {
                        Map.Entry<String, Register>::value {
                            Register::email {
                                minLength(2)
                            }
                        }
                    }
                }
            }

        validation shouldBeValid Data(null)
        validation shouldBeValid Data(emptyMap())

        val invalidData =
            Data(
                registrations =
                    mapOf(
                        "user1" to Register(email = "valid"),
                        "user2" to Register(email = "a"),
                    ),
            )

        (validation shouldBeInvalid invalidData) shouldContainOnlyError
            ValidationError(
                ValidationPath.of(Data::registrations, PathKey("user2"), Register::email),
                "must have at least 2 characters",
            )
    }

    @Test
    fun composeValidations() {
        val addressValidation =
            Validation<Address> {
                Address::address {
                    minLength(1)
                }
            }

        val validation =
            Validation<Register> {
                Register::home ifPresent {
                    run(addressValidation)
                }
            }

        assertEquals(1, countFieldsWithErrors(validation(Register(home = Address()))))
    }

    @Test
    fun replacePlaceholderInString() {
        val validation =
            Validation<Register> {
                Register::password {
                    minLength(8)
                }
            }
        assertTrue(validation(Register(password = "")).errors.messagesAtDataPath(Register::password)[0].contains("8"))
    }

    private data class Register(
        val password: String = "",
        val email: String = "",
        val referredBy: String? = null,
        val home: Address? = null,
    ) {
        fun getPasswordFun() = password

        fun getEmailFun() = email
    }

    private data class Address(
        val address: String = "",
        val country: String = "DE",
    )
}

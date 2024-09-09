package io.konform.validation

import io.konform.validation.jsonschema.const
import io.konform.validation.jsonschema.enum
import io.konform.validation.jsonschema.minItems
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationBuilderTest {
    // Some example constraints for Testing
    fun ValidationBuilder<String>.minLength(minValue: Int) =
        addConstraint("must have at least {0} characters", minValue.toString()) { it.length >= minValue }

    fun ValidationBuilder<String>.maxLength(minValue: Int) =
        addConstraint("must have at most {0} characters", minValue.toString()) { it.length <= minValue }

    fun ValidationBuilder<String>.matches(regex: Regex) = addConstraint("must have correct format") { it.contains(regex) }

    fun ValidationBuilder<String>.containsANumber() = matches("[0-9]".toRegex()) hint "must have at least one number"

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
                    matches(".+@.+".toRegex())
                }
            }

        Register(email = "tester@test.com", password = "verysecure1").let { assertEquals(Valid(it), overlappingValidations(it)) }
        Register(email = "tester@test.com").let {
            assertEquals(1, countFieldsWithErrors(overlappingValidations(it)))
            assertEquals(2, countErrors(overlappingValidations(it), Register::password))
        }
        Register(password = "verysecure1").let { assertEquals(1, countErrors(overlappingValidations(it), Register::email)) }
        Register().let { assertEquals(2, countFieldsWithErrors(overlappingValidations(it))) }
    }

    @Test
    fun validatingNullableFields() {
        val nullableFieldValidation =
            Validation<Register> {
                Register::referredBy ifPresent {
                    matches(".+@.+".toRegex())
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
                    matches(".+@.+".toRegex())
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
                    matches(".+@.+".toRegex())
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
                    matches(".+@.+".toRegex())
                }
            }

        "poweruser@test.com".let { assertEquals(Valid(it), nullableRequiredValidation(it)) }

        null.let { assertEquals(1, countErrors(nullableRequiredValidation(it))) }
        "poweruser@".let { assertEquals(1, countErrors(nullableRequiredValidation(it))) }
    }

    @Test
    fun alternativeSyntax() {
        val splitDoubleValidation =
            Validation<Register> {
                Register::password.has.minLength(1)
                Register::password.has.maxLength(10)
                Register::email.has.matches(".+@.+".toRegex())
            }

        Register(email = "tester@test.com", password = "a").let { assertEquals(Valid(it), splitDoubleValidation(it)) }
        Register(
            email = "tester@test.com",
            password = "",
        ).let { assertEquals(1, countErrors(splitDoubleValidation(it), Register::password)) }
        Register(email = "tester@test.com", password = "aaaaaaaaaaa").let {
            assertEquals(1, countErrors(splitDoubleValidation(it), Register::password))
        }
        Register(email = "tester@").let { assertEquals(2, countFieldsWithErrors(splitDoubleValidation(it))) }
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
                    matches(".+@.+".toRegex())
                }
            }

        Register(email = "tester@test.com", password = "a").let { assertEquals(Valid(it), splitDoubleValidation(it)) }
        Register(
            email = "tester@test.com",
            password = "",
        ).let { assertEquals(1, countErrors(splitDoubleValidation(it), Register::getPasswordFun)) }
        Register(email = "tester@test.com", password = "aaaaaaaaaaa").let {
            assertEquals(1, countErrors(splitDoubleValidation(it), Register::getPasswordFun))
        }
        Register(email = "tester@").let { assertEquals(2, countFieldsWithErrors(splitDoubleValidation(it))) }
    }

    @Test
    fun lambdaAccessorSyntax() {
        val splitDoubleValidation =
            Validation<Register> {
                val getPassword = { r: Register -> r.password }
                val getEmail = { r: Register -> r.email }
                getPassword("getPasswordLambda") {
                    minLength(1)
                }
                getPassword("getPasswordLambda") {
                    maxLength(10)
                }
                getEmail("getEmailLambda") {
                    matches(".+@.+".toRegex())
                }
            }

        Register(email = "tester@test.com", password = "a").let { assertEquals(Valid(it), splitDoubleValidation(it)) }
        Register(
            email = "tester@test.com",
            password = "",
        ).let {
            assertEquals(1, countErrors(splitDoubleValidation(it), "getPasswordLambda"))
        }
        Register(email = "tester@test.com", password = "aaaaaaaaaaa").let {
            assertEquals(1, countErrors(splitDoubleValidation(it), "getPasswordLambda"))
        }
        Register(email = "tester@").let {
            assertEquals(2, countFieldsWithErrors(splitDoubleValidation(it)))
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
            val getClaimValue = { data: Token -> data.claims[key] }
            getClaimValue.required(".claims[$key]") {
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

        Data().let { assertEquals(Valid(it), mapValidation(it)) }
        Data(
            registrations =
                mapOf(
                    "user1" to Register(email = "valid"),
                    "user2" to Register(email = "a"),
                ),
        ).let {
            assertEquals(0, countErrors(mapValidation(it), Data::registrations, "user1", Register::email))
            assertEquals(1, countErrors(mapValidation(it), Data::registrations, "user2", Register::email))
        }
    }

    @Test
    fun validateNullableHashMaps() {
        data class Data(
            val registrations: Map<String, Register>? = null,
        )

        val mapValidation =
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

        Data(null).let { assertEquals(Valid(it), mapValidation(it)) }
        Data(emptyMap()).let { assertEquals(Valid(it), mapValidation(it)) }
        Data(
            registrations =
                mapOf(
                    "user1" to Register(email = "valid"),
                    "user2" to Register(email = "a"),
                ),
        ).let {
            assertEquals(0, countErrors(mapValidation(it), Data::registrations, "user1", Register::email))
            assertEquals(1, countErrors(mapValidation(it), Data::registrations, "user2", Register::email))
        }
    }

    @Test
    fun composeValidations() {
        val addressValidation =
            Validation<Address> {
                Address::address.has.minLength(1)
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
                Register::password.has.minLength(8)
            }
        assertTrue(validation(Register(password = ""))[Register::password]!![0].contains("8"))
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

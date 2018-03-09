package io.konform.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationBuilderTest {

    // Some example constraints for Testing
    fun ValidationBuilder<String>.minLength(minValue: Int) =
        addConstraint("must have at least {1} characters", minValue.toString()) { it.length >= minValue }

    fun ValidationBuilder<String>.maxLength(minValue: Int) =
        addConstraint("must have at most {1} characters", minValue.toString()) { it.length <= minValue }

    fun ValidationBuilder<String>.matches(regex: Regex) =
        addConstraint("must have correct format") { it.contains(regex) }

    fun ValidationBuilder<String>.containsANumber() =
        matches("[0-9]".toRegex()) hint "must have at least one number"

    @Test
    fun singleValidation() {
        val oneValidation = Validation<Register> {
            Register::password {
                minLength(1)
            }
        }

        Register(password = "a").let { assertEquals(Valid(it), oneValidation(it)) }
        Register(password = "").let { assertEquals(1, countErrors(oneValidation(it), Register::password)) }
    }

    @Test
    fun disjunctValidations() {
        val twoDisjunctValidations = Validation<Register> {
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
        val overlappingValidations = Validation<Register> {
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
        val overlappingValidations = Validation<Register> {
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
    fun validatingNullableTypes() {
        val nullableTypeValidation = Validation<Register> {
            Register::referredBy ifPresent {
                matches(".+@.+".toRegex())
            }
        }

        Register(referredBy = null).let { assertEquals(Valid(it), nullableTypeValidation(it)) }
        Register(referredBy = "poweruser@test.com").let { assertEquals(Valid(it), nullableTypeValidation(it)) }
        Register(referredBy = "poweruser@").let { assertEquals(1, countErrors(nullableTypeValidation(it), Register::referredBy)) }
    }

    @Test
    fun validatingRequiredTypes() {
        val nullableTypeValidation = Validation<Register> {
            Register::referredBy required {
                matches(".+@.+".toRegex())
            }
        }

        Register(referredBy = "poweruser@test.com").let { assertEquals(Valid(it), nullableTypeValidation(it)) }

        Register(referredBy = null).let { assertEquals(1, countErrors(nullableTypeValidation(it), Register::referredBy)) }
        Register(referredBy = "poweruser@").let { assertEquals(1, countErrors(nullableTypeValidation(it), Register::referredBy)) }
    }

    @Test
    fun validatingNestedTypesDirectly() {
        val nestedTypeValidation = Validation<Register> {
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
    fun alternativeSyntax() {
        val splitDoubleValidation = Validation<Register> {
            Register::password.has.minLength(1)
            Register::password.has.maxLength(10)
            Register::email.has.matches(".+@.+".toRegex())
        }

        Register(email = "tester@test.com", password = "a").let { assertEquals(Valid(it), splitDoubleValidation(it)) }
        Register(email = "tester@test.com", password = "").let { assertEquals(1, countErrors(splitDoubleValidation(it), Register::password)) }
        Register(email = "tester@test.com", password = "aaaaaaaaaaa").let { assertEquals(1, countErrors(splitDoubleValidation(it), Register::password)) }
        Register(email = "tester@").let { assertEquals(2, countFieldsWithErrors(splitDoubleValidation(it))) }
    }

    @Test
    fun validateLists() {
        val listValidation= Validation<Register> {
            Register::previousAddresses onEach {
                Address::address {
                    minLength(3)
                }
            }
        }

        Register().let { assertEquals(Valid(it), listValidation(it)) }
        Register(previousAddresses = listOf(Address("valid"), Address("ab")))
            .let {
                assertEquals(1, countErrors(listValidation(it), Register::previousAddresses, 1, Address::address))
            }
        Register(previousAddresses = listOf(Address("a"), Address("ab")))
            .let {
                assertEquals(2, countFieldsWithErrors(listValidation(it)))
            }
        Register(previousAddresses = listOf(Address("a"), Address("ab")))
            .let {
                assertEquals(1, countErrors(listValidation(it), Register::previousAddresses, 1, Address::address))
            }
    }

    @Test
    fun replacePlaceholderInString() {
        val validation = Validation<Register> {
            Register::password.has.minLength(8)
        }
        assertTrue(validation(Register(password = ""))[Register::password]!![0].contains("8"))
    }

    private fun <T> countFieldsWithErrors(overlappingValidations: ValidationResult<T>) = (overlappingValidations as Invalid).errors.size
    private fun countErrors(validationResult: ValidationResult<Register>, vararg properties: Any) = validationResult.get(*properties)?.size
        ?: 0

    private data class Register(val password: String = "", val email: String = "", val referredBy: String? = null, val home: Address? = null, val previousAddresses: List<Address> = emptyList())
    private data class Address(val address: String = "", val country: String = "DE")
}

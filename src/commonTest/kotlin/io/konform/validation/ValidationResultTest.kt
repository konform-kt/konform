package io.konform.validation

import io.konform.validation.checks.maxLength
import io.konform.validation.checks.minLength
import io.konform.validation.checks.pattern
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidationResultTest {
    private data class ValidationError(val message: String)

    @Test
    fun singleValidation() {
        val personValidator = Validation<Person, ValidationError> {
            Person::name {
                minLength(1) { ValidationError("must have at least 1 characters") }
            }

            Person::addresses onEach {
                Address::city {
                    City::postalCode {
                        minLength(4) { ValidationError("must have at least 4 characters") }
                        maxLength(5) { ValidationError("must have at least 5 characters") }
                        pattern("\\d{4,5}") { ValidationError("must be a four or five digit number") }
                    }
                }
            }
        }

        val result = personValidator.validate(Person("", addresses = listOf(Address(City("", "")))))

        if (result is Invalid<*>) {
            assertEquals(3, result.errors.size)
            val (firstError, secondError, thirdError) = result.errors

            assertEquals(".name", firstError.path.toString())
            assertEquals("must have at least 1 characters", (firstError.error as ValidationError).message)

            assertEquals(".addresses[0].city.postalCode", secondError.path.toString())
            assertEquals("must have at least 4 characters", (secondError.error as ValidationError).message)

            assertEquals(".addresses[0].city.postalCode", thirdError.path.toString())
            assertEquals("must be a four or five digit number", (thirdError.error as ValidationError).message)
        }
    }

    private data class Person(val name: String, val addresses: List<Address>)
    private data class Address(val city: City)
    private data class City(val postalCode: String, val cityName: String)
}


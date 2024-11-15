package io.konform.validation.validationbuilder

import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.constraints.pattern
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainOnlyError
import kotlin.test.Test

class DynamicValidationTest {
    val validation =
        Validation<Address> {
            runDynamic { address: Address ->
                Validation<Address> {
                    Address::postalCode {
                        when (address.countryCode) {
                            "US" -> pattern("[0-9]{5}")
                            else -> pattern("[A-Z]+")
                        }
                    }
                }
            }
        }

    @Test
    fun dynamicValidation() {
        validation shouldBeValid Address("US", "12345")
        validation shouldBeValid Address("DE", "ABC")

        (validation shouldBeInvalid Address("US", "")) shouldContainOnlyError
            ValidationError.of(Address::postalCode, """must match pattern '[0-9]{5}'""")
        (validation shouldBeInvalid Address("DE", "123")) shouldContainOnlyError
            ValidationError.of(Address::postalCode, """must match pattern '[A-Z]+'""")
    }
}

data class Address(
    val countryCode: String,
    val postalCode: String,
)

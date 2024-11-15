package io.konform.validation.validationbuilder

import io.konform.validation.Constraint
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.ValidationError
import io.konform.validation.constraints.pattern
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainOnlyError
import kotlin.test.Test

class DynamicValidationTest {
    @Test
    fun dynamicValidation1() {
        val validation =
            Validation<Address> {
                dynamic { address ->
                    Address::postalCode {
                        when (address.countryCode) {
                            "US" -> pattern("[0-9]{5}")
                            else -> pattern("[A-Z]+")
                        }
                    }
                }
            }

        validation shouldBeValid Address("US", "12345")
        validation shouldBeValid Address("DE", "ABC")

        (validation shouldBeInvalid Address("US", "")) shouldContainOnlyError
            ValidationError.of(Address::postalCode, """must match pattern '[0-9]{5}'""")
        (validation shouldBeInvalid Address("DE", "123")) shouldContainOnlyError
            ValidationError.of(Address::postalCode, """must match pattern '[A-Z]+'""")
    }

    @Test
    fun dynamicValidation2() {
        val validation =
            Validation<Range> {
                dynamic { range ->
                    Range::to {
                        largerThan(range.from)
                    }
                }
            }

        validation shouldBeValid Range(0, 1)
        (validation shouldBeInvalid Range(1, 0)) shouldContainOnlyError
            ValidationError.of(
                Range::to,
                "must be larger than 1",
            )
    }

    @Test
    fun dynamicOnProperty() {
        val validation =
            Validation<Range> {
                Range::to dynamic { range ->
                    largerThan(range.from)
                }
            }

        validation shouldBeValid Range(0, 1)
        (validation shouldBeInvalid Range(1, 0)) shouldContainOnlyError
            ValidationError.of(
                Range::to,
                "must be larger than 1",
            )
    }

    @Test
    fun dynamicWithLambda() {
        val validation =
            Validation<Range> {
                dynamic(Range::to, { it.from to it.to }) { (from, to) ->
                    constrain("must be larger than from") {
                        to > from
                    }
                }
            }

        validation shouldBeValid Range(0, 1)
        (validation shouldBeInvalid Range(1, 0)) shouldContainOnlyError
            ValidationError.of(
                Range::to,
                "must be larger than from",
            )
    }
}

data class Address(
    val countryCode: String,
    val postalCode: String,
)

data class Range(
    val from: Int,
    val to: Int,
)

fun ValidationBuilder<Int>.largerThan(other: Int): Constraint<Int> = constrain("must be larger than $other") { it > other }

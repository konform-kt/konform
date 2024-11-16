package io.konform.validation.validationbuilder

import io.konform.validation.Constraint
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.ValidationError
import io.konform.validation.constraints.minimum
import io.konform.validation.constraints.pattern
import io.konform.validation.path.ValidationPath
import io.konform.validation.types.AlwaysInvalidValidation
import io.konform.validation.types.EmptyValidation
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainExactlyErrors
import io.kotest.assertions.konform.shouldContainOnlyError
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

        val usResult = (validation shouldBeInvalid Address("US", ""))
        usResult.errors shouldHaveSize 1
        usResult.errors[0].path shouldBe ValidationPath.of(Address::postalCode)
        usResult.errors[0].message shouldContain """must match pattern '"""
        val deResult = (validation shouldBeInvalid Address("DE", "123"))
        deResult.errors shouldHaveSize 1
        deResult.errors[0].path shouldBe ValidationPath.of(Address::postalCode)
        deResult.errors[0].message shouldContain """must match pattern '"""
    }

    @Test
    fun dynamicValidation2() {
        val validation =
            Validation<Address> {
                Address::postalCode dynamic { address ->
                    when (address.countryCode) {
                        "US" -> pattern("[0-9]{5}")
                        else -> pattern("[A-Z]+")
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
    fun dynamicValidation3() {
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

    @Test
    fun runDynamic() {
        val validation =
            Validation<String> {
                runDynamic {
                    if (it == "a") {
                        AlwaysInvalidValidation
                    } else {
                        EmptyValidation
                    }
                }
            }

        validation shouldBeValid "b"
        (validation shouldBeInvalid "a") shouldContainOnlyError
            ValidationError(
                ValidationPath.EMPTY,
                "always invalid",
            )
    }

    @Test
    fun outerDynamic() {
        val validation =
            Validation<Nested1> {
                dynamic { nested1 ->
                    Nested1::nested2s onEach {
                        Nested2::value {
                            minimum(nested1.minimum)
                        }
                    }
                }
            }

        val invalid =
            Nested1(
                20,
                listOf(
                    Nested2(5),
                    Nested2(25),
                    Nested2(10),
                ),
            )

        val valid = invalid.copy(minimum = 1)

        validation shouldBeValid valid
        (validation shouldBeInvalid invalid).shouldContainExactlyErrors(
            ValidationError(ValidationPath.of(Nested1::nested2s, 0, Nested2::value), "must be at least '20'"),
            ValidationError(ValidationPath.of(Nested1::nested2s, 2, Nested2::value), "must be at least '20'"),
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

data class Nested1(
    val minimum: Int,
    val nested2s: List<Nested2>,
)

data class Nested2(
    val value: Int,
)

package io.konform.validation

import io.konform.validation.constraints.minimum
import io.konform.validation.path.ValidationPath
import io.konform.validation.types.EmptyValidation
import io.konform.validation.types.ValidateAll
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainExactlyErrors
import io.kotest.assertions.konform.shouldContainOnlyError
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

class ListValidationTest {
    private val validation1 =
        Validation<Int> {
            minimum(0)
        }
    private val validation2 =
        Validation<Int> {
            minimum(10)
        }

    @Test
    fun flattenNone() {
        val result = listOf<Validation<Int>>().flatten()

        result shouldBeSameInstanceAs EmptyValidation
        result shouldBeValid 0
        result shouldBeValid -100
    }

    @Test
    fun flattenOne() {
        val result = listOf(validation1).flatten()

        result shouldBeSameInstanceAs validation1
        result shouldBeValid 0
        result shouldBeInvalid -100
    }

    @Test
    fun flattenMore() {
        val result = listOf(validation1, validation2).flatten()

        result.shouldBeInstanceOf<ValidateAll<Int>>()

        result shouldBeValid 10
        (result shouldBeInvalid 5) shouldContainOnlyError ValidationError(ValidationPath.EMPTY, "must be at least '10'")
        (result shouldBeInvalid -1).shouldContainExactlyErrors(
            ValidationError(ValidationPath.EMPTY, "must be at least '0'"),
            ValidationError(ValidationPath.EMPTY, "must be at least '10'"),
        )
    }
}

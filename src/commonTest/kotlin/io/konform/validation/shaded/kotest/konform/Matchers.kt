// Shade the kotest konform assertions to avoid the circular dependency and develop independently
@file:Suppress("PackageDirectoryMismatch")

package io.kotest.assertions.konform

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.filterDataPath
import io.konform.validation.messagesAtDataPath
import io.konform.validation.path.ValidationPath
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

infix fun <T> Validation<T>.shouldBeValid(value: T) = this should beValid(value)

fun <A> beValid(a: A) =
    object : Matcher<Validation<A>> {
        override fun test(value: Validation<A>): MatcherResult =
            value(a).let {
                MatcherResult(
                    it is Valid,
                    { "$a should be valid, but was: $it" },
                    { "$a should not be valid" },
                )
            }
    }

infix fun <T> Validation<T>.shouldBeInvalid(value: T): Invalid {
    this should beInvalid(value)
    return this(value) as Invalid
}

fun <A> beInvalid(a: A) =
    object : Matcher<Validation<A>> {
        override fun test(value: Validation<A>): MatcherResult =
            value(a).let {
                MatcherResult(
                    it is Invalid,
                    { "$a should be invalid" },
                    { "$a should not be invalid, but was: $it" },
                )
            }
    }

inline fun <T> Validation<T>.shouldBeInvalid(
    value: T,
    fn: (Invalid) -> Unit,
): Invalid {
    val invalid = this.shouldBeInvalid(value)
    fn(invalid)
    return invalid
}

/**
 * Asserts that the validation result contains an error for the given field.
 */
fun Invalid.shouldContainError(
    path: ValidationPath,
    error: String,
) {
    this.errors shouldContain ValidationError(path, error)
}

/**
 * Asserts that the validation result contains an error for the given field.
 */
fun Invalid.shouldContainError(error: ValidationError) {
    this.errors shouldContain error
}

/**
 * Asserts that the validation result contains an error for the given field.
 * @param propertyPaths a list of paths to the error
 */
fun Invalid.shouldContainError(
    propertyPaths: Collection<Any>,
    error: String,
) {
    val array = propertyPaths.toTypedArray()
    val path = ValidationPath.of(*array)
    // For a clearer error message
    this.shouldContainError(path, error)
    val errors = this.errors.messagesAtDataPath(*array)
    errors.shouldNotBeNull()
    errors shouldContain error
}

fun Invalid.shouldNotContainErrorAt(vararg propertyPaths: Any) {
    val path = ValidationPath.of(*propertyPaths)
    this.errors.map { it.dataPath } shouldNotContain path
    this.errors.filterDataPath(propertyPaths).shouldBeEmpty()
}

infix fun Invalid.shouldHaveErrorCount(count: Int) = this.errors shouldHaveSize count

infix fun Invalid.shouldContainOnlyError(error: ValidationError) {
    this.errors shouldBe listOf(error)
}

fun Invalid.shouldContainExactlyErrors(vararg errors: ValidationError) = this.errors shouldContainExactlyInAnyOrder errors.toList()

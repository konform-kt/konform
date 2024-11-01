package io.konform.validation.path

import kotlin.reflect.KCallable

/**
 * Represents a path to a validation.
 *
 * Example:
 * ```
 * data class Person(val name: String)
 * val validation = Validation<Person> {
 *   Person::name {
 *     notBlank()
 *   }
 * }
 * val result = validation.validate(Person("")) as Invalid
 * result.errors[0].path == FunctionOrPropertyPath()
 * ```
 * */
public sealed interface ValidationPathElement {
    public companion object {
        public fun toStringPath(path: List<ValidationPathElement>): String {
            TODO()
        }
    }
}

/**
 * Represents a path through a function or property.
 *
 * Example:
 * ```
 * data class Person(val name: String) {
 *   fun trimmedName() = name.trim()
 * }
 * val validation = Validation<Person> {
 *   Person::name {
 *     notBlank()
 *   }
 *   Person::trimmedName {
 *     notBlank()
 *   }
 * }
 * val result = validation.validate(Person("")) as Invalid
 * result
 * ```
 */
public data class FunctionOrPropertyPathElement(
    val property: KCallable<*>,
) : ValidationPathElement

/**
 * A way to extend the validation path by adding any custom path data.
 * TODO: Use case, API, example
 * */
public interface CustomPathElement : ValidationPathElement

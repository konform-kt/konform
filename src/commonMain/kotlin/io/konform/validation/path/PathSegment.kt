package io.konform.validation.path

import kotlin.reflect.KCallable
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

/** Represents a path to a validation. */
public data class ValidationPath(
    val segments: List<PathSegment>
) {
    /** A JSONPath-ish representation of the path. */
    public val pathString: String
        get() = segments.joinToString("") { it.pathString }
}

/**
 * Represents a path element in a validation.
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
public sealed interface PathSegment {
    /** A JSONPath-ish representation of the path segment. */
    public val pathString: String

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
     * result.errors[0] =
     * ```
     */
    public data class Property(val callable: KCallable<*>) : PathSegment {
        override val pathString: String
            get() = when (callable) {
                is KProperty1<*, *> -> ".${callable.name}"
                is KFunction1<*, *> -> ".${callable.name}()"
                else -> throw IllegalArgumentException("Unsupported KCallable in path $callable")
            }
    }

    public data class ArrayIndex(val index: Int) : PathSegment {
        override val pathString: String get() = "[$index]"
    }

    /**
     * A way to extend the validation path by adding any custom path data.
     * TODO: Use case, API, example
     * */
    public interface CustomSegment : PathSegment

}





package io.konform.validation.path

import kotlin.reflect.KClass
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

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
 * result.errors[0].path == PathSegment.Property(Person::name)
 * ```
 * */
public sealed interface PathSegment {
    /** A JSONPath-ish representation of the path segment. */
    public val pathString: String

    public companion object {
        /**
         * Converts [Any] value to its corresponding [PathSegment]
         * If it is already a PathSegment it will be returned
         * otherwise the most appropriate subtype of PathSegment will be returned,
         * e.g. an [KFunction1] will become a [PathSegment.Func].
         * If no more appropriate subtype exists, [PathValue] will be returned.
         */
        public fun toPathSegment(pathSegment: Any?): PathSegment =
            when (pathSegment) {
                is PathSegment -> pathSegment
                is KProperty1<*, *> -> pathSegment.toPathSegment()
                is KFunction1<*, *> -> pathSegment.toPathSegment()
                is Int -> PathIndex(pathSegment)
                is Map.Entry<*, *> -> pathSegment.toPathSegment()
                is KClass<*> -> PathClass(pathSegment)
                else -> PathValue(pathSegment)
            }
    }
}

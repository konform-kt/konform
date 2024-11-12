package io.konform.validation.path

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
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
         * e.g. an [KFunction1] will become a [PathSegment.Function].
         * If no more appropriate subtype exists, [ProvidedValue] will be returned.
         */
        public fun toPathSegment(pathSegment: Any): PathSegment =
            when (pathSegment) {
                is PathSegment -> pathSegment
                is KProperty1<*, *> -> pathSegment.toPathSegment()
                is KFunction1<*, *> -> pathSegment.toPathSegment()
                is Int -> Index(pathSegment)
                is Map.Entry<*, *> -> pathSegment.toPathSegment()
                is String -> ProvidedString(pathSegment)
                else -> ProvidedValue(pathSegment)
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
     * result.errors[0] =
     * ```
     */
    public data class Property(
        val property: KProperty1<*, *>,
    ) : PathSegment {
        override val pathString: String get() = ".${property.name}"

        override fun toString(): String = "Property(${property.name})"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Property
            return looseCompareCallable(property, other.property)
        }

        override fun hashCode(): Int = property.name.hashCode()
    }

    public data class Function(
        val function: KFunction1<*, *>,
    ) : PathSegment {
        val x: KFunction2<Int, Int, Int> = TODO()
        override val pathString: String get() = ".${function.name}"

        override fun toString(): String = "Function(${function.name})"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Function

            return looseCompareCallable(function, other.function)
        }

        override fun hashCode(): Int = function.name.hashCode()
    }

    /** A path for a */
    public data class KCls(
        val kcls: KClass<*>,
    ) : PathSegment {
        private val name get() = kcls.simpleName ?: "Anonymous"

        override val pathString: String get() = name

        override fun toString(): String = "KCls($name)"
    }

    /** An index to an array, list, or other iterable. */
    public data class Index(
        val index: Int,
    ) : PathSegment {
        override val pathString: String get() = "[$index]"

        override fun toString(): String = "Index($index)"
    }

    /** The key of a map. */
    public data class MapKey(
        val key: Any?,
    ) : PathSegment {
        override val pathString: String get() = ".$key"

        override fun toString(): String = "MapKey($key)"
    }

    /** A string provided by the user, usually a field name. */
    public data class ProvidedString(
        val string: String,
    ) : PathSegment {
        override val pathString: String get() = ".$string"

        override fun toString(): String = "ProvidedString($string)"
    }

    /** Any non-string value provided by the user. */
    public data class ProvidedValue(
        val value: Any,
    ) : PathSegment {
        override val pathString: String get() = ".$value"

        override fun toString(): String = "ProvidedValue($value)"
    }
}

public fun KProperty1<*, *>.toPathSegment(): PathSegment.Property = PathSegment.Property(this)

public fun KFunction1<*, *>.toPathSegment(): PathSegment.Function = PathSegment.Function(this)

public fun Map.Entry<*, *>.toPathSegment(): PathSegment.MapKey = PathSegment.MapKey(key)

/**
 * On some platforms (JS) a ::property does not seem to be guaranteed the same in different context.
 * To remedy that do a looser comparison which should succeed, and should be good enough for validation purposes.
 * */
private fun looseCompareCallable(
    first: KCallable<*>,
    second: KCallable<*>,
): Boolean {
    if (first === second) return true
    if (first::class != second::class) return false
    return first.name == second.name
}

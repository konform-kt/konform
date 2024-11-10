package io.konform.validation.path

import io.konform.validation.helpers.prepend
import kotlin.reflect.KClass
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

/** Represents a path to a validation. */
public data class ValidationPath(
    val segments: List<PathSegment>,
) {
    public infix operator fun plus(segment: PathSegment): ValidationPath = ValidationPath(segments + segment)

    /** A JSONPath-ish representation of the path. */
    public val pathString: String
        get() = segments.joinToString("") { it.pathString }

    internal fun append(other: ValidationPath): ValidationPath = other.prepend(this)

    internal fun append(pathSegment: PathSegment): ValidationPath = ValidationPath(segments + pathSegment)

    internal fun prepend(other: ValidationPath): ValidationPath =
        when {
            segments.isEmpty() -> other
            other.segments.isEmpty() -> this
            else -> ValidationPath(other.segments + segments)
        }

    internal fun prepend(pathSegment: PathSegment): ValidationPath = ValidationPath(segments.prepend(pathSegment))

    public companion object {
        internal val EMPTY = ValidationPath(emptyList())

        public fun of(pathSegment: PathSegment): ValidationPath = ValidationPath(listOf(pathSegment))

        public fun fromAny(vararg validationPath: Any): ValidationPath =
            ValidationPath(validationPath.map { PathSegment.toPathSegment(it) })
    }
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
    }

    public data class Function(
        val function: KFunction1<*, *>,
    ) : PathSegment {
        override val pathString: String get() = ".${function.name}"
    }

    /** A path for a */
    public data class KCls(
        val kcls: KClass<*>,
    ) : PathSegment {
        override val pathString: String get() = kcls.simpleName ?: "Anonymous"
    }

    /** An index to an array, list, or other iterable. */
    public data class Index(
        val index: Int,
    ) : PathSegment {
        override val pathString: String get() = "[$index]"
    }

    /** The key of a map. */
    public data class MapKey(
        val key: Any?,
    ) : PathSegment {
        override val pathString: String get() = ".$key"
    }

    /** A string provided by the user, usually a field name. */
    public data class ProvidedString(
        val string: String,
    ) : PathSegment {
        override val pathString: String get() = ".$string"
    }

    /** Any non-string value provided by the user. */
    public data class ProvidedValue(
        val value: Any,
    ) : PathSegment {
        override val pathString: String get() = ".$value"
    }
}

public fun KProperty1<*, *>.toPathSegment(): PathSegment.Property = PathSegment.Property(this)

public fun KFunction1<*, *>.toPathSegment(): PathSegment.Function = PathSegment.Function(this)

public fun Map.Entry<*, *>.toPathSegment(): PathSegment.MapKey = PathSegment.MapKey(this.key)

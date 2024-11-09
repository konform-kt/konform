package io.konform.validation.path

import kotlin.reflect.KClass
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

/** Represents a path to a validation. */
public data class ValidationPath(
    val segments: List<PathSegment>,
) {
    public infix operator fun plus(segment: PathSegment): ValidationPath = ValidationPath(segments + segment)

    internal fun prepend(path: ValidationPath): ValidationPath = ValidationPath(path.segments + segments)

    /** A JSONPath-ish representation of the path. */
    public val pathString: String
        get() = segments.joinToString("") { it.pathString }

    public companion object {
        internal val EMPTY = ValidationPath(emptyList())

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
        public fun toPathSegment(pathElement: Any): PathSegment =
            when (pathElement) {
                is KProperty1<*, *> -> pathElement.toPathSegment()
                is KFunction1<*, *> -> pathElement.toPathSegment()
                is Int -> Index(pathElement)
                is Map.Entry<*, *> -> pathElement.toPathSegment()
                is String -> ProvidedString(pathElement)
                else -> ProvidedValue(pathElement)
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
        val kcls: KClass<*>
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

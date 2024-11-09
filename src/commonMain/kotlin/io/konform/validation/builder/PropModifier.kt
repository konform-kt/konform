package io.konform.validation.builder

import io.konform.validation.Validation
import io.konform.validation.path.PathSegment
import io.konform.validation.types.NullableValidation
import io.konform.validation.types.CallableValidation

/** A modifier on the validation of a property */
internal enum class PropModifier {
    /** Indicates that the property is required/not nullable. */
    NonNull,

    /** Indicates that the property is not required/nullable*/
    Optional,

    /** Indicates that even though the property is nullable, is it still required. */
    OptionalRequired,

    ;

    fun <T, R> buildValidation(
        property: (T) -> R,
        pathSegment: PathSegment,
        validation: Validation<R>,
    ): Validation<T> {
        val propValidation = CallableValidation(property, pathSegment, validation)
        if (this == NonNull) return propValidation
        val nullable = NullableValidation(
            required = this == OptionalRequired,
            validation = propValidation,
            pathSegment = pathSegment
        )
        return nullable
    }
}

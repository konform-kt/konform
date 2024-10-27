package io.konform.validation.builder

import io.konform.validation.Validation
import io.konform.validation.internal.NonNullPropertyValidation
import io.konform.validation.internal.OptionalPropertyValidation
import io.konform.validation.internal.RequiredPropertyValidation

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
        propertyName: String,
        validations: Validation<R>,
    ): Validation<T> =
        when (this) {
            NonNull -> NonNullPropertyValidation(property, propertyName, validations)
            Optional -> OptionalPropertyValidation(property, propertyName, validations)
            OptionalRequired -> RequiredPropertyValidation(property, propertyName, validations)
        }
}

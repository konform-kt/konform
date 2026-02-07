package io.konform.validation.validationbuilder

import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.constraints.minimum
import io.konform.validation.path.ValidationPath
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainOnlyError
import kotlin.jvm.JvmInline
import kotlin.test.Test

class PathOverrideTest {
    @JvmInline
    value class ValueClass(
        val integer: Int,
    )

    data class WrapperClass(
        val valueClass: ValueClass,
    )

    @Test
    fun pathOverrideWithValidationPathEmpty() {
        val validation =
            Validation<WrapperClass> {
                WrapperClass::valueClass {
                    ValueClass::integer {
                        path = ValidationPath.EMPTY
                        minimum(1)
                    }
                }
            }

        validation shouldBeValid WrapperClass(ValueClass(1))
        validation shouldBeValid WrapperClass(ValueClass(10))

        // Error path should be .valueClass (not .valueClass.integer)
        (validation shouldBeInvalid WrapperClass(ValueClass(0))) shouldContainOnlyError
            ValidationError.of(WrapperClass::valueClass, "must be at least '1'")
    }

    @Test
    fun pathOverrideDoesNotAffectOtherValidations() {
        data class MultiField(
            val field1: ValueClass,
            val field2: ValueClass,
        )

        val validation =
            Validation<MultiField> {
                MultiField::field1 {
                    ValueClass::integer {
                        path = ValidationPath.EMPTY
                        minimum(1)
                    }
                }
                MultiField::field2 {
                    ValueClass::integer {
                        minimum(1)
                    }
                }
            }

        validation shouldBeValid MultiField(ValueClass(1), ValueClass(1))

        // field1 should have path .field1 (integer suppressed)
        (validation shouldBeInvalid MultiField(ValueClass(0), ValueClass(1))) shouldContainOnlyError
            ValidationError.of(MultiField::field1, "must be at least '1'")

        // field2 should have path .field2.integer (normal behavior)
        (validation shouldBeInvalid MultiField(ValueClass(1), ValueClass(0))) shouldContainOnlyError
            ValidationError(ValidationPath.of(MultiField::field2, ValueClass::integer), "must be at least '1'")
    }

    @Test
    fun pathOverrideWithCustomPath() {
        val validation =
            Validation<WrapperClass> {
                WrapperClass::valueClass {
                    ValueClass::integer {
                        path = ValidationPath.of("customPath")
                        minimum(1)
                    }
                }
            }

        // Error path should be .valueClass.customPath (not .valueClass.integer)
        (validation shouldBeInvalid WrapperClass(ValueClass(0))) shouldContainOnlyError
            ValidationError(ValidationPath.of(WrapperClass::valueClass, "customPath"), "must be at least '1'")
    }

    @Test
    fun pathOverrideOnNestedProperty() {
        data class Level2(
            val value: Int,
        )

        data class Level1(
            val level2: Level2,
        )

        val validation =
            Validation<Level1> {
                Level1::level2 {
                    path = ValidationPath.EMPTY
                    Level2::value {
                        minimum(1)
                    }
                }
            }

        // Error path should be .value (level2 suppressed)
        (validation shouldBeInvalid Level1(Level2(0))) shouldContainOnlyError
            ValidationError(ValidationPath.of(Level2::value), "must be at least '1'")
    }
}

package io.konform.validation

import io.konform.validation.constraints.minimum
import io.konform.validation.path.PathValue
import io.konform.validation.path.PropRef
import io.konform.validation.path.ValidationPath
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainOnlyError
import kotlin.test.Test

class ConfigurablePathTest {
    @JvmInline
    value class WrappedInt(
        val value: Int,
    )

    data class DataWithWrapper(
        val wrapped: WrappedInt,
    )

    @Test
    fun canOverridePathWithEmpty() {
        // Test that setting path = ValidationPath.EMPTY removes the path segment
        val validation =
            Validation<DataWithWrapper> {
                DataWithWrapper::wrapped {
                    path = ValidationPath.EMPTY
                    validate("value", { it.value }) {
                        minimum(1)
                    }
                }
            }

        validation shouldBeValid DataWithWrapper(WrappedInt(5))
        (validation shouldBeInvalid DataWithWrapper(WrappedInt(0))) shouldContainOnlyError
            ValidationError.of(PathValue("value"), "must be at least '1'")
    }

    @Test
    fun canOverridePathWithCustomPath() {
        // Test that setting path to a custom path replaces the default
        val validation =
            Validation<DataWithWrapper> {
                DataWithWrapper::wrapped {
                    path = ValidationPath.of("customPath")
                    validate("value", { it.value }) {
                        minimum(1)
                    }
                }
            }

        validation shouldBeValid DataWithWrapper(WrappedInt(5))
        (validation shouldBeInvalid DataWithWrapper(WrappedInt(0))) shouldContainOnlyError
            ValidationError.of(ValidationPath.of("customPath", "value"), "must be at least '1'")
    }

    @Test
    fun pathWorksWithIfPresent() {
        data class DataWithNullable(
            val wrapped: WrappedInt?,
        )

        val validation =
            Validation<DataWithNullable> {
                DataWithNullable::wrapped ifPresent {
                    path = ValidationPath.EMPTY
                    validate("value", { it.value }) {
                        minimum(1)
                    }
                }
            }

        validation shouldBeValid DataWithNullable(null)
        validation shouldBeValid DataWithNullable(WrappedInt(5))
        (validation shouldBeInvalid DataWithNullable(WrappedInt(0))) shouldContainOnlyError
            ValidationError.of(PathValue("value"), "must be at least '1'")
    }

    @Test
    fun pathWorksWithRequired() {
        data class DataWithNullable(
            val wrapped: WrappedInt?,
        )

        val validation =
            Validation<DataWithNullable> {
                DataWithNullable::wrapped required {
                    path = ValidationPath.EMPTY
                    validate("value", { it.value }) {
                        minimum(1)
                    }
                }
            }

        validation shouldBeValid DataWithNullable(WrappedInt(5))
        // When path is set to EMPTY, the "is required" error also has an empty path
        (validation shouldBeInvalid DataWithNullable(null)) shouldContainOnlyError
            ValidationError.of(ValidationPath.EMPTY, "is required")
        (validation shouldBeInvalid DataWithNullable(WrappedInt(0))) shouldContainOnlyError
            ValidationError.of(PathValue("value"), "must be at least '1'")
    }

    @Test
    fun pathWorksWithOnEach() {
        data class DataWithList(
            val items: List<WrappedInt>,
        )

        val validation =
            Validation<DataWithList> {
                DataWithList::items onEach {
                    path = ValidationPath.EMPTY
                    validate("value", { it.value }) {
                        minimum(1)
                    }
                }
            }

        validation shouldBeValid DataWithList(listOf(WrappedInt(5), WrappedInt(10)))
        // When path is set to EMPTY inside onEach, the list property segment is removed but index is kept
        (validation shouldBeInvalid DataWithList(listOf(WrappedInt(0)))) shouldContainOnlyError
            ValidationError.of(ValidationPath.of(0, "value"), "must be at least '1'")
    }

    @Test
    fun withoutPathOverrideShowsDefaultPath() {
        // Verify that without setting path, the default path is used
        val validation =
            Validation<DataWithWrapper> {
                DataWithWrapper::wrapped {
                    validate("value", { it.value }) {
                        minimum(1)
                    }
                }
            }

        validation shouldBeValid DataWithWrapper(WrappedInt(5))
        (validation shouldBeInvalid DataWithWrapper(WrappedInt(0))) shouldContainOnlyError
            ValidationError.of(
                ValidationPath.of(PropRef(DataWithWrapper::wrapped), "value"),
                "must be at least '1'",
            )
    }
}

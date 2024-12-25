package io.konform.validation.validationbuilder

import io.konform.validation.Validation
import io.konform.validation.constraints.maxLength
import io.konform.validation.constraints.minLength
import kotlin.test.Test

class ValidateAnyTest {
    @Test
    fun validateAny() {
        val validation =
            Validation<String> {
                oneOf(
                    "must be either length 5 or 10",
                    {
                        minLength(5)
                        maxLength(5)
                    },
                    {
                        minLength(10)
                        maxLength(10)
                    },
                )
            }
    }
}

package io.konform.validation

import io.konform.validation.jsonschema.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationResultTest {
    @Test
    fun `Testing the errorsMap function`() {
        val invalidUser = User(
            "Chester",
            "Bennington",
            "chesterLinkinPark",
            43,
            'M'
        )
        val expectedErrorsMap = mapOf(
            "username" to listOf("must have at most 16 characters")
        )
        val validationResult = userValidator(invalidUser)
        assertTrue(validationResult is Invalid)
        val errors = validationResult.errorsMap()
        assertEquals(expectedErrorsMap, errors)
    }
}

data class User(val name: String, val surname: String, val username: String, val age: Int, val gender: Char)
val userValidator = Validation<User> {
    User::name {
        minLength(2)
        maxLength(10)
    }
    User::surname {
        minLength(2)
        maxLength(10)
    }
    User::username {
        minLength(2)
        maxLength(16)
    }
    User::age {
        minimum(18)
        maximum(99)
    }
    User::gender {
        enum('M', 'F', 'H')
    }
}

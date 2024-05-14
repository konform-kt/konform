package io.konform.validation

import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ConcurrencyTest {
    data class User(val name: String)

    val user1 = User("a")
    val user2 = User("invalid")

    private val sleepTimeMs = 300L

    val mustNotBeNamedInvalidValidation =
        Validation<User> { (user) ->
            addConstraint("Cannot be 'invalid'") {
                // Simulate a long expensive blocking operation
                Thread.sleep(sleepTimeMs)
                user.name != "invalid"
            }
        }

    @Test
    fun validationsMustBeThreadSafe() {
        var validation1Holder: ValidationResult<User>? = null
        // Run the first validation concurrently
        thread {
            validation1Holder = mustNotBeNamedInvalidValidation.validate(user1)
        }
        val validation2 = mustNotBeNamedInvalidValidation.validate(user2)
        // Give the thread time to finish
        Thread.sleep(sleepTimeMs + 100)
        val validation1 =
            requireNotNull(validation1Holder) {
                "Validation 1 should have completed by now"
            }
        assertContentEquals(validation1.errors, listOf())
        assertEquals(1, validation2.errors.size)
    }
}

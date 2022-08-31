package io.konform.validation

import io.konform.validation.checks.maxItems
import io.konform.validation.checks.minItems
import io.konform.validation.checks.pattern
import io.konform.validation.errors.maximum
import io.konform.validation.errors.minimum
import io.konform.validation.errors.ValidationError
import io.konform.validation.errors.maxLength
import io.konform.validation.errors.minLength
import io.konform.validation.errors.require
import kotlin.collections.Map.Entry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadmeExampleTest {

    @Test
    fun simpleValidation() {
        data class UserProfile(
            val fullName: String,
            val age: Int?
        )

        val validateUser = Validation<UserProfile, ValidationError> {
            UserProfile::fullName {
                minLength(2)
                maxLength(100)
            }

            UserProfile::age ifPresent {
                minimum(0)
                maximum(150)
            }
        }

        val invalidUser = UserProfile("A", -1)
        val validationResult = validateUser.validate(invalidUser)

        assertTrue(validationResult is Invalid<*>)
        assertEquals(2, validationResult.errors.size)
        assertEquals("must have at least 2 characters", (validationResult.errors.first().error as ValidationError).message)
        assertEquals("must be at least '0'", (validationResult.errors.last().error as ValidationError).message)
    }

    @Test
    fun complexValidation() {
        data class Error(override val message: String) : ValidationError
        data class Person(val name: String, val email: String?, val age: Int)

        data class Event(
            val organizer: Person,
            val attendees: List<Person>,
            val ticketPrices: Map<String, Double?>
        )

        val validateEvent = Validation<Event, ValidationError> {
            Event::organizer {
                // even though the email is nullable you can force it to be set in the validation
                require(Person::email) {
                    pattern("\\w+@bigcorp.com") { Error("Organizers must have a BigCorp email address") }
                }
            }

            // validation on the attendees list
            Event::attendees {
                maxItems(100) {Error("maximum number of attendees is 100")}
            }

            // validation on individual attendees
            Event::attendees onEach {
                Person::name {
                    minLength(2)
                }
                Person::age {
                    minimum(18)
                }
                // Email is optional but if it is set it must be valid
                Person::email ifPresent {
                    pattern("\\w+@\\w+\\.\\w+") { Error("Please provide a valid email address (optional)") }
                }
            }

            // validation on the ticketPrices Map as a whole
            Event::ticketPrices {
                minItems(1) { Error("Provide at least one ticket price") }
            }

            // validations for the individual entries
            Event::ticketPrices onEach {
                // Tickets may be free
                Entry<String, Double?>::value ifPresent {
                    minimum(0.01)
                }
            }
        }

        val validEvent = Event(
            organizer = Person("Organizer", "organizer@bigcorp.com", 30),
            attendees = listOf(
                Person("Visitor", null, 18),
                Person("Journalist", "hello@world.com", 35)
            ),
            ticketPrices = mapOf(
                "diversity-ticket" to null,
                "early-bird" to 200.0,
                "regular" to 400.0
            )
        )

        assertEquals(Valid(validEvent), validateEvent.validate(validEvent))


        val invalidEvent = Event(
            organizer = Person("Organizer", "organizer@smallcorp.com", 30),
            attendees = listOf(
                Person("Youngster", null, 17)
            ),
            ticketPrices = mapOf(
                "we-pay-you" to -100.0
            )
        )

        val result = validateEvent.validate(invalidEvent)
        assertTrue(result is Invalid<*>)
        assertEquals(3, result.errors.size)
//        assertEquals("Attendees must be 18 years or older", validateEvent(invalidEvent)[Event::attendees, 0, Person::age]!![0])
    }

}

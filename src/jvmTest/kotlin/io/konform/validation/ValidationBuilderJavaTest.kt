package io.konform.validation

import io.konform.validation.jsonschema.maxLength
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidationBuilderJavaTest {

    @Test
    fun validateArrays() {
        val validation = Validation<TestSubject> {
            TestSubject::stringArray onEach {
                maxLength(6)
            }
        }

        val result = validation(TestSubject())
        assertEquals(2, countFieldsWithErrors(result))
    }

    @Test
    fun validateIterables() {
        val validation = Validation<TestSubject> {
            TestSubject::stringIterable onEach {
                maxLength(6)
            }
        }

        val result = validation(TestSubject())
        assertEquals(2, countFieldsWithErrors(result))
    }

    @Test
    fun validateMaps() {
        val validation = Validation<TestSubject> {
            TestSubject::stringMap onEach {
                Map.Entry<String, String>::value {
                    maxLength(6)
                }
            }
        }

        val result = validation(TestSubject())
        assertEquals(2, countFieldsWithErrors(result))
    }

    @Test
    fun validateIfPresent() {
        val validationNull = Validation<TestSubject> {
            TestSubject::nullString ifPresent {
                maxLength(2)
            }
        }
        assertTrue(validationNull(TestSubject()) is Valid)

        val validationNonNull = Validation<TestSubject> {
            TestSubject::notNullString ifPresent {
                maxLength(2)
            }
        }
        assertEquals(1, countFieldsWithErrors(validationNonNull(TestSubject())))
    }

    @Test
    fun validateRequired() {
        val validationNull = Validation<TestSubject> {
            TestSubject::nullString required with {
                maxLength(2)
            }
        }
        assertEquals(1, countFieldsWithErrors(validationNull(TestSubject())))

        val validationNonNull = Validation<TestSubject> {
            TestSubject::notNullString required with {
                maxLength(2)
            }
        }
        assertEquals(1, countFieldsWithErrors(validationNonNull(TestSubject())))
    }
}

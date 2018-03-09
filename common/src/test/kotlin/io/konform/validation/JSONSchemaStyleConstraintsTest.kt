package io.konform.validation

import io.konform.validation.JSONSchemaStyleConstraintsTest.TCPPacket.*
import io.konform.validation.jsonschema.const
import io.konform.validation.jsonschema.enum
import io.konform.validation.jsonschema.exclusiveMaximum
import io.konform.validation.jsonschema.exclusiveMinimum
import io.konform.validation.jsonschema.maximum
import io.konform.validation.jsonschema.minimum
import io.konform.validation.jsonschema.multipleOf
import io.konform.validation.jsonschema.type
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JSONSchemaStyleConstraintsTest {

    @Test
    fun typeConstraint() {
        val anyValidation = Validation<Any> { type<String>() }
        assertEquals<ValidationResult<Any>>(
            Valid("This is a string"),
            anyValidation("This is a string"))

        assertEquals(1, countFieldsWithErrors(anyValidation(1)))
        assertEquals(1, countFieldsWithErrors(anyValidation(1.0)))
        assertEquals(1, countFieldsWithErrors(anyValidation(true)))


        val anyNumberValidation = Validation<Any> { type<Int>() }
        assertEquals<ValidationResult<Any>>(Valid(1), anyNumberValidation(1))
        assertEquals(1, countFieldsWithErrors(anyNumberValidation("String")))
        assertEquals(1, countFieldsWithErrors(anyNumberValidation(true)))

        assertEquals("must be of the correct type", anyValidation(1).get()!![0])
        assertEquals("must be of the correct type", anyNumberValidation("String").get()!![0])
    }

    @Test
    fun nullableTypeConstraint() {
        val anyValidation = Validation<Any?> { type<String?>() }
        assertEquals<ValidationResult<Any?>>(
            Valid("This is a string"),
            anyValidation("This is a string"))
        assertEquals<ValidationResult<Any?>>(
            Valid(null),
            anyValidation(null))
    }

    @Test
    fun stringEnumConstraint() {
        val validation = Validation<String> { enum("OK", "CANCEL") }
        assertEquals(Valid("OK"), validation("OK"))
        assertEquals(Valid("CANCEL"), validation("CANCEL"))
        assertEquals(1, countFieldsWithErrors(validation("???")))
        assertEquals(1, countFieldsWithErrors(validation("")))

        assertEquals("must be one of: 'OK', 'CANCEL'", validation("").get()!![0])
    }


    enum class TCPPacket {
        SYN, ACK, SYNACK
    }

    @Test
    fun kotlinEnumConstraint() {
        val partialEnumValidation = Validation<TCPPacket> { enum(SYN, ACK) }
        assertEquals(Valid(SYN), partialEnumValidation(SYN))
        assertEquals(Valid(ACK), partialEnumValidation(ACK))
        assertEquals(1, countFieldsWithErrors(partialEnumValidation(SYNACK)))

        val stringifiedEnumValidation = Validation<String> { enum<TCPPacket>() }
        assertEquals(Valid("SYN"), stringifiedEnumValidation("SYN"))
        assertEquals(Valid("ACK"), stringifiedEnumValidation("ACK"))
        assertEquals(Valid("SYNACK"), stringifiedEnumValidation("SYNACK"))
        assertEquals(1, countFieldsWithErrors(stringifiedEnumValidation("ASDF")))

        assertEquals("must be one of: 'SYN', 'ACK'", partialEnumValidation(SYNACK).get()!![0])
        assertEquals("must be one of: 'SYN', 'ACK', 'SYNACK'", stringifiedEnumValidation("").get()!![0])
    }

    @Test
    fun constConstraint() {
        val validation = Validation<String> { const("Konform") }
        assertEquals(Valid("Konform"), validation("Konform"))
        assertEquals(1, countFieldsWithErrors(validation("")))


        val nullableConstNullValidation = Validation<String?> { const(null) }
        assertEquals<ValidationResult<String?>>(Valid(null), nullableConstNullValidation(null))
        assertEquals(1, countFieldsWithErrors(nullableConstNullValidation("")))
        assertEquals(1, countFieldsWithErrors(nullableConstNullValidation("Konform")))

        val nullableConstValidation = Validation<String?> { const("Konform") }
        assertEquals<ValidationResult<String?>>(Valid("Konform"), nullableConstValidation("Konform"))
        assertEquals(1, countFieldsWithErrors(nullableConstValidation(null)))
        assertEquals(1, countFieldsWithErrors(nullableConstValidation("Konverse")))

        assertEquals("must be 'Konform'", validation("Konverse").get()!![0])
        assertEquals("must be null", nullableConstNullValidation("Konform").get()!![0])
        assertEquals("must be 'Konform'", nullableConstValidation(null).get()!![0])
    }

    @Test
    fun multipleOfConstraint() {
        val validation = Validation<Number> { multipleOf(2.5) }
        assertEquals<ValidationResult<Number>>(Valid(0), validation(0))
        assertEquals<ValidationResult<Number>>(Valid(-2.5), validation(-2.5))
        assertEquals<ValidationResult<Number>>(Valid(2.5), validation(2.5))
        assertEquals<ValidationResult<Number>>(Valid(5), validation(5))
        assertEquals<ValidationResult<Number>>(Valid(25), validation(25))

        assertEquals(1, countFieldsWithErrors(validation(1)))
        assertEquals(1, countFieldsWithErrors(validation(1.0)))
        assertEquals(1, countFieldsWithErrors(validation(-4.0)))
        assertEquals(1, countFieldsWithErrors(validation(25.1)))

        assertFailsWith(IllegalArgumentException::class) { Validation<Number> { multipleOf(0) }}
        assertFailsWith(IllegalArgumentException::class) { Validation<Number> { multipleOf(-1) }}

        assertEquals("must be a multiple of '2.5'", validation(1).get()!![0])
    }

    @Test
    fun maximumConstraint() {
        val validation = Validation<Number> { maximum(10) }

        assertEquals<ValidationResult<Number>>(Valid(Double.NEGATIVE_INFINITY), validation(Double.NEGATIVE_INFINITY))
        assertEquals<ValidationResult<Number>>(Valid(-10), validation(-10))
        assertEquals<ValidationResult<Number>>(Valid(9), validation(9))
        assertEquals<ValidationResult<Number>>(Valid(10), validation(10))
        assertEquals<ValidationResult<Number>>(Valid(10.0), validation(10.0))

        assertEquals(1, countFieldsWithErrors(validation(10.00001)))
        assertEquals(1, countFieldsWithErrors(validation(11)))
        assertEquals(1, countFieldsWithErrors(validation(Double.POSITIVE_INFINITY)))

        assertEquals<ValidationResult<Number>>(Valid(Double.POSITIVE_INFINITY), Validation<Number> { maximum(Double.POSITIVE_INFINITY) } (Double.POSITIVE_INFINITY))

        assertEquals("must be at most '10'", validation(11).get()!![0])
    }

    @Test
    fun exclusiveMaximumConstraint() {
        val validation = Validation<Number> { exclusiveMaximum(10) }

        assertEquals<ValidationResult<Number>>(Valid(Double.NEGATIVE_INFINITY), validation(Double.NEGATIVE_INFINITY))
        assertEquals<ValidationResult<Number>>(Valid(-10), validation(-10))
        assertEquals<ValidationResult<Number>>(Valid(9), validation(9))
        assertEquals<ValidationResult<Number>>(Valid(9.99999999), validation(9.99999999))

        assertEquals(1, countFieldsWithErrors(validation(10)))
        assertEquals(1, countFieldsWithErrors(validation(10.0)))
        assertEquals(1, countFieldsWithErrors(validation(10.00001)))
        assertEquals(1, countFieldsWithErrors(validation(11)))
        assertEquals(1, countFieldsWithErrors(validation(Double.POSITIVE_INFINITY)))
        assertEquals(1, countFieldsWithErrors(Validation<Number> { exclusiveMaximum(Double.POSITIVE_INFINITY) } (Double.POSITIVE_INFINITY)))


        assertEquals("must be less than '10'", validation(11).get()!![0])
    }

    @Test
    fun minimumConstraint() {
        val validation = Validation<Number> { minimum(10) }

        assertEquals<ValidationResult<Number>>(Valid(Double.POSITIVE_INFINITY), validation(Double.POSITIVE_INFINITY))
        assertEquals<ValidationResult<Number>>(Valid(20), validation(20))
        assertEquals<ValidationResult<Number>>(Valid(11), validation(11))
        assertEquals<ValidationResult<Number>>(Valid(10.1), validation(10.1))
        assertEquals<ValidationResult<Number>>(Valid(10.0), validation(10.0))

        assertEquals(1, countFieldsWithErrors(validation(9.99999999999)))
        assertEquals(1, countFieldsWithErrors(validation(8)))
        assertEquals(1, countFieldsWithErrors(validation(Double.NEGATIVE_INFINITY)))

        assertEquals<ValidationResult<Number>>(Valid(Double.NEGATIVE_INFINITY), Validation<Number> { minimum(Double.NEGATIVE_INFINITY) } (Double.NEGATIVE_INFINITY))

        assertEquals("must be at least '10'", validation(9).get()!![0])
    }

    @Test
    fun minimumExclusiveConstraint() {
        val validation = Validation<Number> { exclusiveMinimum(10) }

        assertEquals<ValidationResult<Number>>(Valid(Double.POSITIVE_INFINITY), validation(Double.POSITIVE_INFINITY))
        assertEquals<ValidationResult<Number>>(Valid(20), validation(20))
        assertEquals<ValidationResult<Number>>(Valid(11), validation(11))
        assertEquals<ValidationResult<Number>>(Valid(10.1), validation(10.1))

        assertEquals(1, countFieldsWithErrors(validation(10)))
        assertEquals(1, countFieldsWithErrors(validation(10.0)))
        assertEquals(1, countFieldsWithErrors(validation(9.99999999999)))
        assertEquals(1, countFieldsWithErrors(validation(8)))
        assertEquals(1, countFieldsWithErrors(validation(Double.NEGATIVE_INFINITY)))
        assertEquals(1, countFieldsWithErrors(Validation<Number> { exclusiveMinimum(Double.NEGATIVE_INFINITY) } (Double.NEGATIVE_INFINITY)))


        assertEquals("must be greater than '10'", validation(9).get()!![0])
    }
}

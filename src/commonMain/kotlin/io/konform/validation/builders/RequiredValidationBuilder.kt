package io.konform.validation.builders

import io.konform.validation.ValidationBuilder
import io.konform.validation.types.RequireNotNullValidation
import io.konform.validation.types.RequireNotNullValidation.Companion.DEFAULT_REQUIRED_HINT

/**
 * A [ValidationBuilder] for [RequireNotNullValidation].
 * Allows setting the hint for the validation when the value is null with the following syntax:
 * ```
 * User::name required {
 *   hint = "Please fill in your name"
 *   // other validations on name as normal
 * }
 * ```
 */
public class RequiredValidationBuilder<T : Any> : ValidationBuilder<T>() {
    public var hint: String = DEFAULT_REQUIRED_HINT
    public var userContext: Any? = null

    override fun build(): RequireNotNullValidation<T> {
        val subValidation = super.build()
        return RequireNotNullValidation(hint, subValidation, userContext)
    }

    public companion object {
        public inline fun <T : Any> buildWithNew(block: RequiredValidationBuilder<T>.() -> Unit): RequireNotNullValidation<T> {
            val builder = RequiredValidationBuilder<T>()
            block(builder)
            return builder.build()
        }
    }
}

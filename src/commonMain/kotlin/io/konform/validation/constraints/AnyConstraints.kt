package io.konform.validation.constraints

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public inline fun <reified T> ValidationBuilder<*>.type(): Constraint<*> =
    addConstraint("must be of type '${T::class.simpleName}'") { it is T }

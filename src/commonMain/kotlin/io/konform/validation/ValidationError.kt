package io.konform.validation

public interface ValidationError {
    public val dataPath: String
    public val message: String

    public companion object {
        internal operator fun invoke(
            dataPath: String,
            message: String,
        ): ValidationError = PropertyValidationError(dataPath, message)
    }
}

package com.gabriel.mylibrary.util

enum class ValidationError { REQUIRED, INVALID_NUMBER, NEGATIVE, TOO_LARGE, EXCEEDS_TOTAL }

data class ItemValidation(
    val titleErrorKind: ValidationError? = null,
    val currentErrorKind: ValidationError? = null,
    val totalErrorKind: ValidationError? = null,
) {
    val isValid: Boolean
        get() = titleErrorKind == null && currentErrorKind == null && totalErrorKind == null
    val titleError: String? get() = titleErrorKind?.let { "Please enter a title." }
    val currentError: String? get() = currentErrorKind?.message
    val totalError: String? get() = totalErrorKind?.message
}

private val ValidationError.message: String
    get() = when (this) {
        ValidationError.REQUIRED -> "This field is required."
        ValidationError.INVALID_NUMBER -> "Enter a whole number."
        ValidationError.NEGATIVE -> "Progress cannot be negative."
        ValidationError.TOO_LARGE -> "Enter a number no greater than 2,147,483,647."
        ValidationError.EXCEEDS_TOTAL -> "Current progress cannot exceed the total."
    }

private fun numberError(value: String): ValidationError? {
    val number = value.trim()
    if (number.isEmpty()) return ValidationError.REQUIRED
    if (!number.matches(Regex("[+-]?[0-9]+"))) return ValidationError.INVALID_NUMBER
    if (number.startsWith("-") && number.drop(1).any { it != '0' }) return ValidationError.NEGATIVE
    if (number.toIntOrNull() == null) return ValidationError.TOO_LARGE
    return null
}

fun validateItem(title: String, currentProgress: String, totalCount: String): ItemValidation {
    var currentError = numberError(currentProgress)
    val totalError = numberError(totalCount)
    if (currentError == null && totalError == null && currentProgress.trim().toInt() > totalCount.trim().toInt()) {
        currentError = ValidationError.EXCEEDS_TOTAL
    }
    return ItemValidation(
        titleErrorKind = if (title.isBlank()) ValidationError.REQUIRED else null,
        currentErrorKind = currentError,
        totalErrorKind = totalError,
    )
}

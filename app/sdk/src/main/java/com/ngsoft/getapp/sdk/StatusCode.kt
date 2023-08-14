package com.ngsoft.getapp.sdk

enum class StatusCode(val code: Int) {
    // Success.
    SUCCESS(200), REQUEST_ID_NOT_FOUND(4001),  // Errors.
    INVALID_REQUEST(400), UNAUTHORIZED(401), FORBIDDEN(403), NOT_FOUND(404), INTERNAL_SERVER_ERROR(
        500
    )

}
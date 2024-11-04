package com.ngsoft.getapp.sdk.annotations

/**
 * Indicates that a function requires a valid IMEI to execute.
 * If the SDK instance does not have a IMEI, invoking this function will
 * throw a `MissingIMEIException`.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresIMEI

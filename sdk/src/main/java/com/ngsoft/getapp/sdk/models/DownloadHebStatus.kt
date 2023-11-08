package com.ngsoft.getapp.sdk.models

enum class DownloadHebStatus(val value: String) {
    REQUEST_SENT("בקשה נשלחה"),
    REQUEST_IN_PROCESS("בקשה בהפקה"),
    DOWNLOAD("בהורדה"),
    DONE("הסתיים"),
    PAUSED("מושהה"),
    CANCELED("בוטל"),
    FAILED("נכשל"),
    BAD_CONNECTION("קליטה גרועה"),
    NO_CONNECTION("אין רשת"),
    IN_VERIFICATION("באימות"),
    FAILDE_VARIFICATION("נכשל באימות"),
}
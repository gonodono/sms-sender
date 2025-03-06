package com.gonodono.smssender.work

data class WorkerState(
    val isSending: Boolean,
    val isCancelled: Boolean,
    val lastError: String?
)
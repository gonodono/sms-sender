package com.gonodono.smssender.repository

import com.gonodono.smssender.internal.nameAndMessage

sealed interface FatalError {

    data object None : FatalError

    data class SmsError(val errorCode: Int) : FatalError {
        override fun toString(): String = errorCodeToString(errorCode)
    }

    data class RuntimeError(val throwable: Throwable) : FatalError {
        override fun toString(): String = throwable.nameAndMessage()
    }
}

fun FatalError(errorCode: Int) = FatalError.SmsError(errorCode)

fun FatalError(throwable: Throwable) = FatalError.RuntimeError(throwable)

fun FatalError.isError() = this != FatalError.None
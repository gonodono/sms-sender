package com.gonodono.smssender.repository

sealed interface SendResult {

    data object Completed : SendResult

    data object Cancelled : SendResult

    data class Error(val error: FatalError) : SendResult {
        override fun toString(): String = error.toString()
    }
}
package de.sirywell.handlehints

import com.intellij.openapi.diagnostic.Logger

private val remembered = mutableSetOf<String>()
fun Logger.warnOnce(message: String) {
    if (remembered.add(message)) {
        this.warn(message)
    }
}
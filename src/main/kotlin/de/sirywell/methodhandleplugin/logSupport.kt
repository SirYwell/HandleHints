package de.sirywell.methodhandleplugin

import com.intellij.openapi.diagnostic.Logger

private val remembered = mutableSetOf<String>()
fun Logger.warnOnce(message: String) {
    if (remembered.add(message)) {
        this.warn(message)
    }
}
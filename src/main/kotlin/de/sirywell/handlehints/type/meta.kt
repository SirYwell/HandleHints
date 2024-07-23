package de.sirywell.handlehints.type

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class TypeInfo(val topType: KClass<out TopTypeLatticeElement<*>>)
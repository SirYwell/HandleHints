package de.sirywell.methodhandleplugin

// see VarHandle.AccessType
enum class AccessType {
    GET,
    SET,
    COMPARE_AND_SET,
    COMPARE_AND_EXCHANGE,
    GET_AND_UPDATE
}

fun accessTypeForAccessModeName(name: String): AccessType? {
    return when (name) {
        "GET",
        "GET_ACQUIRE",
        "GET_OPAQUE",
        "GET_VOLATILE" -> AccessType.GET

        "SET",
        "SET_RELEASE",
        "SET_OPAQUE",
        "SET_VOLATILE" -> AccessType.SET

        "COMPARE_AND_SET",
        "WEAK_COMPARE_AND_SET_PLAIN",
        "WEAK_COMPARE_AND_SET",
        "WEAK_COMPARE_AND_SET_ACQUIRE",
        "WEAK_COMPARE_AND_SET_RELEASE" -> AccessType.COMPARE_AND_SET

        "COMPARE_AND_EXCHANGE",
        "COMPARE_AND_EXCHANGE_ACQUIRE",
        "COMPARE_AND_EXCHANGE_RELEASE" -> AccessType.COMPARE_AND_EXCHANGE

        "GET_AND_SET",
        "GET_AND_SET_ACQUIRE",
        "GET_AND_SET_RELEASE",
        "GET_AND_ADD",
        "GET_AND_ADD_ACQUIRE",
        "GET_AND_ADD_RELEASE",
        "GET_AND_BITWISE_OR",
        "GET_AND_BITWISE_OR_RELEASE",
        "GET_AND_BITWISE_OR_ACQUIRE",
        "GET_AND_BITWISE_AND",
        "GET_AND_BITWISE_AND_RELEASE",
        "GET_AND_BITWISE_AND_ACQUIRE",
        "GET_AND_BITWISE_XOR",
        "GET_AND_BITWISE_XOR_RELEASE",
        "GET_AND_BITWISE_XOR_ACQUIRE" -> AccessType.GET_AND_UPDATE
        else -> null
    }
}
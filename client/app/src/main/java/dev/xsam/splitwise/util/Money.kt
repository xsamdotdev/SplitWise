package dev.xsam.splitwise.util

import kotlin.math.abs

fun formatMoney(cents: Int): String {
    val prefix = if (cents < 0) "-$" else "$"
    val value = abs(cents)
    return "$prefix${value / 100}.${(value % 100).toString().padStart(2, '0')}"
}

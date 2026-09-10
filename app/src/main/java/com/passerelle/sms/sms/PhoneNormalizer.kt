package com.passerelle.sms.sms

object PhoneNormalizer {
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        val leadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return null

        val compact = if (leadingPlus) "+$digitsOnly" else digitsOnly
        val number = when {
            compact.startsWith("00") -> "+" + compact.drop(2)
            !compact.startsWith("+") && compact.startsWith("0") && compact.length == 10 ->
                "+33" + compact.drop(1)
            else -> compact
        }

        val digits = number.filter { it.isDigit() }
        if (digits.length !in 8..15) return null
        return number
    }
}

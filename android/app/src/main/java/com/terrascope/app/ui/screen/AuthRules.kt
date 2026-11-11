package com.terrascope.app.ui.screen

/** Reglas de correo y contraseña para REGISTRARSE. Deben coincidir con backend/src/lib/credentialPolicy.js. */
object AuthRules {
    const val EMAIL_MAX = 80
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX = 20
    const val NAME_MAX = 80

    private val DOMAINS = setOf(
        "gmail.com", "googlemail.com",
        "outlook.com", "outlook.es", "hotmail.com", "hotmail.es", "hotmail.com.mx", "live.com", "live.com.mx", "msn.com",
        "yahoo.com", "yahoo.com.mx", "yahoo.es", "ymail.com",
        "icloud.com", "me.com", "mac.com",
        "aol.com", "proton.me", "protonmail.com", "zoho.com"
    )
    private val SUFFIXES = listOf(".edu", ".edu.mx")
    private val EMAIL_RE = Regex("^[a-z0-9._%+-]{1,64}@([a-z0-9-]+\\.)+[a-z]{2,}$")

    /** Mensaje de error o null si el correo es válido (o está vacío: aún no se escribe nada). */
    fun emailError(raw: String): String? {
        val email = raw.trim().lowercase()
        if (email.isEmpty()) return null
        if (!email.contains("@")) return "Falta el @ (ej. nombre@gmail.com)"
        if (!EMAIL_RE.matches(email)) return "Escribe un correo válido (ej. nombre@gmail.com)"
        val domain = email.substringAfter("@")
        if (domain !in DOMAINS && SUFFIXES.none { domain.endsWith(it) }) {
            return "Usa un correo reconocido: Gmail, Outlook, Hotmail, Yahoo, iCloud…"
        }
        return null
    }

    fun emailValid(raw: String) = raw.isNotBlank() && emailError(raw) == null

    data class PasswordChecks(val length: Boolean, val upper: Boolean, val digit: Boolean) {
        val ok get() = length && upper && digit
    }

    fun passwordChecks(p: String) = PasswordChecks(
        length = p.length in PASSWORD_MIN..PASSWORD_MAX,
        upper = p.any { it.isUpperCase() },
        digit = p.any { it in '0'..'9' }
    )
}

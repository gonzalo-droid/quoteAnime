package com.gondroid.quoteanime.presentation.navigation

/**
 * Percent-encodes [value] for use as a route argument, so an id with `&`, `?`, `#`, `/` or `%`
 * can't break the route: `home?quoteId=a&b` would otherwise read as `quoteId=a` plus a stray
 * `b` argument.
 *
 * Same output as `android.net.Uri.encode` (UTF-8, everything but letters, digits and `_-!.~'()*`
 * escaped), written in plain Kotlin so it runs in JVM unit tests, where `Uri` is a stub.
 *
 * **Reading side:** Navigation decodes the argument when it matches the route (query values
 * through `Uri.getQueryParameters`, path segments through `NavUriUtils.decode`), so the
 * destination's `SavedStateHandle` already holds the original value. Decoding it a second time
 * would corrupt any id containing `%`.
 */
fun encodeRouteArg(value: String): String {
    val out = StringBuilder(value.length)
    for (byte in value.toByteArray(Charsets.UTF_8)) {
        val c = (byte.toInt() and 0xFF).toChar()
        if (c.isAsciiLetterOrDigit() || c in UNESCAPED) {
            out.append(c)
        } else {
            out.append('%').append(HEX[(byte.toInt() shr 4) and 0x0F]).append(HEX[byte.toInt() and 0x0F])
        }
    }
    return out.toString()
}

private fun Char.isAsciiLetterOrDigit() = this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

private const val UNESCAPED = "_-!.~'()*"
private const val HEX = "0123456789ABCDEF"

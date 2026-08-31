// port-lint: source url/src/form_urlencoded
package io.github.kotlinmania.url

internal fun parseFormUrlencoded(query: String): List<Pair<String, String>> {
    if (query.isEmpty()) return emptyList()
    return query.split('&').map { pair ->
        val eqIdx = pair.indexOf('=')
        if (eqIdx >= 0) {
            decodeFormUrlencoded(pair.substring(0, eqIdx)) to
                decodeFormUrlencoded(pair.substring(eqIdx + 1))
        } else {
            decodeFormUrlencoded(pair) to ""
        }
    }
}

internal fun encodeFormUrlencoded(s: String): String {
    val sb = StringBuilder(s.length)
    for (c in s) {
        when {
            c == ' ' -> sb.append('+')
            c.isLetterOrDigit() || c == '-' || c == '_' || c == '.' || c == '~' -> sb.append(c)
            c.code < 128 ->
                sb.append('%').append(
                    c.code
                        .toString(16)
                        .uppercase()
                        .padStart(2, '0'),
                )
            else -> {
                val bytes = c.toString().encodeToByteArray()
                for (b in bytes) {
                    sb.append('%').append((b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
                }
            }
        }
    }
    return sb.toString()
}

internal fun decodeFormUrlencoded(s: String): String {
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == '+' -> bytes.add(' '.code.toByte())
            c == '%' && i + 2 < s.length -> {
                val hex = s.substring(i + 1, i + 3)
                val byteVal = hex.toIntOrNull(16)
                if (byteVal != null) {
                    bytes.add(byteVal.toByte())
                    i += 2
                } else {
                    bytes.addAll(c.toString().encodeToByteArray().toList())
                }
            }
            else -> bytes.addAll(c.toString().encodeToByteArray().toList())
        }
        i++
    }
    return bytes.toByteArray().decodeToString()
}

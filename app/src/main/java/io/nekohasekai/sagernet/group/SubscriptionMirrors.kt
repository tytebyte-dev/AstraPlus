package io.nekohasekai.sagernet.group

/**
 * A subscription link may list several mirrors of the same list separated by `|`
 * (the `|` character is never valid in a raw URL). Mirrors are tried in order.
 */
object SubscriptionMirrors {
    private const val SEPARATOR = '|'

    fun candidates(link: String): List<String> {
        if (link.startsWith("content://")) return listOf(link)
        return link.split(SEPARATOR).map(String::trim).filter(String::isNotEmpty)
    }

    fun join(mirrors: List<String>): String = mirrors.joinToString(SEPARATOR.toString())
}

package io.nekohasekai.sagernet.group

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSubscriptionsTest {

    @Test
    fun bundledListHasUniqueNamedEntriesWithHttpsMirrors() {
        val entries = DefaultSubscriptions.entries
        assertEquals(25, entries.size)
        assertEquals(entries.size, entries.map { it.name }.toSet().size)
        assertTrue(entries.all { it.name.isNotBlank() && it.mirrors.isNotEmpty() })
        assertTrue(entries.all { entry -> entry.mirrors.all { it.startsWith("https://") && '|' !in it } })
    }

    @Test
    fun mirrorsAreSplitAndJoinedInOrder() {
        val link = "https://a.example/x| https://b.example/x |"
        assertEquals(listOf("https://a.example/x", "https://b.example/x"), SubscriptionMirrors.candidates(link))
        assertEquals("https://a.example/x|https://b.example/x", SubscriptionMirrors.join(SubscriptionMirrors.candidates(link)))
    }

    @Test
    fun contentLinksAreNeverSplit() {
        assertEquals(listOf("content://x|y"), SubscriptionMirrors.candidates("content://x|y"))
    }
}

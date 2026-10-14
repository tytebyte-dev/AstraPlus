package io.nekohasekai.sagernet.database

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileBatchRenamerTest {

    @Test
    fun numbersEveryServerInOrder() {
        assertEquals(
            listOf("Server 1", "Server 2", "Server 3"),
            ProfileBatchRenamer.plan(listOf("🇩🇪 Germany", "俄罗斯联邦 t.me/x", "1.2.3.4")),
        )
    }

    @Test
    fun existingNumberIsKeptAndSkipped() {
        assertEquals(
            listOf("Server 1", "Server 2", "Server 3"),
            ProfileBatchRenamer.plan(listOf("a", "Server 2", "b")),
        )
        assertEquals(
            listOf("Server 1", "Server 2", "Server 4", "Server 3"),
            ProfileBatchRenamer.plan(listOf("a", "b", "c", "Server 3")),
        )
    }

    @Test
    fun duplicateNumberedNamesAreRenumbered() {
        assertEquals(
            listOf("Server 1", "Server 2", "Server 3"),
            ProfileBatchRenamer.plan(listOf("Server 1", "Server 1", "Server 1")),
        )
    }
}

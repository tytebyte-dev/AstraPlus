package io.nekohasekai.sagernet.database

import io.nekohasekai.sagernet.ktx.Logs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.yield

/** Renames every server of a group to "Server 1", "Server 2", ... keeping already numbered ones. */
object ProfileBatchRenamer {

    const val PREFIX = "Server "
    private const val CHUNK_SIZE = 20

    data class Progress(val running: Boolean = false, val done: Int = 0, val total: Int = 0)

    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()

    private val numbered = Regex("^" + Regex.escape(PREFIX) + "([1-9][0-9]{0,8})$")

    /**
     * New names for [names], in order. A name that is already exactly "Server N" (first occurrence
     * of that N) is kept and its number is reserved; every other server takes the next free number.
     */
    fun plan(names: List<String>): List<String> {
        val reserved = HashSet<Int>()
        val keep = BooleanArray(names.size)
        names.forEachIndexed { index, name ->
            val number = numbered.matchEntire(name)?.groupValues?.get(1)?.toIntOrNull()
            if (number != null && reserved.add(number)) keep[index] = true
        }
        var next = 1
        return names.mapIndexed { index, name ->
            if (keep[index]) {
                name
            } else {
                while (next in reserved) next++
                PREFIX + next++
            }
        }
    }

    /** Marks a rename as running (so the modal can open at once); false if one is already running. */
    @Synchronized
    fun tryStart(): Boolean {
        if (_progress.value.running) return false
        _progress.value = Progress(running = true)
        return true
    }

    /** Runs the rename for [groupId]; call only after [tryStart] returned true. */
    suspend fun rename(groupId: Long) {
        try {
            val profiles = AppData.profiles.getByGroup(groupId)
            val names = plan(profiles.map { it.displayName() })
            _progress.update { it.copy(total = profiles.size) }
            profiles.chunked(CHUNK_SIZE).forEachIndexed { chunkIndex, chunk ->
                val changed = ArrayList<ProxyEntity>()
                chunk.forEachIndexed { i, profile ->
                    val newName = names[chunkIndex * CHUNK_SIZE + i]
                    val bean = profile.requireBean()
                    if (bean.name != newName) {
                        bean.name = newName
                        changed += profile
                    }
                }
                if (changed.isNotEmpty()) {
                    runCatching { ProfileManager.updateProfile(changed) }.onFailure(Logs::w)
                }
                _progress.update { it.copy(done = it.done + chunk.size) }
                yield()
            }
        } finally {
            _progress.value = Progress()
        }
    }
}

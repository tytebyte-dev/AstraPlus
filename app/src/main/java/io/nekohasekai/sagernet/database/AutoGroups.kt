package io.nekohasekai.sagernet.database

import io.nekohasekai.sagernet.AUTO_COUNTRY_SOURCE
import io.nekohasekai.sagernet.GroupOrder
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.aidl.TrafficData
import io.nekohasekai.sagernet.fmt.internal.ProxySetBean
import io.nekohasekai.sagernet.ktx.Logs
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ktx.applyDefaultValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Maintains generated "Auto-<group>" entries (sing-box urltest with automatic failover)
 * and the aggregate "All servers" group.
 *
 * Auto entries are regular ProxyEntity rows marked with [AUTO_COUNTRY_SOURCE]; DAO queries
 * used by subscription sync / group listings hide them, so they are never touched by updates.
 */
object AutoGroups {

    const val MIN_SERVERS = 5
    const val FLAG_COUNTRY = "ZZ"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val trigger = Channel<Unit>(Channel.CONFLATED)
    private val mutex = Mutex()
    private var started = false

    fun ProxyEntity.isAutoEntry(): Boolean = countrySource == AUTO_COUNTRY_SOURCE

    @Synchronized
    fun start() {
        if (started) return
        started = true
        ProfileManager.addListener(object : ProfileManager.Listener {
            override suspend fun onAdd(profile: ProxyEntity) = requestSync()
            override suspend fun onUpdated(data: List<TrafficData>) = Unit
            override suspend fun onUpdated(profile: ProxyEntity, noTraffic: Boolean) = Unit
            override suspend fun onRemoved(groupId: Long, profileId: Long) = requestSync()
        })
        GroupManager.addListener(object : GroupManager.Listener {
            override suspend fun groupAdd(group: ProxyGroup) = requestSync()
            override suspend fun groupUpdated(group: ProxyGroup) = requestSync()
            override suspend fun groupRemoved(groupId: Long) = requestSync()
            override suspend fun groupUpdated(groupId: Long) = Unit
            override suspend fun groupReloaded(groupId: Long, reason: GroupManager.ReloadReason) =
                requestSync()

            override suspend fun groupProfileCountChanged(groupId: Long) = requestSync()
        })
        scope.launch {
            runCatching { io.nekohasekai.sagernet.group.DefaultSubscriptions.stripLegacyNamePrefix() }
            for (ignored in trigger) {
                delay(1500)
                trigger.tryReceive()
                runCatching { sync() }.onFailure { Logs.w(it) }
            }
        }
        requestSync()
    }

    fun requestSync() {
        trigger.trySend(Unit)
    }

    /** Profiles to show in the group tab: generated auto entries first, then the servers. */
    fun loadGroupProfiles(group: ProxyGroup): List<ProxyEntity> {
        val auto = if (group.type == GroupType.ALL) {
            AppData.profiles.getAllAuto().filter {
                (it.requireBean() as? ProxySetBean)?.type == ProxySetBean.TYPE_ALL
            }
        } else {
            AppData.profiles.getAutoByGroup(group.id)
        }
        val manual = if (group.type == GroupType.ALL) {
            AppData.profiles.getAllManual()
        } else {
            AppData.profiles.getByGroup(group.id)
        }
        return auto + manual
    }

    suspend fun sync() = mutex.withLock {
        repairMarkers()
        val groups = AppData.groups.allGroups()
        val regular = groups.filter { it.type != GroupType.ALL }
        val changedGroups = linkedSetOf<Long>()

        for (group in regular) {
            val count = AppData.profiles.countByGroup(group.id)
            val changed = ensureAuto(
                existing = AppData.profiles.getAutoByGroup(group.id),
                wanted = count >= MIN_SERVERS,
                groupId = group.id,
                type = ProxySetBean.TYPE_GROUP,
                targetGroupId = group.id,
                title = app.getString(R.string.auto_server_name, group.displayName()),
            )
            if (changed) changedGroups += group.id
        }

        // The aggregate "All servers" group was removed: clean up leftovers from earlier builds.
        groups.filter { it.type == GroupType.ALL }.forEach { stale ->
            AppData.transactions.run {
                AppData.groups.deleteById(stale.id)
                AppData.profiles.deleteByGroup(stale.id)
            }
            if (DataStore.selectedGroup == stale.id) {
                DataStore.selectedGroup = regular.firstOrNull()?.id ?: 0L
            }
            GroupManager.iterator { groupRemoved(stale.id) }
        }
        if (regular.isEmpty()) {
            val group = ProxyGroup(ungrouped = true)
            group.id = AppData.groups.createGroup(group)
            DataStore.selectedGroup = group.id
            GroupManager.iterator { groupAdd(group) }
        }

        for (id in changedGroups) GroupManager.postReload(id)
    }

    private var repaired = false

    /** Auto entries whose marker was overwritten by older builds are re-marked (or dropped if duplicated). */
    private fun repairMarkers() {
        if (repaired) return
        repaired = true
        val broken = AppData.profiles.getAll().filter {
            it.countrySource != AUTO_COUNTRY_SOURCE &&
                it.type == ProxyEntity.TYPE_PROXY_SET &&
                (it.requireBean() as? ProxySetBean)?.auto == true
        }
        if (broken.isEmpty()) return
        val have = AppData.profiles.getAllAuto().map { it.groupId }.toHashSet()
        for (entity in broken) {
            if (entity.groupId in have) {
                AppData.profiles.deleteProxy(entity)
            } else {
                entity.countrySource = AUTO_COUNTRY_SOURCE
                entity.countryCode = FLAG_COUNTRY
                AppData.profiles.updateProxy(entity)
                have += entity.groupId
            }
        }
    }

    /** @return true when something changed. */
    private fun ensureAuto(
        existing: List<ProxyEntity>,
        wanted: Boolean,
        groupId: Long,
        type: Int,
        targetGroupId: Long,
        title: String,
    ): Boolean {
        if (!wanted) {
            if (existing.isEmpty()) return false
            existing.forEach {
                AppData.profiles.deleteProxy(it)
                if (DataStore.selectedProxy == it.id) DataStore.selectedProxy = 0L
            }
            return true
        }
        val keep = existing.firstOrNull()
        if (keep == null) {
            val bean = ProxySetBean().apply {
                auto = true
                mode = ProxySetBean.MODE_URL_TEST
                this.type = type
                this.groupId = targetGroupId
                name = title
            }
            bean.applyDefaultValues()
            val entity = ProxyEntity(groupId = groupId).apply {
                id = 0
                putBean(bean)
                userOrder = 0
                countryCode = FLAG_COUNTRY
                countrySource = AUTO_COUNTRY_SOURCE
            }
            entity.id = AppData.profiles.addProxy(entity)
            return true
        }
        var changed = false
        if (existing.size > 1) {
            existing.drop(1).forEach { AppData.profiles.deleteProxy(it) }
            changed = true
        }
        val bean = keep.requireBean() as? ProxySetBean ?: return changed
        if (bean.name != title || bean.groupId != targetGroupId || !bean.auto) {
            bean.name = title
            bean.groupId = targetGroupId
            bean.auto = true
            keep.putBean(bean)
            AppData.profiles.updateProxy(keep)
            changed = true
        }
        return changed
    }
}

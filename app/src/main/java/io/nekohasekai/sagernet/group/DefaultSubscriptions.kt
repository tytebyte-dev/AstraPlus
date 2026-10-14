package io.nekohasekai.sagernet.group

import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.database.AppData
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SubscriptionBean
import io.nekohasekai.sagernet.ktx.Logs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Subscriptions bundled with the app. On a fresh install they are created as subscription groups
 * and fetched right away. Every entry lists mirrors of one list; they are tried in order.
 */
object DefaultSubscriptions {

    data class Entry(val name: String, val mirrors: List<String>)

    private const val UPDATE_PARALLELISM = 3
    private const val AUTO_UPDATE_MINUTES = 1440

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val entries: List<Entry> = listOf(
        Entry(
            "🌀 CYBERPORTAL | White List · LTE",
            listOf(
                "https://cdn.jsdelivr.net/gh/hussaroff/lte-universal-checked@main/whitelist.txt",
                "https://fastly.jsdelivr.net/gh/hussaroff/lte-universal-checked@main/whitelist.txt",
                "https://raw.githubusercontent.com/hussaroff/lte-universal-checked/main/whitelist.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Проверенные",
            listOf(
                "https://sub.vlessfo.ru/vlessforu/working_configs.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List",
            listOf(
                "https://hub.mos.ru/zieng2/wl/raw/main/list_universal.txt",
                "https://gitverse.ru/api/repos/zieng2/wl/raw/branch/master/list_universal.txt",
                "https://raw.githubusercontent.com/zieng2/wl/main/vless_universal.txt",
                "https://codeberg.org/zieng2/wl/raw/branch/main/vless_universal.txt",
                "https://gitlab.com/zieng2/wl/raw/main/vless_universal.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List 2",
            listOf(
                "https://gitlab.com/igareck/vpn-configs-for-russia/raw/main/Vless-Reality-White-Lists-Rus-Mobile.txt",
                "https://codeberg.org/igareck/vpn-configs-for-russia/raw/branch/main/Vless-Reality-White-Lists-Rus-Mobile.txt",
                "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/main/Vless-Reality-White-Lists-Rus-Mobile.txt",
                "https://raw.githack.com/igareck/vpn-configs-for-russia/main/Vless-Reality-White-Lists-Rus-Mobile.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List · LTE 2",
            listOf(
                "https://raw.githubusercontent.com/Maskkost93/kizyak-vpn-4.0/refs/heads/main/kizyakbeta7.txt",
                "https://raw.githubusercontent.com/Maskkost93/kizyak-vpn-4.0/main/kizyakbeta7.txt",
                "https://fastly.jsdelivr.net/gh/Maskkost93/kizyak-vpn-4.0@main/kizyakbeta7.txt",
                "https://cdn.jsdelivr.net/gh/Maskkost93/kizyak-vpn-4.0@main/kizyakbeta7.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List",
            listOf(
                "https://raw.githubusercontent.com/Maskkost93/kizyak-vpn-4.0/refs/heads/main/kizyakbeta6BL.txt",
                "https://raw.githubusercontent.com/Maskkost93/kizyak-vpn-4.0/main/kizyakbeta6BL.txt",
                "https://fastly.jsdelivr.net/gh/Maskkost93/kizyak-vpn-4.0@main/kizyakbeta6BL.txt",
                "https://cdn.jsdelivr.net/gh/Maskkost93/kizyak-vpn-4.0@main/kizyakbeta6BL.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List 3",
            listOf(
                "https://etoneya.su/whitelist",
                "https://etoneya.best/whitelist",
                "https://alley.serv00.net/whitelist",
                "https://internet-tenshi.kangel.tech/whitelist",
                "https://whitelist.etoneya.baby",
                "https://etoskam.ru/whitelist",
                "https://xn--e1apcp8cq.xn--p1ai/whitelist",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List 2",
            listOf(
                "https://etoneya.su/other",
                "https://etoneya.best/other",
                "https://alley.serv00.net/other",
                "https://internet-tenshi.kangel.tech/other",
                "https://blacklist.etoneya.baby",
                "https://etoskam.ru/other",
                "https://xn--e1apcp8cq.xn--p1ai/other",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List 4",
            listOf(
                "https://raw.githubusercontent.com/NullCoreDeveloper/cyberportal-mirror/master/cp-035.txt",
                "https://warp-gen.cyb-portal.org/CP-035",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List 3",
            listOf(
                "https://raw.githubusercontent.com/NullCoreDeveloper/cyberportal-mirror/master/cp-001.txt",
                "https://warp-gen.cyb-portal.org/CP-001",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List · CIDR",
            listOf(
                "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/main/WHITE-CIDR-RU-all.txt",
                "https://gitlab.com/igareck/vpn-configs-for-russia/raw/main/WHITE-CIDR-RU-all.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List 4",
            listOf(
                "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/main/BLACK_VLESS_RUS.txt",
                "https://gitlab.com/igareck/vpn-configs-for-russia/raw/main/BLACK_VLESS_RUS.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List 5",
            listOf(
                "https://gitlab.com/igareck/vpn-configs-for-russia/raw/main/BLACK_VLESS_RUS_mobile.txt",
                "https://codeberg.org/igareck/vpn-configs-for-russia/raw/branch/main/BLACK_VLESS_RUS_mobile.txt",
                "https://raw.githubusercontent.com/igareck/vpn-configs-for-russia/main/BLACK_VLESS_RUS_mobile.txt",
                "https://raw.githack.com/igareck/vpn-configs-for-russia/main/BLACK_VLESS_RUS_mobile.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List · LTE 3",
            listOf(
                "https://raw.githubusercontent.com/Maskkost93/kizyak-vpn-4.0/refs/heads/main/kizyakbeta6.txt",
                "https://raw.githubusercontent.com/Maskkost93/kizyak-vpn-4.0/main/kizyakbeta6.txt",
                "https://fastly.jsdelivr.net/gh/Maskkost93/kizyak-vpn-4.0@main/kizyakbeta6.txt",
                "https://cdn.jsdelivr.net/gh/Maskkost93/kizyak-vpn-4.0@main/kizyakbeta6.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List 5",
            listOf(
                "https://raw.githubusercontent.com/RKPchannel/RKP_bypass_configs/main/whitelist.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List 6",
            listOf(
                "https://hub.mos.ru/rkp/sub-roskompozor",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List · VLESS",
            listOf(
                "https://raw.githubusercontent.com/NullCoreDeveloper/cyberportal-mirror/master/cp-002.txt",
                "https://warp-gen.cyb-portal.org/CP-002",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List · CIDR 2",
            listOf(
                "https://raw.githubusercontent.com/NullCoreDeveloper/cyberportal-mirror/master/cp-006.txt",
                "https://warp-gen.cyb-portal.org/CP-006",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List · SNI",
            listOf(
                "https://raw.githubusercontent.com/NullCoreDeveloper/cyberportal-mirror/master/cp-008.txt",
                "https://warp-gen.cyb-portal.org/CP-008",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List · Keys",
            listOf(
                "https://raw.githubusercontent.com/NullCoreDeveloper/cyberportal-mirror/master/cp-042.txt",
                "https://warp-gen.cyb-portal.org/CP-042",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | White List 6",
            listOf(
                "https://raw.githubusercontent.com/AirLinkVPN1/AirLinkVPN/refs/heads/main/rkn_white_list",
                "https://raw.githubusercontent.com/AirLinkVPN1/AirLinkVPN/main/rkn_white_list",
                "https://fastly.jsdelivr.net/gh/AirLinkVPN1/AirLinkVPN@main/rkn_white_list",
                "https://cdn.jsdelivr.net/gh/AirLinkVPN1/AirLinkVPN@main/rkn_white_list",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List 7",
            listOf(
                "https://raw.githubusercontent.com/flaafix/AetrisVPN/main/AetrisVPN.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List 8",
            listOf(
                "https://raw.githubusercontent.com/hiztin/VLESS-PO-GRIBI/main/deploy/subscriptions/25.txt",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List · Gemini",
            listOf(
                "https://etoneya.su/gemini",
                "https://etoneya.best/gemini",
                "https://alley.serv00.net/gemini",
                "https://internet-tenshi.kangel.tech/gemini",
                "https://gemini.etoneya.baby",
                "https://etoskam.ru/gemini",
                "https://xn--e1apcp8cq.xn--p1ai/gemini",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List · YouTube",
            listOf(
                "https://etoneya.su/youtube",
                "https://etoneya.best/youtube",
                "https://alley.serv00.net/youtube",
                "https://internet-tenshi.kangel.tech/youtube",
                "https://yt.etoneya.baby",
                "https://etoskam.ru/youtube",
                "https://xn--e1apcp8cq.xn--p1ai/youtube",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List · YouTube Music",
            listOf(
                "https://etoneya.su/ytm",
                "https://etoneya.best/ytm",
                "https://alley.serv00.net/ytm",
                "https://internet-tenshi.kangel.tech/ytm",
                "https://ytm.etoneya.baby",
                "https://etoskam.ru/ytm",
                "https://xn--e1apcp8cq.xn--p1ai/ytm",
            ),
        ),
        Entry(
            "🌀 CYBERPORTAL | Black List 9",
            listOf(
                "https://raw.githubusercontent.com/whoahaow/rjsxrd/refs/heads/main/githubmirror/bypass/bypass-all.txt",
                "https://raw.githubusercontent.com/whoahaow/rjsxrd/main/githubmirror/bypass/bypass-all.txt",
                "https://fastly.jsdelivr.net/gh/whoahaow/rjsxrd@main/githubmirror/bypass/bypass-all.txt",
                "https://cdn.jsdelivr.net/gh/whoahaow/rjsxrd@main/githubmirror/bypass/bypass-all.txt",
            ),
        ),
    )

    /** Creates the missing groups once and starts fetching them. Safe to call repeatedly. */
    fun seedOnce() {
        if (DataStore.defaultSubscriptionsSeeded) return
        scope.launch {
            val created = runCatching { createMissingGroups() }
                .onFailure(Logs::w)
                .getOrNull() ?: return@launch
            DataStore.defaultSubscriptionsSeeded = true
            val permits = Semaphore(UPDATE_PARALLELISM)
            created.forEach { group ->
                launch {
                    permits.withPermit {
                        runCatching { GroupUpdater.executeUpdate(group, true) }.onFailure(Logs::w)
                    }
                }
            }
        }
    }

    private suspend fun createMissingGroups(): List<ProxyGroup> {
        val existing = AppData.groups.subscriptions()
            .mapNotNull { it.subscription?.link?.takeIf(String::isNotBlank) }
            .toHashSet()
        val created = ArrayList<ProxyGroup>()
        for (entry in entries) {
            val link = SubscriptionMirrors.join(entry.mirrors)
            if (link in existing) continue
            val group = ProxyGroup(type = GroupType.SUBSCRIPTION).apply {
                name = entry.name
                subscription = SubscriptionBean().apply {
                    this.link = link
                    autoUpdate = true
                    autoUpdateDelay = AUTO_UPDATE_MINUTES
                }
            }
            created += GroupManager.createGroup(group)
        }
        return created
    }
}

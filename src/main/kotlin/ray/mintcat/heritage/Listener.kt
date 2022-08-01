package ray.mintcat.heritage

import ink.ptms.adyeshach.api.event.AdyeshachEntityInteractEvent
import org.bukkit.entity.EntityType
import org.bukkit.event.inventory.InventoryCloseEvent
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.platform.util.hasLore
import taboolib.platform.util.isAir
import taboolib.platform.util.isNotAir

object Listener {

    @Schedule(period = 20)
    fun update() {
        try {
            HeritageAPI.datas.forEach {
                it.update()
            }
        } catch (_: Exception) {
        }
    }

    @SubscribeEvent
    fun onEvent(event: AdyeshachEntityInteractEvent) {
        val data = HeritageAPI.datas.firstOrNull { it.heritage == event.entity } ?: return
        if (!data.canLock(event.player)) {
            return
        }
        if (data.drops.isEmpty()) {
            data.close()
            return
        }
        event.player.openInventory(data.inv)
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        val data = HeritageAPI.datas.firstOrNull { it.inv == event.inventory } ?: return
        if (data.inv.filter { it != null && it.isNotAir() }.isEmpty()) {
            data.close()
        }
    }

}
package ray.mintcat.heritage

import ink.ptms.adyeshach.api.AdyeshachAPI
import ink.ptms.adyeshach.api.Hologram
import ink.ptms.adyeshach.common.entity.EntityInstance
import ink.ptms.adyeshach.common.entity.type.AdyArmorStand
import ink.ptms.adyeshach.common.entity.type.AdyEntityLiving
import ink.ptms.um.Mythic
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import ray.mintcat.heritage.Config.Heritage.getChineseName
import taboolib.module.chat.colored
import taboolib.platform.util.hasLore
import taboolib.platform.util.isAir
import java.util.UUID

class HeritageData(
    val entity: Entity,
    val heritage: EntityInstance,
    val overTime: Long,
    var drops: MutableList<ItemStack> = mutableListOf(),
    val hds: MutableList<Hologram<*>> = mutableListOf(),
    val looker: MutableList<UUID> = mutableListOf(),
) {

    fun canLock(player: Player): Boolean {
        val item = player.inventory.itemInMainHand
        if (entity.type == EntityType.PLAYER) {
            if (Config.PlayerDrop.DeathRule.lock) {
                if (item == null || item.isAir || !item.hasItemMeta()) {
                    player.sendMessage(Config.PlayerDrop.DeathRule.lockMessage.colored())
                    return false
                }
                if (!item.hasLore(Config.PlayerDrop.DeathRule.lockLore(this))) {
                    player.sendMessage(Config.PlayerDrop.DeathRule.lockMessage.colored())
                    return false
                }
                player.inventory.itemInMainHand.amount = player.inventory.itemInMainHand.amount - 1
            }
            return true
        } else if (Mythic.API.getMob(entity) != null) {
            val mob = Mythic.API.getMob(entity)!!
            if (mob.config.getBoolean("Heritage.lock.enable")) {
                val lore = mob.config.getString("Heritage.lock.lore")
                if (!item.hasLore(lore)) {
                    player.sendMessage(mob.config.getString("Heritage.lock.error", "缺少道具").colored())
                    return false
                }
                player.inventory.itemInMainHand.amount = player.inventory.itemInMainHand.amount - 1
            }
            return true
        } else {
            val mob = Config.MobDrop.list[entity.type]
            if (mob != null && mob.lock) {
                if (item == null || item.isAir || !item.hasItemMeta()) {
                    player.sendMessage(mob.error.colored())
                    return false
                }
                if (!item.hasLore(mob.lockInfo)) {
                    player.sendMessage(mob.error.colored())
                    return false
                }
                player.inventory.itemInMainHand.amount = player.inventory.itemInMainHand.amount - 1
            }
            return true
        }
    }

    fun close() {
        heritage.delete()
        hds.map { it.delete() }
        HeritageAPI.datas.remove(this)
        inv.viewers.toList().forEach {
            it.closeInventory()
        }
    }

    init {
        if (heritage is AdyArmorStand) {
            heritage.setCustomName(Config.Heritage.name(this))
        }
        update()
        if (Config.PlayerDrop.DropRule.enable) {
            val gets = mutableListOf<ItemStack>()
            drops.forEach { item ->
                (1..item.amount).forEach {
                    gets.add(item.clone().apply {
                        amount = 1
                    })
                }
            }
            gets.shuffle()
            (1..((gets.size * Config.PlayerDrop.DropRule.amount).toInt())).forEach { _ ->
                gets.removeFirstOrNull()
            }
            drops = gets
        }
    }

    val inv = Bukkit.createInventory(null, InventoryType.CHEST, entity.getChineseName()).apply {
        addItem(*drops.toTypedArray())
    }

    fun update() {
        if (lastTime() <= 0) {
            close()
            return
        }
        Bukkit.getOnlinePlayers().forEach {
            if (!looker.contains(it.uniqueId)) {
                looker.add(it.uniqueId)
                val data = AdyeshachAPI.createHologram(
                    it,
                    entity.location.clone().add(0.0, 1.0, 0.0),
                    Config.Heritage.info(this)
                )
                hds.add(data)
            }
        }
        hds.forEach {
            it.update(Config.Heritage.info(this))
        }
    }

    fun lastTime(): Long {
        return overTime - System.currentTimeMillis()
    }


}
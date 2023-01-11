package ray.mintcat.heritage

import ink.ptms.adyeshach.api.AdyeshachAPI
import ink.ptms.adyeshach.common.entity.EntityInstance
import ink.ptms.adyeshach.common.entity.EntityTypes
import ink.ptms.adyeshach.common.entity.type.AdyArmorStand
import ink.ptms.adyeshach.common.entity.type.AdyEntityLiving
import ink.ptms.um.Mythic
import io.lumine.xikage.mythicmobs.MythicMobs
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobDeathEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.util.random
import taboolib.common5.Coerce
import taboolib.platform.util.giveItem
import taboolib.platform.util.hasLore
import taboolib.platform.util.isNotAir

object HeritageAPI {

    val datas = ArrayList<HeritageData>()

    @SubscribeEvent
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        val world = entity.world ?: return
        if (!Config.OpenWorld.list.contains(world.name)) {
            return
        }
        when {
            // 玩家
            entity is Player -> {
                Config.config.getStringList("PlayerDrop.deathCommand").ketherEval(entity)
            }
            // MM怪物
            MythicMobs.inst().mobManager.getMythicMobInstance(entity) != null -> {
                val mythicMobInstance = MythicMobs.inst().mobManager.getMythicMobInstance(entity)!!
                mythicMobInstance.type.config.getStringList("Heritage.deathCommand").ketherEval(entity)
            }
            // 动物
            else -> {
                Config.config.getStringList("MobDrop.${entity.type}.deathCommand").ketherEval(entity)
            }
        }
    }

    @SubscribeEvent
    fun onEntityDeathEvent(event: EntityDeathEvent) {
        val entity: Entity = event.entity
        val world = entity.world ?: return
        // 不在启用世界，则停止
        if (!Config.OpenWorld.list.contains(world.name)) {
            return
        }
        // 如果是玩家
        if (entity is Player) {
            if (Config.PlayerDrop.Info.enabled) {
                Bukkit.getOnlinePlayers().forEach {
                    it.sendMessage(Config.PlayerDrop.Info.message(entity))
                }
            }
            if (Config.PlayerDrop.DeathRule.enable) {
                createPlayer(entity, event)
            } else {
                val drops = event.drops.toList()
                event.drops.clear()
                submit(delay = 5) {
                    entity.spigot().respawn()
                    submit(delay = 5) {
                        entity.giveItem(drops)
                    }
                }

            }
            return
        }
        // 如果不是MM则停止
        if (MythicMobs.inst().mobManager.getMythicMobInstance(entity) != null) {
            return
        }
        createMinecraft(entity, event)
    }

    @SubscribeEvent
    fun mobCreate(event: MythicMobDeathEvent) {
        datas.add(
            HeritageData(
                event.mob.entity.bukkitEntity, getHeritage(event), System.currentTimeMillis() + Config.Heritage.time * 1000, event.drops.toMutableList()
            )
        )
        event.drops.clear()
    }

    fun createPlayer(target: Player, event: EntityDeathEvent) {
        val type = Config.PlayerDrop.DeathRule.type
        val notDrop = event.drops.filter {
            it != null && it.isNotAir() && it.hasItemMeta() && Config.PlayerDrop.DeathRule.nodrop.any { lore -> it.hasLore(lore) }
        }.toMutableList()
        event.drops.removeAll(notDrop)
        when (type) {
            Config.PlayerDrop.DeathRule.Type.ALL -> {
                datas.add(
                    HeritageData(
                        target, getHeritage(event), System.currentTimeMillis() + Config.Heritage.time * 1000, event.drops.toMutableList()
                    )
                )
                event.drops.clear()
            }

            Config.PlayerDrop.DeathRule.Type.SOME -> {
                val gets = mutableListOf<ItemStack>()
                event.drops.forEach { item ->
                    (1..item.amount).forEach {
                        gets.add(item.clone().apply {
                            amount = 1
                        })
                    }
                }
                gets.shuffle()
                (1..((gets.size * Config.PlayerDrop.DeathRule.amount).toInt())).forEach { _ ->
                    notDrop.add(gets.firstOrNull() ?: return@forEach)
                    gets.removeFirstOrNull()
                }
                event.drops.clear()
                datas.add(
                    HeritageData(
                        target, getHeritage(event), System.currentTimeMillis() + Config.Heritage.time * 1000, gets
                    )
                )
            }
        }
        submit(delay = 5) {
            target.spigot().respawn()
            submit(delay = 5) {
                target.giveItem(notDrop)
            }
        }

    }

    fun createMinecraft(entity: Entity, event: EntityDeathEvent) {
        val drop = Config.MobDrop.list[entity.type] ?: return
        drop.drops.forEach {
            val args = it.split(" ")
            if (args.size == 3 && !random(Coerce.toDouble(args[2]))) {
                return@forEach
            }
            val item = Mythic.API.getItemStack(args[0]) ?: return@forEach
            val amount = args.getOrElse(1) { "1" }.split("-").map { a -> Coerce.toInteger(a) }
            item.amount = random(amount[0], amount.getOrElse(1) { amount[0] })
            event.drops.add(item)
        }
        datas.add(
            HeritageData(
                entity, getHeritage(event), System.currentTimeMillis() + Config.Heritage.time * 1000, event.drops.toMutableList()
            )
        )
        event.drops.clear()
    }

    fun getHeritage(event: EntityDeathEvent): EntityInstance {
        val type = if (Config.Heritage.dead) {
            EntityTypes.valueOf(event.entity.type.name)
        } else {
            Config.Heritage.type
        }
        val npc = AdyeshachAPI.getEntityManagerPublicTemporary().create(type, event.entity.location).apply {
            if (this !is AdyArmorStand) {
                val tos = this as? AdyEntityLiving
                tos?.die(true)
                setCustomNameVisible(false)
            }
        }
        return npc
    }

    fun getHeritage(event: MythicMobDeathEvent): EntityInstance {
        val type = if (Config.Heritage.dead) {
            EntityTypes.valueOf(event.entity.type.name)
        } else {
            Config.Heritage.type
        }
        val npc = AdyeshachAPI.getEntityManagerPublicTemporary().create(type, event.entity.location).apply {
            if (this !is AdyArmorStand) {
                val tos = this as? AdyEntityLiving
                tos?.die(true)
                setCustomNameVisible(false)
            }
        }
        return npc
    }

}
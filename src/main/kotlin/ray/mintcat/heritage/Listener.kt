package ray.mintcat.heritage

import ink.ptms.adyeshach.api.event.AdyeshachEntityInteractEvent
import io.lumine.xikage.mythicmobs.MythicMobs
import org.bukkit.entity.EntityType
import org.bukkit.event.inventory.InventoryCloseEvent
import ray.mintcat.heritage.Config.PlayerDrop.DeathRule
import taboolib.common.platform.Schedule
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
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
        // 延时时间
        var delay: Long = 0
        // 判断是否是玩家类型
        if (data.entity.type == EntityType.PLAYER) {
            // 设置延时时间为玩家配置
            delay = DeathRule.delay
            // 执行语句
            DeathRule.command.ketherEval(event.player)
        } else if (MythicMobs.inst().mobManager.getMythicMobInstance(data.entity) != null) {
            val mob = MythicMobs.inst().mobManager.getMythicMobInstance(data.entity)!!
            if (mob.type.config.getBoolean("Heritage.lock.enable")) {
                delay = mob.type.config.getInteger("Heritage.delay").toLong()
                mob.type.config.getStringList("Heritage.command").ketherEval(event.player)
            }
        } else {
            Config.MobDrop.list[data.entity.type]?.let {
                // 设置延时时间为怪物配置
                delay = it.delay
                // 执行语句
                it.command.ketherEval(event.player)
            }
        }
        // 延迟打开界面
        submit(delay = delay) {
            // 判断玩家是否在线
            if (event.player.isOnline) {
                // 打开界面
                event.player.openInventory(data.inv)
            }
        }
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        val data = HeritageAPI.datas.firstOrNull { it.inv == event.inventory } ?: return
        if (data.inv.filter { it != null && it.isNotAir() }.isEmpty()) {
            data.close()
        }
    }

}
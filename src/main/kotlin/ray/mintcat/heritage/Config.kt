package ray.mintcat.heritage

import ink.ptms.adyeshach.common.entity.EntityTypes
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.chat.colored
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.configuration.util.getStringColored
import taboolib.module.configuration.util.getStringListColored
import taboolib.module.nms.getI18nName
import taboolib.platform.compat.replacePlaceholder

object Config {

    @Config(value = "setting.yml")
    lateinit var config: ConfigFile
        private set

    object PlayerDrop {

        object Info {
            val enabled: Boolean by lazy {
                config.getBoolean("PlayerDrop.Info.enable", false)
            }

            val message: Player.() -> String = {
                val get = config.getStringColored("PlayerDrop.Info.message")!!
                get.replacePlaceholder(this)
                    .replace("{location}", location(this))
            }

            val location: Player.() -> String = {
                val str = config.getString("PlayerDrop.Info.location")!!
                str.replace("{x}", location.x.toInt().toString())
                    .replace("{y}", location.y.toInt().toString())
                    .replace("{z}", location.z.toInt().toString())
                    .replace("{world}", location.world?.name?.toString() ?: "world")
                    .replacePlaceholder(this)
            }

        }

        object DeathRule {

            val enable: Boolean by lazy {
                config.getBoolean("PlayerDrop.DeathRule.enable", false)
            }

            val amount: Double by lazy {
                config.getDouble("PlayerDrop.DeathRule.amount", 1.0)
            }

            val type: Type by lazy {
                Type.valueOf(config.getString("PlayerDrop.DeathRule.type", "ALL")!!)
            }

            val nodrop: String by lazy {
                config.getString("PlayerDrop.DeathRule.nodrop", "null")?.colored()!!
            }

            val lock: Boolean by lazy {
                config.getBoolean("PlayerDrop.DeathRule.lock.enable", false)
            }

            val lockLore: HeritageData.() -> String = {
                config.getString("PlayerDrop.DeathRule.lock.lore", "!!ERROR!!")!!
            }

            val lockMessage:  String by lazy {
                config.getString("PlayerDrop.DeathRule.lock.error", "缺少道具")!!
            }


            enum class Type {
                ALL, SOME
            }
        }

        object DropRule {

            val enable: Boolean by lazy {
                config.getBoolean("PlayerDrop.DropRule.enable")
            }

            val amount: Double by lazy {
                config.getDouble("PlayerDrop.DropRule.amount", 0.0)
            }
        }

    }

    object MobDrop {

        val list = HashMap<EntityType, MobDropData>()

        @Awake(LifeCycle.ENABLE)
        fun load() {
            config.getConfigurationSection("MobDrop")?.getKeys(false)?.forEach {
                val type = try {
                    EntityType.valueOf(it)
                } catch (_: Exception) {
                    return@forEach
                }
                list[type] = MobDropData(
                    type,
                    config.getStringList("MobDrop.${it}.drop"),
                    config.getBoolean("MobDrop.${it}.lock.enable"),
                    config.getString("MobDrop.${it}.lock.lore", "null")!!,
                    config.getString("MobDrop.${it}.lock.error", "缺少道具")!!
                )
            }
        }

        data class MobDropData(
            val type: EntityType,
            val drops: List<String>,
            val lock: Boolean,
            val lockInfo: String,
            val error: String
        )

    }

    object Heritage {

        val type: EntityTypes by lazy {
            try {
                EntityTypes.valueOf(config.getString("Heritage.type", "ARMOR_STAND")!!)
            } catch (_: Exception) {
                EntityTypes.ARMOR_STAND
            }
        }

        val name: HeritageData.() -> String = {
            val get = config.getString("Heritage.name", "墓碑")!!
            get.replace("{name}", entity.getChineseName())
                .replace("{type}", type.name)
                .replace("{lastTime}", Time(lastTime()).toString())
        }

        val dead: Boolean by lazy {
            config.getBoolean("Heritage.dead", false)
        }

        val info: HeritageData.() -> List<String> = {
            val get = config.getStringListColored("Heritage.info")
            get.asSequence().map {
                it.replace("{name}", entity.getChineseName())
                    .replace("{type}", type.name)
                    .replace("{lastTime}", Time(lastTime()).toString())
            }.toList()
        }

        val time: Long by lazy {
            config.getLong("Heritage.time", 1L)
        }

        fun Entity.getChineseName(): String {
            if (this is Player) {
                return this.name
            }
            if (customName != null) {
                return customName
            }
            return getI18nName()
        }

    }

    object OpenWorld {
        val list: List<String> = config.getStringList("OpenWorld")
    }


}
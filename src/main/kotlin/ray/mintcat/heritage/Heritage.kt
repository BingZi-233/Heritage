package ray.mintcat.heritage

import org.bukkit.command.CommandSender
import taboolib.common.platform.Plugin
import taboolib.common.platform.command.command

object Heritage : Plugin() {

    val api = HeritageAPI

    override fun onEnable() {
        command("heritage") {
            execute<CommandSender> { sender, _, _ ->
                Config.config.reload()
                sender.sendMessage("重载完成")
            }
        }
    }

}
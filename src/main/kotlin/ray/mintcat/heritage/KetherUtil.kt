package ray.mintcat.heritage

import org.bukkit.command.CommandSender
import taboolib.common.platform.function.adaptPlayer
import taboolib.library.kether.LocalizedException
import taboolib.module.kether.KetherShell
import taboolib.module.kether.printKetherErrorMessage

/**
 * Kether eval
 * Kether执行器
 *
 * @param sender 执行者
 */
fun List<String>.ketherEval(sender: CommandSender) {
    try {
        KetherShell.eval(this) {
            this.sender = adaptPlayer(sender)
        }
    } catch (e: LocalizedException) {
        e.printKetherErrorMessage()
    } catch (e: Throwable) {
        e.printKetherErrorMessage()
    }
}

/**
 * Kether eval
 * Kether执行器
 *
 * @param sender 执行者
 */
fun String.ketherEval(sender: CommandSender) {
    try {
        KetherShell.eval(this) {
            this.sender = adaptPlayer(sender)
        }
    } catch (e: LocalizedException) {
        e.printKetherErrorMessage()
    } catch (e: Throwable) {
        e.printKetherErrorMessage()
    }
}
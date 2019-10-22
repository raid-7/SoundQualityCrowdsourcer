package raid.soundquality

import raid.soundquality.db.Db
import raid.soundquality.db.SoundSet
import java.io.File
import java.lang.System.getenv
import java.net.InetSocketAddress
import java.net.Proxy


private fun getEnvProxy(): Proxy {
    val url: String = getenv("PROXY_URL") ?: return Proxy.NO_PROXY
    val parts = url.split(':')
    return Proxy(Proxy.Type.SOCKS, InetSocketAddress(parts[0], parts[1].toInt()))
}

fun main(args: Array<String>) {
    if (args.isEmpty())
        throw IllegalArgumentException("Specify dataset directory")

    val db = Db(getenv("DB_URL"), getenv("DB_USER"), getenv("DB_PASSWORD"))
    val sounds = SoundSet(File(args[0]))
    val bot = SQCrowdsourcerBot(getenv("TELEGRAM_TOKEN"), db, sounds, getEnvProxy())
    bot.startPolling()
}

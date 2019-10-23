package raid.soundquality

import me.ivmg.telegram.bot
import me.ivmg.telegram.dispatch
import me.ivmg.telegram.dispatcher.command
import me.ivmg.telegram.dispatcher.telegramError
import me.ivmg.telegram.dispatcher.text
import me.ivmg.telegram.entities.ParseMode
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import raid.soundquality.db.Db
import raid.soundquality.db.SoundSet
import java.io.File
import java.net.Proxy
import java.util.*


internal class SQCrowdsourcerBot(
    tgToken: String,
    private val db: Db = Db(),
    private val sounds: SoundSet,
    proxy: Proxy = Proxy.NO_PROXY
) {
    private val random = Random()
    private val logger = LoggerFactory.getLogger(SQCrowdsourcerBot::class.java)

    private val bot = bot {
        token = tgToken
        this.proxy = proxy
        logLevel = HttpLoggingInterceptor.Level.BASIC

        dispatch {
            text { _, update ->
                update.message?.apply {
                    if (text?.startsWith("/") == true)
                        return@apply

                    safe { processPossibleRate(chat.id, text) }
                }
            }

            command("another") { _, update ->
                update.message?.apply {
                    safe { sendRateRequest(chat.id, true) }
                }
            }

            command("help") { _, update ->
                update.message?.apply {
                    safe { sendHelp(chat.id) }
                    safe { sendRateRequest(chat.id) }
                }
            }
            command("start") { _, update ->
                update.message?.apply {
                    safe { sendStart(chat.id) }
                    safe { sendRateRequest(chat.id) }
                }
            }

            command("stats") { _, update ->
                update.message?.apply {
                    safe { sendResults(chat.id) }
                }
            }

            telegramError { _, err ->
                logger.error("Telegram error: ${err.getErrorMessage()}")
            }
        }
    }

    fun startPolling() {
        bot.startPolling()
    }

    private fun sendResults(chatId: Long) {
        val name = "dataset" + random.nextLong() + ".csv"
        val file = File(name)
        exportStats(file, db.getRates())
        bot.sendDocument(chatId, file)
        file.delete()
    }

    private fun sendRateRequest(chatId: Long, override: Boolean = false) {
        val proposed = sounds.proposeSample()
        val real = db.setState(chatId, proposed.first, proposed.second, override)
        val (source, derivative) = sounds.getFiles(real.first, real.second)

        bot.sendAudio(chatId, source, title = "Source speach")
        bot.sendAudio(chatId, derivative, title = "Phone sound")
        bot.sendMessage(chatId, "Please rate this call from 0 (completely unrecognizable) to 6 (prefect quality)")
    }

    private fun sendWrongRate(chatId: Long) {
        bot.sendMessage(chatId, "Wrong rate. Please rate this call from 0 (completely unrecognizable) to 6 (prefect quality)")
    }

    fun processPossibleRate(chatId: Long, text: String?) {
        val value = text?.let {
            if (!it.all { it.isDigit() })
                null
            else {
                val v = try {
                    it.toInt()
                } catch (exc: NumberFormatException) {
                    return@let null
                }
                if (v >= 0 && v <= 6)
                    v
                else
                    null
            }
        }

        if (value == null) {
            sendWrongRate(chatId)
            return
        }

        db.addRate(chatId, value)
        sendRateRequest(chatId)
    }

    private fun sendStart(chatId: Long) {
        bot.sendMessage(
            chatId,
            """
            Hi there!
            
            We are going to win the Telegram contest and bring VoIP calls quality to a new level. And now we need your help!
            
            The bot will send you sets of two sounds. The first one is a source speech and the second one is what the other side hears.
            
            Please listen to both audios carefully and rate the call quality from 0 (speech is completely unrecognizable) to 6 (prefect quality).
            
            Thank you!
            """.trimIndent(), parseMode = ParseMode.MARKDOWN
        )
    }

    private fun sendHelp(chatId: Long) {
        bot.sendMessage(
            chatId,
            """
            We need your help to win the Telegram contest and bring VoIP calls quality to a new level.
            
            The bot will send you sets of two sounds. The first one is a source speech and the second one is what the other side hears.
            
            Please, listen to both audios carefully and rate the call quality from 0 (speech is completely unrecognizable) to 6 (prefect quality).
            
            Thank you!
            """.trimIndent(), parseMode = ParseMode.MARKDOWN
        )
    }

    private inline fun <T> T.safe(func: T.() -> Unit) {
        try {
            func()
        } catch (exc: Exception) {
            exc.printStackTrace()
        }
    }
}

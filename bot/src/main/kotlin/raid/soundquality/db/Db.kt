package raid.soundquality.db

import me.ivmg.telegram.entities.Chat
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.DEFAULT_REPETITION_ATTEMPTS
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object Rates : Table("rates") {
    val sampleName = text("sample_name").primaryKey()
    val derivativeName = text("derivative_name").primaryKey()
    val chatId = long("chat_id").primaryKey()
    val rate = integer("rate")
}

object ChatStates : Table("states") {
    val chatId = long("chat_id").primaryKey()
    val sampleName = text("sample_name").nullable()
    val derivativeName = text("derivative_name").nullable()
}

class Db(url: String? = null, user: String? = null, password: String? = null) {
    private val conn: Database

    init {
        val finalUrl = url ?: "jdbc:sqlite:bot-test.db"
        val driver = try {
            DriverManager.getDriver(finalUrl).javaClass.name
        } catch (_: SQLException) {
            when {
                finalUrl.contains("sqlite") -> "org.sqlite.JDBC"
                finalUrl.contains("postgres") -> "org.postgresql.Driver"
                else -> throw RuntimeException("Cannot determine jdbc driver")
            }
        }
        conn = Database.connect(finalUrl, driver, user = user ?: "", password = password ?: "")

        transaction {
            SchemaUtils.create(Rates, ChatStates)
        }
    }

    fun addRate(chatIdVal: Long, rateVal: Int): Boolean =
        transaction {
            val (sampleNameVal, derivativeNameVal) = ChatStates.select {
                ChatStates.chatId.eq(chatIdVal)
            }.map {
                Pair(it[ChatStates.sampleName], it[ChatStates.derivativeName])
            }.getOrNull(0) ?: return@transaction false

            if (sampleNameVal == null || derivativeNameVal == null)
                return@transaction false

            Rates.insert {
                it[chatId] = chatIdVal
                it[sampleName] = sampleNameVal
                it[derivativeName] = derivativeNameVal
                it[rate] = rateVal
            }
            ChatStates.update({
                ChatStates.chatId.eq(chatIdVal)
            }) {
                it[sampleName] = null
                it[derivativeName] = null
            }

            true
        }

    fun setState(chatIdVal: Long, sampleNameVal: String, derivativeNameVal: String, override: Boolean = true) =
        transaction {
            val isNew = Rates.select {
                Rates.chatId.eq(chatIdVal) and
                        Rates.sampleName.eq(sampleNameVal) and
                        Rates.derivativeName.eq(derivativeNameVal)
            }.empty()

            val isNewChat = ChatStates.select {
                ChatStates.chatId.eq(chatIdVal)
            }.empty()


            if (isNew) {
                if (isNewChat) {
                    ChatStates.insert {
                        it[chatId] = chatIdVal
                        it[sampleName] = sampleNameVal
                        it[derivativeName] = derivativeNameVal
                    }
                } else {
                    val overrideExpr = if (override) {
                        Op.TRUE
                    } else {
                        Op.FALSE
                    }

                    ChatStates.update({
                        ChatStates.chatId.eq(chatIdVal) and
                                ((ChatStates.sampleName.isNull() and ChatStates.derivativeName.isNull()) or overrideExpr)
                    }) {
                        it[sampleName] = sampleNameVal
                        it[derivativeName] = derivativeNameVal
                    }
                }
            }

            ChatStates.select {
                ChatStates.chatId.eq(chatIdVal)
            }.map {
                Pair(it[ChatStates.sampleName]!!, it[ChatStates.derivativeName]!!)
            }[0]
        }

    fun getRates(): Map<Pair<String, String>, List<Int>> =
        transaction {
            Rates.selectAll()
                .map {
                    Pair(Pair(it[Rates.sampleName], it[Rates.derivativeName]), it[Rates.rate])
                }
                .groupBy {
                    it.first
                }
                .mapValues {
                    it.value.map {
                        it.second
                    }
                }
        }


    private fun <T> transaction(statement: Transaction.() -> T): T =
        transaction(
            Connection.TRANSACTION_SERIALIZABLE, DEFAULT_REPETITION_ATTEMPTS, conn, statement
        )
}

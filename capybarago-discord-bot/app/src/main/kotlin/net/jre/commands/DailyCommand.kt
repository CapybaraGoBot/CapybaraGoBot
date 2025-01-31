package net.jre.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.jre.Database
import java.sql.Date
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

object DailyCommand {
    
    fun handle(event: MessageReceivedEvent) {
        val userId = event.author.idLong
        val connection = Database.getConnection() // Isso aqui eu peguei de um tutorial antigo, mas funciona

        connection.use { conn ->
            // Primeiro vejo se o cara já tá no sistema 
            val checkUserQuery = "SELECT * FROM user_coins WHERE user_id = ?"
            val checkUserStatement = conn.prepareStatement(checkUserQuery).apply {
                setLong(1, userId) // Coloquei PreparedStatement pq eu quis
            }
            val userExists = checkUserStatement.executeQuery().next()

            if (userExists) {
                // verificar se já pegou o daily hoje
                val lastDailyQuery = "SELECT last_daily FROM user_coins WHERE user_id = ?"
                val lastDailyStatement = conn.prepareStatement(lastDailyQuery).apply {
                    setLong(1, userId) // quase copiei do código anterior
                }
                val lastDailyResult = lastDailyStatement.executeQuery()

                if (lastDailyResult.next()) {
                    val lastDaily = lastDailyResult.getDate("last_daily").toLocalDate()
                    val today = LocalDate.now(ZoneId.of("America/Brasilia")) // Fuso de brasilia 

                    if (lastDaily.isBefore(today)) {
                        // Gerando a mangas
                        val mangas = Random.nextInt(1250, 3701) // O Random é inclusivo no primeiro e exclusivo no segundo? Ainda me confundo
                        
                        // Atualiza no banco (aqui quase me perdi nas interrogações)
                        val updateQuery = "UPDATE user_coins SET mangas = mangas + ?, last_daily = ? WHERE user_id = ?"
                        val updateStatement = conn.prepareStatement(updateQuery).apply {
                            setInt(1, mangas)
                            setDate(2, Date.valueOf(today)) // Tive que pesquisar como converter LocalDate pra SQL Date kkk
                            setLong(3, userId)
                        }
                        updateStatement.executeUpdate()

                        event.channel.sendMessage("<@${userId}> recebeu **$mangas** mangas diários!").queue() // Mensagem básica mas eficiente
                    } else {
                        // Código do contador de tempo o parte chata vey
                        val now = ZonedDateTime.now(ZoneId.of("America/Brasilia"))
                        // Cálculo da próxima meia-noite 
                        val nextReset = now.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("America/Brasilia"))
                        val timestamp = nextReset.toEpochSecond() // Convertendo pro formato do Discord

                        // Usei a marcação temporal do Discord pra ficar dinâmico 
                        event.channel.sendMessage("<@${userId}> você já coletou seus mangas hoje! Volte <t:${timestamp}:R>.").queue()
                    }
                }
            } else {
                // Se for novo usuário 
                val mangas = Random.nextInt(1250, 3701) // Mesmo valor que os veteranos
                val insertQuery = "INSERT INTO user_coins (user_id, mangas, last_daily) VALUES (?, ?, ?)"
                val insertStatement = conn.prepareStatement(insertQuery).apply {
                    setLong(1, userId)
                    setInt(2, mangas)
                    setDate(3, Date.valueOf(LocalDate.now(ZoneId.of("America/Brasilia")))) // Data atual BR
                }
                insertStatement.executeUpdate()

                event.channel.sendMessage("<@${userId}> recebeu **$mangas** mangas diários!").queue() // Mesma mensagem pra não criar diferença
            }
        }
    }
}
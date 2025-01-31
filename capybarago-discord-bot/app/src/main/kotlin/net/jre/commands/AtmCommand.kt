package net.jre.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.entities.User
import net.jre.Database

object AtmCommand {
    fun handle(event: MessageReceivedEvent) {
        // Verify if the message has been sent by a bot. If yes, ignore.
        // This avoids the bot reply yourself or another bots.
        if (event.author.isBot) return

        // Catch the sent message by the user and divide in parts (commands + args).
        val message = event.message
        val content = message.contentRaw
        val args = content.split(" ")

        // The author of the message (who sent the message).
        val author = event.author

        // Define the target user. If the command mention someone, use this people.
        // Else, try find by the ID or name/nickname.
        // If nothing has been found, the target is the author himself.
        val targetUser = when {
            // If has mentions, catch the first user mentioned.
            message.mentions.users.isNotEmpty() -> message.mentions.users.first()

            // If the second arg is an valid ID, try find the user by the ID.
            args.size > 1 && args[1].toLongOrNull() != null -> {
                try {
                    event.jda.retrieveUserById(args[1].toLong()).complete() ?: author
                } catch (e: Exception) {
                    author // If have an error, use the author as fallback.
                }
            }

            // If the second arg is a name/nickname, try find the user.
            args.size > 1 -> {
                val memberByNickname = event.guild.getMembersByNickname(args[1], true).firstOrNull()
                val memberByName = event.guild.getMembersByName(args[1], true).firstOrNull()
                memberByNickname?.user ?: memberByName?.user ?: author
            }

            // If have no arguments, the target is the author himself.
            else -> author
        }

        // Connect to the database to search the quantity of mangas and the ranking of the user.
        val connection = Database.getConnection()
        connection.use { conn ->
            // Query to search the quantity of mangas and the ranking of the user.
            val query = """
                SELECT mangas, (
                    SELECT COUNT(*) 
                    FROM user_coins u2
                    WHERE u2.mangas > u1.mangas
                    OR (u2.mangas = u1.mangas AND u2.user_id <= u1.user_id)
                ) AS ranking
                FROM user_coins u1
                WHERE u1.user_id = ?
            """.trimIndent()

            // Prepare a query and define the ID of the target user.
            val preparedStatement = conn.prepareStatement(query)
            preparedStatement.setLong(1, targetUser.idLong)

            // Execute a query and get the result.
            val resultSet = preparedStatement.executeQuery()

            // Build the reply with base of results of the query.
            val response = if (resultSet.next()) {
                val mangas = resultSet.getInt("mangas")
                val ranking = resultSet.getInt("ranking")

                // Se o alvo for o próprio autor, monta uma mensagem personalizada.
                if (targetUser == author) {
                    "${author.asMention} you have **$mangas** mangas and is in the position **#$ranking** in ranking!"
                } else {
                    // Se o alvo for outro usuário, monta uma mensagem diferente.
                    "${author.asMention} ${targetUser.asMention} have **$mangas** mangas and is in the position **#$ranking** in ranking!"
                }
            } else {
                // Se o usuário não tiver registros no banco de dados, retorna que ele tem 0 mangas.
                if (targetUser == author) {
                    "${author.asMention} you have **0** mangas!"
                } else {
                    "${author.asMention} ${targetUser.asMention} have **0** mangas!"
                }
            }

            // Sent the response to the chat.
            event.channel.sendMessage(response).queue()
        }
    }
}

package net.jre.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.jre.Database

object AtmCommand {
    fun handle(event: MessageReceivedEvent) {
        // Verifies if the message was sent by a bot. If so, ignore it.
        // This prevents the bot from replying to itself or other bots.
        if (event.author.isBot) return

        // Captures the message sent by the user and splits it into parts (commands + arguments).
        val message = event.message
        val content = message.contentRaw
        val args = content.split(" ")

        // The author of the message (who sent it).
        val author = event.author

        // Defines the target user. If the command mentions someone, use that person.
        // Otherwise, try to find the user by ID or name/nickname.
        // If no match is found, the target is the author.
        val targetUser = when {
            // If there are mentions, get the first mentioned user.
            message.mentions.users.isNotEmpty() -> message.mentions.users.first()

            // If the second argument is a valid ID, try to find the user by it.
            args.size > 1 && args[1].toLongOrNull() != null -> {
                try {
                    event.jda.retrieveUserById(args[1].toLong()).complete() ?: author
                } catch (e: Exception) {
                    author // If an error occurs, use the author as a fallback.
                }
            }

            // If the second argument is a name/nickname, try to find the user.
            args.size > 1 -> {
                val memberByNickname = event.guild.getMembersByNickname(args[1], true).firstOrNull()
                val memberByName = event.guild.getMembersByName(args[1], true).firstOrNull()
                memberByNickname?.user ?: memberByName?.user ?: author
            }

            // If there are no arguments, the target is the author.
            else -> author
        }

        // Connects to the database to retrieve the number of mangas and the user's ranking.
        val connection = Database.getConnection()
        connection.use { conn ->
            // Query to retrieve the number of mangas and the user's ranking.
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

            // Prepares the query and sets the target user's ID.
            val preparedStatement = conn.prepareStatement(query)
            preparedStatement.setLong(1, targetUser.idLong)

            // Executes the query and retrieves the result.
            val resultSet = preparedStatement.executeQuery()

            // Builds the reply based on the query results.
            val response = if (resultSet.next()) {
                val mangas = resultSet.getInt("mangas")
                val ranking = resultSet.getInt("ranking")

                // If the target is the author, send a personalized message.
                if (targetUser == author) {
                    "${author.asMention} you have **$mangas** mangas and are in position **#$ranking** in the ranking!"
                } else {
                    // If the target is another user, send a different message.
                    "${author.asMention} ${targetUser.asMention} has **$mangas** mangas and is in position **#$ranking** in the ranking!"
                }
            } else {
                // If the user has no records in the database, return that they have 0 mangas.
                if (targetUser == author) {
                    "${author.asMention} you have **0** mangas!"
                } else {
                    "${author.asMention} ${targetUser.asMention} has **0** mangas!"
                }
            }

            // Sends the response to the chat.
            event.channel.sendMessage(response).queue()
        }
    }
}

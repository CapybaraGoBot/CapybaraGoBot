package net.jre.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.entities.User
import net.jre.Database

object AtmCommand {
    fun handle(event: MessageReceivedEvent) {
        val message = event.message
        val content = message.contentRaw
        val args = content.split(" ")

        val author = event.author
        val targetUser = when {
            message.mentions.users.isNotEmpty() -> message.mentions.users.first()
            args.size > 1 && args[1].toLongOrNull() != null -> {
                try {
                    event.jda.retrieveUserById(args[1].toLong()).complete() ?: author
                } catch (e: Exception) {
                    author
                }
            }
            args.size > 1 -> {
                val memberByNickname = event.guild.getMembersByNickname(args[1], true).firstOrNull()
                val memberByName = event.guild.getMembersByName(args[1], true).firstOrNull()
                memberByNickname?.user ?: memberByName?.user ?: author
            }
            else -> author
        }

        val connection = Database.getConnection()
        connection.use { conn ->
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

            val preparedStatement = conn.prepareStatement(query)
            preparedStatement.setLong(1, targetUser.idLong)

            val resultSet = preparedStatement.executeQuery()

            val response = if (resultSet.next()) {
                val mangas = resultSet.getInt("mangas")
                val ranking = resultSet.getInt("ranking")

                if (targetUser == author) {
                    "${author.asMention} você tem **$mangas** mangas e está na posição **#$ranking** no ranking!"
                } else {
                    "${author.asMention} ${targetUser.asMention} tem **$mangas** mangas e está na posição **#$ranking** no ranking!"
                }
            } else {
                if (targetUser == author) {
                    "${author.asMention} você tem **0** mangas!"
                } else {
                    "${author.asMention} ${targetUser.asMention} tem **0** mangas!"
                }
            }

            event.channel.sendMessage(response).queue()
        }
    }
}
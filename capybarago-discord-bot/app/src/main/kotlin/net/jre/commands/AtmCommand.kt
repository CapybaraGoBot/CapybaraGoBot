package net.jre.commands

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.entities.User
import net.jre.Database

object AtmCommand {
    fun handle(event: MessageReceivedEvent) {
        // Verifica se a mensagem foi enviada por um bot. Se for, ignora.
        // Isso evita que o bot responda a si mesmo ou a outros bots.
        if (event.author.isBot) return

        // Pega a mensagem enviada pelo usuário e divide em partes (comando + argumentos).
        val message = event.message
        val content = message.contentRaw
        val args = content.split(" ")

        // O autor da mensagem (quem enviou o comando).
        val author = event.author

        // Define o usuário alvo. Se o comando mencionar alguém, usa essa pessoa.
        // Caso contrário, tenta buscar pelo ID ou nome/nickname.
        // Se nada for encontrado, o alvo é o próprio autor.
        val targetUser = when {
            // Se houver menções, pega o primeiro usuário mencionado.
            message.mentions.users.isNotEmpty() -> message.mentions.users.first()

            // Se o segundo argumento for um ID válido, tenta buscar o usuário pelo ID.
            args.size > 1 && args[1].toLongOrNull() != null -> {
                try {
                    event.jda.retrieveUserById(args[1].toLong()).complete() ?: author
                } catch (e: Exception) {
                    author // Se der erro, usa o autor como fallback.
                }
            }

            // Se o segundo argumento for um nome/nickname, tenta buscar o usuário.
            args.size > 1 -> {
                val memberByNickname = event.guild.getMembersByNickname(args[1], true).firstOrNull()
                val memberByName = event.guild.getMembersByName(args[1], true).firstOrNull()
                memberByNickname?.user ?: memberByName?.user ?: author
            }

            // Se não houver argumentos, o alvo é o próprio autor.
            else -> author
        }

        // Conecta ao banco de dados para buscar a quantidade de mangás e o ranking do usuário.
        val connection = Database.getConnection()
        connection.use { conn ->
            // Query para buscar a quantidade de mangás e o ranking do usuário.
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

            // Prepara a query e define o ID do usuário alvo.
            val preparedStatement = conn.prepareStatement(query)
            preparedStatement.setLong(1, targetUser.idLong)

            // Executa a query e obtém o resultado.
            val resultSet = preparedStatement.executeQuery()

            // Monta a resposta com base no resultado da query.
            val response = if (resultSet.next()) {
                val mangas = resultSet.getInt("mangas")
                val ranking = resultSet.getInt("ranking")

                // Se o alvo for o próprio autor, monta uma mensagem personalizada.
                if (targetUser == author) {
                    "${author.asMention} você tem **$mangas** mangas e está na posição **#$ranking** no ranking!"
                } else {
                    // Se o alvo for outro usuário, monta uma mensagem diferente.
                    "${author.asMention} ${targetUser.asMention} tem **$mangas** mangas e está na posição **#$ranking** no ranking!"
                }
            } else {
                // Se o usuário não tiver registros no banco de dados, retorna que ele tem 0 mangas.
                if (targetUser == author) {
                    "${author.asMention} você tem **0** mangas!"
                } else {
                    "${author.asMention} ${targetUser.asMention} tem **0** mangas!"
                }
            }

            // Envia a resposta no chat.
            event.channel.sendMessage(response).queue()
        }
    }
}
package net.jre.events

import dev.minn.jda.ktx.events.listener
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.jre.commands.AtmCommand
import net.jre.commands.DailyCommand

class MessageListener : ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val message = event.message.contentRaw

        when {
            message.startsWith("+atm") -> AtmCommand.handle(event)
            message.startsWith("+daily") -> DailyCommand.handle(event)
        }
    }
}
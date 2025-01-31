package net.jre

import dev.minn.jda.ktx.jdabuilder.light
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.jre.events.MessageListener
import io.github.cdimascio.dotenv.dotenv

fun main() {
    val dotenv = dotenv()

    val token = dotenv["DISCORD_TOKEN"]


    val jda = light(token, enableCoroutines = true) {
        setActivity(Activity.playing("💥"))
        enableIntents(GatewayIntent.MESSAGE_CONTENT) 
    }

    jda.addEventListener(MessageListener())
}
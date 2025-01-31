package net.jre

import java.sql.Connection
import java.sql.DriverManager

object Database {
    private const val URL = "jdbc:mariadb://localhost:3306/jre"
    private const val USER = "jre"
    private const val PASSWORD = "Bozoye052816#"

    fun getConnection(): Connection {
        return DriverManager.getConnection(URL, USER, PASSWORD)
    }
}
package me.voltual.mcl

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement

class MclSqliteSaver(dbPath: String) : AutoCloseable {
    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
    private val insertStmt: PreparedStatement

    init {
        connection.autoCommit = false
        val statement = connection.createStatement()
        statement.execute("PRAGMA synchronous = OFF;")
        statement.execute("PRAGMA journal_mode = MEMORY;")
        statement.execute(
            """
            CREATE TABLE IF NOT EXISTS `blocks` (
                `pos` INT PRIMARY KEY,
                `data` BLOB
            );
            """.trimIndent()
        )
        insertStmt = connection.prepareStatement("INSERT OR REPLACE INTO `blocks` (`pos`, `data`) VALUES (?, ?)")
    }

    fun saveBlock(pos: MclPos, data: ByteArray) {
        insertStmt.setLong(1, pos.encode())
        insertStmt.setBytes(2, data)
        insertStmt.addBatch()
    }

    fun commit() {
        insertStmt.executeBatch()
        connection.commit()
    }

    override fun close() {
        insertStmt.close()
        connection.close()
    }
}
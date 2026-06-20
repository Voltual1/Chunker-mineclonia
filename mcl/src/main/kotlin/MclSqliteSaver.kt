package me.voltual.mcl

import java.io.File
import java.lang.reflect.Method

class MclSqliteSaver(dbPath: String) : AutoCloseable {
    private var isAndroid = false
    
    // JDBC 引擎字段
    private var jdbcConnection: Any? = null
    private var jdbcInsertStmt: Any? = null
    
    // 安卓原生引擎反射字段
    private var androidDb: Any? = null
    private var androidInsertStmt: Any? = null
    private var androidBindLongMethod: Method? = null
    private var androidBindBlobMethod: Method? = null
    private var androidExecuteInsertMethod: Method? = null
    private var androidClearBindingsMethod: Method? = null
    private var androidBeginTransactionMethod: Method? = null
    private var androidSetTransactionSuccessfulMethod: Method? = null
    private var androidEndTransactionMethod: Method? = null
    private var androidInTransactionMethod: Method? = null
    private var androidCloseDbMethod: Method? = null
    private var androidCloseStmtMethod: Method? = null

    init {
        val file = File(dbPath)
        file.parentFile?.mkdirs()

        try {
            // 探测当前运行环境是否为安卓系统
            Class.forName("android.database.sqlite.SQLiteDatabase")
            isAndroid = true
        } catch (e: ClassNotFoundException) {
            isAndroid = false
        }

        if (isAndroid) {
            initAndroid(dbPath)
        } else {
            initJdbc(dbPath)
        }
    }

    private fun initAndroid(dbPath: String) {
        try {
            val dbClass = Class.forName("android.database.sqlite.SQLiteDatabase")
            val stmtClass = Class.forName("android.database.sqlite.SQLiteStatement")
            val cursorClass = Class.forName("android.database.Cursor")
            
            val openMethod = dbClass.getMethod(
                "openOrCreateDatabase", 
                String::class.java, 
                Class.forName("android.database.sqlite.SQLiteDatabase\$CursorFactory")
            )
            androidDb = openMethod.invoke(null, dbPath, null)
            
            // 使用 rawQuery 安全执行 PRAGMA 语句，避免 execSQL 报错
            val rawQueryMethod = dbClass.getMethod("rawQuery", String::class.java, Array<String>::class.java)
            val moveToFirstMethod = cursorClass.getMethod("moveToFirst")
            val closeCursorMethod = cursorClass.getMethod("close")

            val safeExecutePragma = { pragmaSql: String ->
                val cursor = rawQueryMethod.invoke(androidDb, pragmaSql, null)
                if (cursor != null) {
                    moveToFirstMethod.invoke(cursor)
                    closeCursorMethod.invoke(cursor)
                }
            }

            safeExecutePragma("PRAGMA synchronous = OFF;")
            safeExecutePragma("PRAGMA journal_mode = MEMORY;")
            
            // CREATE TABLE 不需要返回结果，使用 execSQL 执行
            val execSQLMethod = dbClass.getMethod("execSQL", String::class.java)
            execSQLMethod.invoke(androidDb, """
                CREATE TABLE IF NOT EXISTS `blocks` (
                    `pos` INT PRIMARY KEY,
                    `data` BLOB
                );
            """.trimIndent())
            
            val compileMethod = dbClass.getMethod("compileStatement", String::class.java)
            androidInsertStmt = compileMethod.invoke(androidDb, "INSERT OR REPLACE INTO `blocks` (`pos`, `data`) VALUES (?, ?)")
            
            androidBindLongMethod = stmtClass.getMethod("bindLong", Int::class.javaPrimitiveType, Long::class.javaPrimitiveType)
            androidBindBlobMethod = stmtClass.getMethod("bindBlob", Int::class.javaPrimitiveType, ByteArray::class.java)
            androidExecuteInsertMethod = stmtClass.getMethod("executeInsert")
            androidClearBindingsMethod = stmtClass.getMethod("clearBindings")
            
            androidBeginTransactionMethod = dbClass.getMethod("beginTransaction")
            androidSetTransactionSuccessfulMethod = dbClass.getMethod("setTransactionSuccessful")
            androidEndTransactionMethod = dbClass.getMethod("endTransaction")
            androidInTransactionMethod = dbClass.getMethod("inTransaction")
            
            androidCloseDbMethod = dbClass.getMethod("close")
            androidCloseStmtMethod = stmtClass.getMethod("close")
            
            // 开启首个批处理事务
            androidBeginTransactionMethod?.invoke(androidDb)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果反射初始化发生任何异常，自动降级至 JDBC 驱动
            isAndroid = false
            initJdbc(dbPath)
        }
    }

    private fun initJdbc(dbPath: String) {
        try {
            val driverClass = Class.forName("java.sql.DriverManager")
            val getConnectionMethod = driverClass.getMethod("getConnection", String::class.java)
            val conn = getConnectionMethod.invoke(null, "jdbc:sqlite:$dbPath")
            jdbcConnection = conn
            
            val setAutoCommitMethod = conn.javaClass.getMethod("setAutoCommit", Boolean::class.javaPrimitiveType)
            setAutoCommitMethod.invoke(conn, false)
            
            val createStatementMethod = conn.javaClass.getMethod("createStatement")
            val statement = createStatementMethod.invoke(conn)
            val executeMethod = statement.javaClass.getMethod("execute", String::class.java)
            executeMethod.invoke(statement, "PRAGMA synchronous = OFF;")
            executeMethod.invoke(statement, "PRAGMA journal_mode = MEMORY;")
            executeMethod.invoke(statement, """
                CREATE TABLE IF NOT EXISTS `blocks` (
                    `pos` INT PRIMARY KEY,
                    `data` BLOB
                );
            """.trimIndent())
            statement.javaClass.getMethod("close").invoke(statement)
            
            val prepareStatementMethod = conn.javaClass.getMethod("prepareStatement", String::class.java)
            jdbcInsertStmt = prepareStatementMethod.invoke(conn, "INSERT OR REPLACE INTO `blocks` (`pos`, `data`) VALUES (?, ?)")
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize JDBC SQLite driver", e)
        }
    }

    fun saveBlock(pos: MclPos, data: ByteArray) {
        if (isAndroid) {
            try {
                androidClearBindingsMethod?.invoke(androidInsertStmt)
                androidBindLongMethod?.invoke(androidInsertStmt, 1, pos.encode())
                androidBindBlobMethod?.invoke(androidInsertStmt, 2, data)
                androidExecuteInsertMethod?.invoke(androidInsertStmt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val setLongMethod = jdbcInsertStmt!!.javaClass.getMethod("setLong", Int::class.javaPrimitiveType, Long::class.javaPrimitiveType)
                val setBytesMethod = jdbcInsertStmt!!.javaClass.getMethod("setBytes", Int::class.javaPrimitiveType, ByteArray::class.java)
                val addBatchMethod = jdbcInsertStmt!!.javaClass.getMethod("addBatch")
                
                setLongMethod.invoke(jdbcInsertStmt, 1, pos.encode())
                setBytesMethod.invoke(jdbcInsertStmt, 2, data)
                addBatchMethod.invoke(jdbcInsertStmt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun commit() {
        if (isAndroid) {
            try {
                val inTx = androidInTransactionMethod?.invoke(androidDb) as? Boolean ?: false
                if (inTx) {
                    androidSetTransactionSuccessfulMethod?.invoke(androidDb)
                    androidEndTransactionMethod?.invoke(androidDb)
                }
                androidBeginTransactionMethod?.invoke(androidDb)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val executeBatchMethod = jdbcInsertStmt!!.javaClass.getMethod("executeBatch")
                val commitMethod = jdbcConnection!!.javaClass.getMethod("commit")
                executeBatchMethod.invoke(jdbcInsertStmt)
                commitMethod.invoke(jdbcConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun close() {
        if (isAndroid) {
            try {
                val inTx = androidInTransactionMethod?.invoke(androidDb) as? Boolean ?: false
                if (inTx) {
                    androidSetTransactionSuccessfulMethod?.invoke(androidDb)
                    androidEndTransactionMethod?.invoke(androidDb)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                androidCloseStmtMethod?.invoke(androidInsertStmt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                androidCloseDbMethod?.invoke(androidDb)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                jdbcInsertStmt?.javaClass?.getMethod("close")?.invoke(jdbcInsertStmt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                jdbcConnection?.javaClass?.getMethod("close")?.invoke(jdbcConnection)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
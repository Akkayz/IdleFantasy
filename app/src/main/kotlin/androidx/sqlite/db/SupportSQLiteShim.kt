package androidx.sqlite.db

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL as sqliteExecSQL
import java.io.Closeable

interface SupportSQLiteDatabase : SQLiteConnection, Closeable {
    fun execSQL(sql: String)
    fun query(sql: String): Cursor
}

class BundledSupportSQLiteDatabase(
    private val connection: SQLiteConnection
) : SupportSQLiteDatabase, SQLiteConnection by connection {

    override fun execSQL(sql: String) {
        connection.sqliteExecSQL(sql)
    }

    override fun query(sql: String): Cursor {
        val stmt = connection.prepare(sql)
        try {
            val colCount = stmt.getColumnCount()
            val colNames = Array(colCount) { i -> stmt.getColumnName(i) }
            val cursor = MatrixCursor(colNames)
            while (stmt.step()) {
                val row = Array<Any?>(colCount) { i ->
                    if (stmt.isNull(i)) null else stmt.getText(i)
                }
                cursor.addRow(row)
            }
            return cursor
        } finally {
            stmt.close()
        }
    }

    override fun close() {
        connection.close()
    }
}

interface SupportSQLiteOpenHelper : Closeable {
    val writableDatabase: SupportSQLiteDatabase
    val readableDatabase: SupportSQLiteDatabase

    abstract class Callback(val version: Int) {
        open fun onCreate(db: SupportSQLiteDatabase) {}
        open fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        open fun onOpen(db: SupportSQLiteDatabase) {}
    }

    class Configuration(
        val context: Context,
        val name: String?,
        val callback: Callback
    ) {
        class Builder(private val context: Context) {
            private var name: String? = null
            private var callback: Callback? = null

            fun name(name: String?): Builder = apply { this.name = name }
            fun callback(callback: Callback): Builder = apply { this.callback = callback }
            fun build(): Configuration = Configuration(
                context = context,
                name = name,
                callback = requireNotNull(callback) { "Must set a callback" }
            )
        }

        companion object {
            @JvmStatic
            fun builder(context: Context): Builder = Builder(context)
        }
    }

    interface Factory {
        fun create(configuration: Configuration): SupportSQLiteOpenHelper
    }
}

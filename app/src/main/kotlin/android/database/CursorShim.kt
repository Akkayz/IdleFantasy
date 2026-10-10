package android.database

import java.io.Closeable

interface Cursor : Closeable {
    fun moveToFirst(): Boolean
    fun moveToNext(): Boolean
    fun getColumnIndexOrThrow(columnName: String): Int
    fun getString(columnIndex: Int): String
    fun getInt(columnIndex: Int): Int
    fun getLong(columnIndex: Int): Long
    fun getDouble(columnIndex: Int): Double
    fun isNull(columnIndex: Int): Boolean
    override fun close()
}

class MatrixCursor(private val columnNames: Array<String>) : Cursor {
    private val rows = mutableListOf<List<Any?>>()
    private var pos = -1

    fun addRow(columnValues: Iterable<Any?>) {
        rows.add(columnValues.toList())
    }

    fun addRow(columnValues: Array<Any?>) {
        rows.add(columnValues.toList())
    }

    override fun moveToFirst(): Boolean {
        if (rows.isNotEmpty()) {
            pos = 0
            return true
        }
        return false
    }

    override fun moveToNext(): Boolean {
        if (pos + 1 < rows.size) {
            pos++
            return true
        }
        return false
    }

    override fun getColumnIndexOrThrow(columnName: String): Int {
        val idx = columnNames.indexOf(columnName)
        if (idx < 0) throw IllegalArgumentException("Column '$columnName' does not exist")
        return idx
    }

    override fun getString(columnIndex: Int): String {
        return rows[pos][columnIndex]?.toString() ?: ""
    }

    override fun getInt(columnIndex: Int): Int {
        val v = rows[pos][columnIndex] ?: return 0
        return when (v) {
            is Number -> v.toInt()
            else -> v.toString().toIntOrNull() ?: 0
        }
    }

    override fun getLong(columnIndex: Int): Long {
        val v = rows[pos][columnIndex] ?: return 0L
        return when (v) {
            is Number -> v.toLong()
            else -> v.toString().toLongOrNull() ?: 0L
        }
    }

    override fun getDouble(columnIndex: Int): Double {
        val v = rows[pos][columnIndex] ?: return 0.0
        return when (v) {
            is Number -> v.toDouble()
            else -> v.toString().toDoubleOrNull() ?: 0.0
        }
    }

    override fun isNull(columnIndex: Int): Boolean {
        return rows[pos][columnIndex] == null
    }

    override fun close() {}
}

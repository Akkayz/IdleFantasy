package androidx.sqlite.db.framework

import androidx.sqlite.db.BundledSupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File

class FrameworkSQLiteOpenHelperFactory : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        return object : SupportSQLiteOpenHelper {
            private val db: SupportSQLiteDatabase by lazy {
                val path = configuration.name?.let { name ->
                    if (name == ":memory:") ":memory:"
                    else File(configuration.context.filesDir, name).absolutePath
                } ?: ":memory:"
                val conn = BundledSQLiteDriver().open(path)
                val wrapper = BundledSupportSQLiteDatabase(conn)
                configuration.callback.onCreate(wrapper)
                configuration.callback.onOpen(wrapper)
                wrapper
            }

            override val writableDatabase: SupportSQLiteDatabase
                get() = db

            override val readableDatabase: SupportSQLiteDatabase
                get() = db

            override fun close() {
                db.close()
            }
        }
    }
}

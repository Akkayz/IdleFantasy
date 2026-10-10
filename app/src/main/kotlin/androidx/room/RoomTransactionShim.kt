package androidx.room

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.fantasyidler.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers

suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R {
    return useWriterConnection { transactor ->
        if (transactor.inTransaction()) {
            block()
        } else {
            transactor.immediateTransaction {
                block()
            }
        }
    }
}

fun createTestInMemoryAppDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Unconfined)
}

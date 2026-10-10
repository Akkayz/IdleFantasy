package androidx.room

import android.content.Context

object Room {
    class TestDatabaseBuilder<T : RoomDatabase>(
        private val delegate: RoomDatabase.Builder<T>
    ) {
        fun allowMainThreadQueries(): TestDatabaseBuilder<T> = this
        fun build(): T = delegate.build()
    }

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : RoomDatabase> inMemoryDatabaseBuilder(
        @Suppress("UNUSED_PARAMETER") context: Context,
        @Suppress("UNUSED_PARAMETER") klass: Class<T>
    ): TestDatabaseBuilder<T> {
        return TestDatabaseBuilder(createTestInMemoryAppDatabaseBuilder() as RoomDatabase.Builder<T>)
    }
}

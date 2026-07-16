package com.keyglass.nfc.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Identifier::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun identifierDao(): IdentifierDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keyglass.db"
                )
                    .addCallback(SeedCallback)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    /** Seeds the sample identifiers from the spec once, on database creation. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val seed = listOf(
                Triple("EM", "email", 0),
                Triple("BA", "bank", 1),
                Triple("MM", "crypto", 2),
                Triple("SP", "spotify", 3)
            )
            for ((code, account, position) in seed) {
                db.execSQL(
                    "INSERT INTO identifiers (code, account, position) VALUES (?, ?, ?)",
                    arrayOf<Any>(code, account, position)
                )
            }
        }
    }
}

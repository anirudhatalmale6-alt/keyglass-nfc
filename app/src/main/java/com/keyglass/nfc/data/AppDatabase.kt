package com.keyglass.nfc.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Identifier::class], version = 2, exportSchema = false)
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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * The sample list in the design is EM / BA / MM, so drop the "SP : spotify"
         * row seeded by earlier builds — but only if the user never edited it.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM identifiers WHERE code = 'SP' AND account = 'spotify'")
            }
        }
    }

    /** Seeds the sample identifiers from the design once, on database creation. */
    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val seed = listOf(
                Triple("EM", "email", 0),
                Triple("BA", "bank", 1),
                Triple("MM", "crypto", 2)
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

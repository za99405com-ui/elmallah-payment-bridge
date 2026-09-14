package com.elmallah.paymentbridge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PaymentEventEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PaymentDatabase : RoomDatabase() {

    abstract fun paymentEventDao(): PaymentEventDao

    companion object {
        @Volatile
        private var INSTANCE: PaymentDatabase? = null

        /**
         * Room Migration from v1 to v2:
         * 1. Adds parseConfidence column to store exact parser confidence.
         * 2. Adds composite unique index on (provider, transactionReference) to ensure
         *    the same transaction reference from a provider can never be inserted twice.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE payment_events ADD COLUMN parseConfidence TEXT NOT NULL DEFAULT 'high'")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_payment_events_provider_transactionReference ON payment_events (provider, transactionReference)")
            }
        }

        fun getInstance(context: Context): PaymentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaymentDatabase::class.java,
                    "almallah_payment_bridge.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

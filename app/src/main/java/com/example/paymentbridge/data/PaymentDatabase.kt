package com.example.paymentbridge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PaymentEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PaymentDatabase : RoomDatabase() {

    abstract fun paymentEventDao(): PaymentEventDao

    companion object {
        @Volatile
        private var INSTANCE: PaymentDatabase? = null

        fun getInstance(context: Context): PaymentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaymentDatabase::class.java,
                    "almallah_payment_bridge.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

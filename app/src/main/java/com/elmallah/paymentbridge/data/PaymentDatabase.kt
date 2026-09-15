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
         * Uses the SQLite table-recreation pattern:
         * 1. Creates `payment_events_new` matching the exact schema expected by Room v2.
         * 2. Copies and maps compatible legacy columns safely:
         *    - transactionFingerprint -> fingerprint
         *    - rawMessageSnippet -> rawSnippet
         *    - attemptCount -> syncAttempts
         *    - lastAttemptAt -> lastSyncAttemptAt
         *    - lastErrorMessage -> lastSyncError
         *    - matchedOrderId -> serverOrderId
         *    - parserVersion -> '1.0'
         *    - parseConfidence -> 'high'
         *    - deviceId -> 'legacy_migrated_device'
         *    - syncStatus -> 'CAPTURE_ONLY' (Phase-1 mandate: legacy rows are NOT auto-uploaded)
         * 3. Drops legacy `payment_events` table (and old incompatible indexes).
         * 4. Renames `payment_events_new` to `payment_events`.
         * 5. Recreates all exact indexes expected by Room v2 entity.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create payment_events_new matching the exact v2 schema
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `payment_events_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `eventId` TEXT NOT NULL,
                        `fingerprint` TEXT NOT NULL,
                        `provider` TEXT NOT NULL,
                        `paymentChannel` TEXT NOT NULL,
                        `amountMinor` INTEGER NOT NULL,
                        `currency` TEXT NOT NULL,
                        `payerPhone` TEXT,
                        `walletPhone` TEXT,
                        `transactionReference` TEXT NOT NULL,
                        `accountLast4` TEXT,
                        `sourceSender` TEXT NOT NULL,
                        `sourcePackage` TEXT NOT NULL,
                        `notificationPostedAt` INTEGER NOT NULL,
                        `capturedAt` INTEGER NOT NULL,
                        `parserVersion` TEXT NOT NULL,
                        `parseConfidence` TEXT NOT NULL DEFAULT 'high',
                        `rawMessageHash` TEXT NOT NULL,
                        `rawSnippet` TEXT,
                        `deviceId` TEXT NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `syncAttempts` INTEGER NOT NULL,
                        `lastSyncAttemptAt` INTEGER,
                        `lastSyncError` TEXT,
                        `serverMatchStatus` TEXT,
                        `serverOrderId` TEXT,
                        `serverMessage` TEXT,
                        `acknowledgedAt` INTEGER
                    )
                    """.trimIndent()
                )

                // 2. Copy and map compatible legacy data safely
                db.execSQL(
                    """
                    INSERT INTO `payment_events_new` (
                        `id`,
                        `eventId`,
                        `fingerprint`,
                        `provider`,
                        `paymentChannel`,
                        `amountMinor`,
                        `currency`,
                        `payerPhone`,
                        `walletPhone`,
                        `transactionReference`,
                        `accountLast4`,
                        `sourceSender`,
                        `sourcePackage`,
                        `notificationPostedAt`,
                        `capturedAt`,
                        `parserVersion`,
                        `parseConfidence`,
                        `rawMessageHash`,
                        `rawSnippet`,
                        `deviceId`,
                        `syncStatus`,
                        `syncAttempts`,
                        `lastSyncAttemptAt`,
                        `lastSyncError`,
                        `serverMatchStatus`,
                        `serverOrderId`,
                        `serverMessage`,
                        `acknowledgedAt`
                    )
                    SELECT
                        `id`,
                        `eventId`,
                        COALESCE(`transactionFingerprint`, `provider` || ':' || `transactionReference`) AS `fingerprint`,
                        `provider`,
                        `paymentChannel`,
                        `amountMinor`,
                        COALESCE(`currency`, 'EGP') AS `currency`,
                        `payerPhone`,
                        `walletPhone`,
                        `transactionReference`,
                        `accountLast4`,
                        `sourceSender`,
                        `sourcePackage`,
                        `notificationPostedAt`,
                        `capturedAt`,
                        '1.0' AS `parserVersion`,
                        'high' AS `parseConfidence`,
                        `rawMessageHash`,
                        `rawMessageSnippet` AS `rawSnippet`,
                        'legacy_migrated_device' AS `deviceId`,
                        'CAPTURE_ONLY' AS `syncStatus`,
                        COALESCE(`attemptCount`, 0) AS `syncAttempts`,
                        `lastAttemptAt` AS `lastSyncAttemptAt`,
                        `lastErrorMessage` AS `lastSyncError`,
                        `serverMatchStatus`,
                        `matchedOrderId` AS `serverOrderId`,
                        NULL AS `serverMessage`,
                        NULL AS `acknowledgedAt`
                    FROM `payment_events`
                    """.trimIndent()
                )

                // 3. Drop legacy table
                db.execSQL("DROP TABLE `payment_events`")

                // 4. Rename new table to payment_events
                db.execSQL("ALTER TABLE `payment_events_new` RENAME TO `payment_events`")

                // 5. Recreate exact indexes expected by Room v2
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_events_eventId` ON `payment_events` (`eventId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_events_provider_transactionReference` ON `payment_events` (`provider`, `transactionReference`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_events_capturedAt` ON `payment_events` (`capturedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_payment_events_syncStatus` ON `payment_events` (`syncStatus`)")
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

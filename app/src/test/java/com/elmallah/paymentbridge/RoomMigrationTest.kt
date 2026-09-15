package com.elmallah.paymentbridge

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.elmallah.paymentbridge.data.PaymentDatabase
import com.elmallah.paymentbridge.data.PaymentEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationTest {

    private lateinit var context: Context
    private val dbName = "test_migration_payment_bridge.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migration1To2_preservesLegacyDataAndAppliesV2Schema() = runBlocking {
        // 1. Create the exact v1 database schema
        val factory = FrameworkSQLiteOpenHelperFactory()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE `payment_events` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `eventId` TEXT NOT NULL,
                            `transactionFingerprint` TEXT NOT NULL,
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
                            `receivedAt` INTEGER NOT NULL,
                            `rawMessageHash` TEXT NOT NULL,
                            `rawMessageSnippet` TEXT,
                            `syncStatus` TEXT NOT NULL,
                            `serverMatchStatus` TEXT,
                            `matchedOrderId` TEXT,
                            `parseStatus` TEXT NOT NULL,
                            `attemptCount` INTEGER NOT NULL,
                            `lastAttemptAt` INTEGER,
                            `lastErrorMessage` TEXT,
                            `createdAt` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_events_eventId` ON `payment_events` (`eventId`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // Not called here
                }
            })
            .build()

        val helper = factory.create(config)
        val v1Db = helper.writableDatabase

        // 2. Insert legacy rows: Vodafone Cash and NBE InstaPay
        v1Db.execSQL(
            """
            INSERT INTO `payment_events` (
                `eventId`, `transactionFingerprint`, `provider`, `paymentChannel`,
                `amountMinor`, `currency`, `payerPhone`, `walletPhone`,
                `transactionReference`, `accountLast4`, `sourceSender`, `sourcePackage`,
                `notificationPostedAt`, `capturedAt`, `receivedAt`, `rawMessageHash`,
                `rawMessageSnippet`, `syncStatus`, `serverMatchStatus`, `matchedOrderId`,
                `parseStatus`, `attemptCount`, `lastAttemptAt`, `lastErrorMessage`, `createdAt`
            ) VALUES (
                'evt_legacy_vf_001', 'vodafone_cash:VF7766554433', 'vodafone_cash', 'vodafone_cash',
                18000, 'EGP', '01012345678', NULL,
                'VF7766554433', NULL, 'VF-Cash', 'com.google.android.apps.messaging',
                1710400000000, 1710400001000, 1710400002000, 'hash_vf_001',
                'تم استلام مبلغ 180.00 جنيه من 01012345678', 'CAPTURE_ONLY', NULL, 'ORD-101',
                'PARSED', 1, 1710400005000, 'Timeout error', 1710400000000
            )
            """.trimIndent()
        )

        v1Db.execSQL(
            """
            INSERT INTO `payment_events` (
                `eventId`, `transactionFingerprint`, `provider`, `paymentChannel`,
                `amountMinor`, `currency`, `payerPhone`, `walletPhone`,
                `transactionReference`, `accountLast4`, `sourceSender`, `sourcePackage`,
                `notificationPostedAt`, `capturedAt`, `receivedAt`, `rawMessageHash`,
                `rawMessageSnippet`, `syncStatus`, `serverMatchStatus`, `matchedOrderId`,
                `parseStatus`, `attemptCount`, `lastAttemptAt`, `lastErrorMessage`, `createdAt`
            ) VALUES (
                'evt_legacy_nbe_002', 'nbe_incoming_transfer:NBE998877665', 'nbe_incoming_transfer', 'instapay_or_bank_transfer',
                28722, 'EGP', NULL, NULL,
                'NBE998877665', '5678', 'Bank-AlAhly', 'com.samsung.android.messaging',
                1710410000000, 1710410001000, 1710410002000, 'hash_nbe_002',
                'تحويل وارد بقيمة 287.22 جنيه مصري', 'PENDING_UPLOAD', 'MATCHED', 'ORD-202',
                'PARSED', 0, NULL, NULL, 1710410000000
            )
            """.trimIndent()
        )

        v1Db.close()
        helper.close()

        // 3. Runs MIGRATION_1_2 and 4. Opens database as v2 through Room
        // Room will execute MIGRATION_1_2 and run onValidateSchema()
        val v2Db = Room.databaseBuilder(
            context,
            PaymentDatabase::class.java,
            dbName
        )
            .addMigrations(PaymentDatabase.MIGRATION_1_2)
            .build()

        val dao = v2Db.paymentEventDao()
        val migratedEvents = dao.getAllEventsFlow().first()

        // 5. Verify the financial data survived and no destructive migration occurred
        assertEquals("Both legacy events must survive migration", 2, migratedEvents.size)

        val vfEvent = migratedEvents.find { it.eventId == "evt_legacy_vf_001" }
        assertNotNull("Vodafone Cash legacy event must exist", vfEvent)
        assertEquals(18000L, vfEvent!!.amountMinor)
        assertEquals("EGP", vfEvent.currency)
        assertEquals("vodafone_cash", vfEvent.provider)
        assertEquals("VF7766554433", vfEvent.transactionReference)
        assertEquals("01012345678", vfEvent.payerPhone)
        assertEquals("VF-Cash", vfEvent.sourceSender)
        assertEquals("com.google.android.apps.messaging", vfEvent.sourcePackage)
        assertEquals("vodafone_cash:VF7766554433", vfEvent.fingerprint)
        assertEquals("تم استلام مبلغ 180.00 جنيه من 01012345678", vfEvent.rawSnippet)
        assertEquals(1, vfEvent.syncAttempts)
        assertEquals(1710400005000L, vfEvent.lastSyncAttemptAt)
        assertEquals("Timeout error", vfEvent.lastSyncError)
        assertEquals("ORD-101", vfEvent.serverOrderId)

        // 6. Verify parseConfidence exists and is populated
        assertEquals("high", vfEvent.parseConfidence)
        assertEquals("1.0", vfEvent.parserVersion)
        assertEquals("legacy_migrated_device", vfEvent.deviceId)

        // Verify syncStatus is hard-locked to CAPTURE_ONLY for legacy migrated rows
        assertEquals("CAPTURE_ONLY", vfEvent.syncStatus)

        val nbeEvent = migratedEvents.find { it.eventId == "evt_legacy_nbe_002" }
        assertNotNull("NBE legacy event must exist", nbeEvent)
        assertEquals(28722L, nbeEvent!!.amountMinor)
        assertEquals("EGP", nbeEvent.currency)
        assertEquals("nbe_incoming_transfer", nbeEvent.provider)
        assertEquals("NBE998877665", nbeEvent.transactionReference)
        assertEquals("5678", nbeEvent.accountLast4)
        assertEquals("Bank-AlAhly", nbeEvent.sourceSender)
        assertEquals("MATCHED", nbeEvent.serverMatchStatus)
        assertEquals("ORD-202", nbeEvent.serverOrderId)
        assertEquals("high", nbeEvent.parseConfidence)
        assertEquals("CAPTURE_ONLY", nbeEvent.syncStatus)

        // 7. Verify provider + transactionReference uniqueness
        // A) Room DAO insert with IGNORE returns -1 on conflict
        val duplicateVfEvent = PaymentEventEntity(
            eventId = "evt_vf_duplicate_new_id",
            fingerprint = "vodafone_cash:VF7766554433",
            provider = "vodafone_cash",
            paymentChannel = "vodafone_cash",
            amountMinor = 18000L,
            currency = "EGP",
            transactionReference = "VF7766554433",
            sourceSender = "VF-Cash",
            sourcePackage = "com.google.android.apps.messaging",
            notificationPostedAt = System.currentTimeMillis(),
            capturedAt = System.currentTimeMillis(),
            parserVersion = "1.0",
            parseConfidence = "high",
            rawMessageHash = "hash_new_dup",
            deviceId = "device_test"
        )
        val insertResult = dao.insert(duplicateVfEvent)
        assertEquals(-1L, insertResult)

        // B) Direct SQL insert fails with SQLiteConstraintException due to index_payment_events_provider_transactionReference
        try {
            v2Db.openHelper.writableDatabase.execSQL(
                """
                INSERT INTO payment_events (
                    eventId, fingerprint, provider, paymentChannel, amountMinor, currency,
                    transactionReference, sourceSender, sourcePackage, notificationPostedAt,
                    capturedAt, parserVersion, parseConfidence, rawMessageHash, deviceId,
                    syncStatus, syncAttempts
                ) VALUES (
                    'evt_direct_dup_id', 'vodafone_cash:VF7766554433', 'vodafone_cash', 'vodafone_cash', 18000, 'EGP',
                    'VF7766554433', 'VF-Cash', 'com.google.android.apps.messaging', 100,
                    100, '1.0', 'high', 'h', 'd',
                    'CAPTURE_ONLY', 0
                )
                """.trimIndent()
            )
            fail("Expected SQLiteConstraintException on direct insert of duplicate provider + transactionReference")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            assertTrue(true)
        }

        v2Db.close()
    }
}

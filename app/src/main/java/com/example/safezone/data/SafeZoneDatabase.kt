
package com.example.safezone.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SafetyCheckInEntity::class,
        TrustedContactEntity::class,
        IncidentReportEntity::class,
        SafetyAlertEntity::class,
        EmergencyRequestEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class SafeZoneDatabase : RoomDatabase() {

    abstract fun safetyCheckInDao(): SafetyCheckInDao

    abstract fun trustedContactDao(): TrustedContactDao

    abstract fun incidentReportDao(): IncidentReportDao

    abstract fun safetyAlertDao(): SafetyAlertDao

    abstract fun emergencyRequestDao(): EmergencyRequestDao

    companion object {

        @Volatile
        private var INSTANCE: SafeZoneDatabase? = null

        // Version 7 -> 8: Create emergency requests.
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS emergency_requests (
                        requestId TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        assistanceType TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Version 8 -> 9: Add campus support to cached alerts.
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE cached_safety_alerts
                    ADD COLUMN campus TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

        // Version 9 -> 10: Add campus and audio evidence
        // to existing incident reports.
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE incident_reports
                    ADD COLUMN campus TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    ALTER TABLE incident_reports
                    ADD COLUMN audioEvidence TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): SafeZoneDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SafeZoneDatabase::class.java,
                    "safezone_database"
                )
                    .addMigrations(
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}


package com.example.safezone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentReportDao {

    // Save a new report or update an existing report.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: IncidentReportEntity)

    // Retrieve all reports belonging to the signed-in user.
    // Room automatically emits updates when the data changes.
    @Query("""
        SELECT * FROM incident_reports
        WHERE userId = :userId
        ORDER BY createdAt DESC
    """)
    fun getReportsForUser(
        userId: String
    ): Flow<List<IncidentReportEntity>>

    // Retrieve campus reports for a particular user.
    @Query("""
        SELECT * FROM incident_reports
        WHERE userId = :userId
        AND campus = :campus
        ORDER BY createdAt DESC
    """)
    fun getReportsForCampus(
        userId: String,
        campus: String
    ): Flow<List<IncidentReportEntity>>

    // Retrieve reports that still need to be uploaded.
    @Query("""
        SELECT * FROM incident_reports
        WHERE userId = :userId
        AND syncStatus = 'Pending Sync'
        ORDER BY createdAt ASC
    """)
    suspend fun getPendingSyncReports(
        userId: String
    ): List<IncidentReportEntity>

    // Mark a report as Synced after Firestore confirms success.
    @Query("""
        UPDATE incident_reports
        SET syncStatus = :syncStatus
        WHERE localId = :localId
        AND userId = :userId
    """)
    suspend fun updateSyncStatus(
        localId: String,
        userId: String,
        syncStatus: String
    )

    // Retrieve one report using its stable local ID.
    @Query("""
        SELECT * FROM incident_reports
        WHERE localId = :localId
        AND userId = :userId
        LIMIT 1
    """)
    suspend fun getReportById(
        localId: String,
        userId: String
    ): IncidentReportEntity?

    // Retrieve a report by its ID regardless of sync status.
    @Query("""
        SELECT * FROM incident_reports
        WHERE localId = :localId
        LIMIT 1
    """)
    suspend fun getReportByLocalId(
        localId: String
    ): IncidentReportEntity?
}

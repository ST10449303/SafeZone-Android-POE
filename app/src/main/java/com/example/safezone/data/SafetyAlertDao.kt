
package com.example.safezone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyAlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(
        alerts: List<SafetyAlertEntity>
    )

    // ==========================================
    // PERSONAL ALERTS
    // ==========================================

    @Query("""
        SELECT * FROM cached_safety_alerts
        WHERE location = :location
        AND campus = ''
        ORDER BY cachedAt DESC
    """)
    fun getAlertsForLocation(
        location: String
    ): Flow<List<SafetyAlertEntity>>

    @Query("""
        DELETE FROM cached_safety_alerts
        WHERE location = :location
        AND campus = ''
    """)
    suspend fun deleteAlertsForLocation(
        location: String
    )

    @Query("""
        SELECT * FROM cached_safety_alerts
        WHERE location = :location
        AND campus = ''
        ORDER BY cachedAt DESC
    """)
    suspend fun getAlertsForLocationOnce(
        location: String
    ): List<SafetyAlertEntity>

    // ==========================================
    // CAMPUS ALERTS
    // ==========================================

    @Query("""
        SELECT * FROM cached_safety_alerts
        WHERE campus = :campus
        ORDER BY cachedAt DESC
    """)
    fun getAlertsForCampus(
        campus: String
    ): Flow<List<SafetyAlertEntity>>

    @Query("""
        SELECT * FROM cached_safety_alerts
        WHERE campus = :campus
        ORDER BY cachedAt DESC
    """)
    suspend fun getAlertsForCampusOnce(
        campus: String
    ): List<SafetyAlertEntity>

    @Query("""
        DELETE FROM cached_safety_alerts
        WHERE campus = :campus
    """)
    suspend fun deleteAlertsForCampus(
        campus: String
    )
}

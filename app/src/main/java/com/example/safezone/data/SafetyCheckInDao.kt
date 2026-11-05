
package com.example.safezone.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SafetyCheckInDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: SafetyCheckInEntity)

    @Update
    suspend fun update(checkIn: SafetyCheckInEntity)

    @Query(
        """
        SELECT * FROM safety_check_ins
        WHERE userId = :userId
        ORDER BY startedAt DESC
        """
    )
    fun getCheckInsForUser(
        userId: String
    ): Flow<List<SafetyCheckInEntity>>

    @Query(
        """
        SELECT * FROM safety_check_ins
        WHERE syncStatus = 'Pending Sync'
        ORDER BY startedAt ASC
        """
    )
    suspend fun getPendingSyncCheckIns(): List<SafetyCheckInEntity>

    @Query(
        """
        UPDATE safety_check_ins
        SET status = :status,
            completedAt = :completedAt,
            cancelledAt = :cancelledAt,
            expiredAt = :expiredAt,
            syncStatus = 'Pending Sync'
        WHERE localId = :localId
        """
    )
    suspend fun updateCheckInStatus(
        localId: String,
        status: String,
        completedAt: Long?,
        cancelledAt: Long?,
        expiredAt: Long?
    )

    @Query(
        """
        UPDATE safety_check_ins
        SET syncStatus = :syncStatus,
            firestoreDocumentId = :documentId
        WHERE localId = :localId
        """
    )
    suspend fun updateSyncStatus(
        localId: String,
        syncStatus: String,
        documentId: String?
    )

    @Query(
        """
        SELECT * FROM safety_check_ins
        WHERE localId = :localId
        LIMIT 1
        """
    )
    suspend fun getCheckInById(
        localId: String
    ): SafetyCheckInEntity?

    @Query(
        """
        DELETE FROM safety_check_ins
        WHERE localId = :localId
        """
    )
    suspend fun deleteCheckIn(localId: String)
}
